package com.vtc.openapi.domain.instance.service.business.impl;

import com.vtc.openapi.domain.instance.model.command.RemediateInstanceCommand;
import com.vtc.openapi.domain.instance.model.command.SearchInstanceCommand;
import com.vtc.openapi.domain.instance.model.audit.OpenVulnInstanceAudit;
import com.vtc.openapi.domain.instance.model.audit.OpenVulnInstanceAuditContext;
import com.vtc.openapi.domain.operationcase.context.OperationCaseContext;
import com.vtc.openapi.domain.instance.model.command.VerifyFixInstanceCommand;
import com.vtc.openapi.domain.instance.model.command.VerifyInstanceCommand;
import com.vtc.openapi.domain.instance.model.result.InstanceItemResult;
import com.vtc.openapi.domain.instance.model.result.InstancePageResult;
import com.vtc.openapi.domain.instance.model.result.InstanceStateResult;
import com.vtc.openapi.domain.instance.repository.IInstanceRepository;
import com.vtc.openapi.domain.instance.service.business.IVerifyFixJobDomainService;
import com.vtc.openapi.domain.instance.service.business.IInstanceDomainService;
import com.vtc.openapi.domain.instance.service.business.IInstanceScanFollowUpService;
import com.vtc.openapi.domain.open.OpenApiConstants;
import com.vtc.openapi.domain.open.OpenApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 实例领域服务：状态机校验 + 写操作 + Webhook 事件发布。
 */
@Service
public class InstanceDomainServiceImpl implements IInstanceDomainService {

    private static final Logger log = LoggerFactory.getLogger(InstanceDomainServiceImpl.class);

    /** verify 允许的前置状态：潜在预警(0)、初始发现(1) */
    private static final Set<Integer> VERIFY_ALLOWED_STATES = new HashSet<>(Arrays.asList(0, 1));
    /** remediate 允许的前置状态：初始发现(1)、已验证有效(2)、核验未修复(7) */
    private static final Set<Integer> REMEDIATE_ALLOWED_STATES = new HashSet<>(Arrays.asList(1, 2, 7));
    /** verify-fix 允许的前置状态：已修复(5) */
    private static final Set<Integer> VERIFY_FIX_ALLOWED_STATES = new HashSet<>(Collections.singletonList(5));

    private static final int STAT_VALIDATED_TRUE = 2;
    private static final int STAT_VALIDATED_FALSE = 3;
    private static final int STAT_FIXED = 5;
    private static final int STAT_VERIFIED_FIXED = 6;
    private static final int STAT_VERIFIED_UNFIXED = 7;
    private static final int STAT_ARCHIVE_FAILED = 9;

    private static final int FIX_METHOD_MIN = 1050;
    private static final int FIX_METHOD_MAX = 1053;
    private static final int FIX_METHOD_PATCH = 1050;
    private static final int FIX_METHOD_EQUIV = 1051;
    private static final int FIX_METHOD_BLOCK = 1052;

    private static final String VERIFY_RESULT_VALID = "VALID";
    private static final String VERIFY_RESULT_FALSE_POSITIVE = "FALSE_POSITIVE";
    private static final String VERIFY_FIX_RESULT_CONFIRMED = "FIX_CONFIRMED";
    private static final String VERIFY_FIX_RESULT_FAILED = "FIX_FAILED";

    private final IInstanceRepository instanceRepository;
    private final IInstanceScanFollowUpService scanFollowUpService;
    private final IVerifyFixJobDomainService verifyFixJobDomainService;

    public InstanceDomainServiceImpl(IInstanceRepository instanceRepository,
                                     IInstanceScanFollowUpService scanFollowUpService,
                                     IVerifyFixJobDomainService verifyFixJobDomainService) {
        this.instanceRepository = instanceRepository;
        this.scanFollowUpService = scanFollowUpService;
        this.verifyFixJobDomainService = verifyFixJobDomainService;
    }

    @Override
    public InstancePageResult search(String partnerId, SearchInstanceCommand command) {
        return instanceRepository.searchInstances(partnerId, command);
    }

    @Override
    public InstanceItemResult getByVulInfoId(String partnerId, String vulInfoId) {
        InstanceItemResult result = instanceRepository.findByVulInfoId(partnerId, vulInfoId);
        if (result == null) {
            return null;
        }
        return result;
    }

    @Override
    public InstanceStateResult verify(String partnerId, VerifyInstanceCommand command) {
        validateVerifyCommand(command);
        InstanceItemResult current = requireInstance(partnerId, command.getVulInfoId());
        int currentStat = requireStat(current);

        if (!VERIFY_ALLOWED_STATES.contains(currentStat)) {
            if (currentStat == STAT_VALIDATED_TRUE || currentStat == STAT_VALIDATED_FALSE) {
                throw new OpenApiException(OpenApiConstants.CODE_DUPLICATE_OP,
                        "实例已验证");
            }
            throw new OpenApiException(OpenApiConstants.CODE_STATE_INVALID,
                    "实例当前状态不允许验证，当前状态: " + currentStat);
        }

        int targetStat = resolveVerifyTarget(command.getVerifyResult());
        InstanceStateResult result = OpenVulnInstanceAuditContext.callWith(
                withOperationCase(OpenVulnInstanceAudit.partnerVerify(command.getTransferTime())),
                () -> executeStateChange(
                        current, targetStat, command.getSrcMethod(), null, command.getTransferTime()));
        scanFollowUpService.scheduleVerifyScan(partnerId, current.getVulInfoId());
        return result;
    }

    @Override
    public InstanceStateResult remediate(String partnerId, RemediateInstanceCommand command) {
        if (command == null) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "请求体不能为空");
        }
        InstanceItemResult current = requireInstance(partnerId, command.getVulInfoId());
        int currentStat = requireStat(current);
        assertRemediatePreState(currentStat);

        int targetStat = resolveRemediateTarget(command);
        validateRemediateConsistency(command, targetStat);
        validateRemediateFields(command, targetStat);
        validateRemediateTicket(command);

        return OpenVulnInstanceAuditContext.callWith(
                withOperationCase(OpenVulnInstanceAudit.partnerRemediate(command.getTransferTime())),
                () -> executeRemediateStateChange(current, targetStat, command));
    }

    @Override
    public InstanceStateResult verifyFix(String partnerId, VerifyFixInstanceCommand command) {
        InstanceItemResult current = requireInstance(partnerId, command.getVulInfoId());
        int currentStat = requireStat(current);

        if (!VERIFY_FIX_ALLOWED_STATES.contains(currentStat)) {
            if (currentStat == STAT_VERIFIED_FIXED || currentStat == STAT_VERIFIED_UNFIXED) {
                throw new OpenApiException(OpenApiConstants.CODE_DUPLICATE_OP,
                        "实例已核验");
            }
            throw new OpenApiException(OpenApiConstants.CODE_STATE_INVALID,
                    "实例当前状态不允许核验修复，当前状态: " + currentStat);
        }

        if (!StringUtils.hasText(command.getVerifyResult())) {
            return verifyFixJobDomainService.accept(partnerId, command, command.getBatchId());
        }

        int targetStat = resolveVerifyFixTarget(command.getVerifyResult());
        return OpenVulnInstanceAuditContext.callWith(
                withOperationCase(OpenVulnInstanceAudit.verifyFixComplete(command.getBatchId())),
                () -> executeStateChange(
                        current, targetStat, null, null, command.getTransferTime()));
    }

    private void validateVerifyCommand(VerifyInstanceCommand command) {
        if (command == null || !StringUtils.hasText(command.getVerifyResult())) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "verifyResult 不能为空");
        }
        if (!StringUtils.hasText(command.getOperator())) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "operator 不能为空");
        }
        if (VERIFY_RESULT_VALID.equalsIgnoreCase(command.getVerifyResult())
                && command.getSrcMethod() == null) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR,
                    "verifyResult=VALID 时 srcMethod 不能为空");
        }
    }

    private void validateRemediateTicket(RemediateInstanceCommand command) {
        if (command.getSrcTktRole() == null) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "srcTktRole 不能为空");
        }
        if (command.getDstTktRole() == null) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "dstTktRole 不能为空");
        }
        if (!StringUtils.hasText(command.getAssignerDept())) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "assignerDept 不能为空");
        }
        if (!StringUtils.hasText(command.getHandlerDept())) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "handlerDept 不能为空");
        }
        if (!StringUtils.hasText(command.getAssignerEmail())
                && !StringUtils.hasText(command.getAssignerPhone())) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR,
                    "assignerEmail 与 assignerPhone 至少填一项");
        }
        if (!StringUtils.hasText(command.getHandlerEmail())
                && !StringUtils.hasText(command.getHandlerPhone())) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR,
                    "handlerEmail 与 handlerPhone 至少填一项");
        }
    }

    private InstanceItemResult requireInstance(String partnerId, String vulInfoId) {
        InstanceItemResult current = instanceRepository.findByVulInfoId(partnerId, vulInfoId);
        if (current == null) {
            throw new OpenApiException(OpenApiConstants.CODE_CROSS_PARTNER,
                    "实例不存在或无权访问");
        }
        return current;
    }

    private int requireStat(InstanceItemResult item) {
        if (item.getVulInfoStat() == null) {
            throw new OpenApiException(OpenApiConstants.CODE_STATE_INVALID,
                    "实例状态未知");
        }
        return item.getVulInfoStat();
    }

    private int resolveVerifyTarget(String verifyResult) {
        if (VERIFY_RESULT_VALID.equals(verifyResult)) {
            return STAT_VALIDATED_TRUE;
        }
        if (VERIFY_RESULT_FALSE_POSITIVE.equals(verifyResult)) {
            return STAT_VALIDATED_FALSE;
        }
        throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR,
                "无效的 verifyResult，期望 VALID 或 FALSE_POSITIVE");
    }

    private int resolveVerifyFixTarget(String verifyResult) {
        if (VERIFY_FIX_RESULT_CONFIRMED.equals(verifyResult)) {
            return STAT_VERIFIED_FIXED;
        }
        if (VERIFY_FIX_RESULT_FAILED.equals(verifyResult)) {
            return STAT_VERIFIED_UNFIXED;
        }
        throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR,
                "无效的 verifyResult，期望 FIX_CONFIRMED 或 FIX_FAILED");
    }

    private void assertRemediatePreState(int currentStat) {
        if (currentStat == STAT_FIXED || currentStat == STAT_ARCHIVE_FAILED) {
            throw new OpenApiException(OpenApiConstants.CODE_DUPLICATE_OP, "实例已处置");
        }
        if (currentStat == STAT_VALIDATED_FALSE) {
            throw new OpenApiException(OpenApiConstants.CODE_STATE_INVALID,
                    "误报实例不可处置，当前状态: " + currentStat);
        }
        if (!REMEDIATE_ALLOWED_STATES.contains(currentStat)) {
            throw new OpenApiException(OpenApiConstants.CODE_STATE_INVALID,
                    "实例当前状态不允许处置，当前状态: " + currentStat);
        }
    }

    private int resolveRemediateTarget(RemediateInstanceCommand command) {
        Integer requested = command.getVulInfoStat();
        if (requested != null) {
            if (requested != STAT_FIXED && requested != STAT_ARCHIVE_FAILED) {
                throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR,
                        "vulInfoStat 仅允许 5（已修复）或 9（修复失败/备案）");
            }
            return requested;
        }
        return command.getLvRsn() != null ? STAT_ARCHIVE_FAILED : STAT_FIXED;
    }

    private void validateRemediateConsistency(RemediateInstanceCommand command, int targetStat) {
        if (targetStat == STAT_FIXED && command.getLvRsn() != null) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR,
                    "已修复(5)不应填写 lvRsn");
        }
        if (targetStat == STAT_ARCHIVE_FAILED && command.getLvRsn() == null) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR,
                    "修复失败/备案(9)必须填写 lvRsn");
        }
    }

    private void validateRemediateFields(RemediateInstanceCommand command, int targetStat) {
        if (command.getSrcMethod() == null) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "srcMethod 不能为空");
        }
        int srcMethod = command.getSrcMethod();
        if (targetStat == STAT_FIXED) {
            if (srcMethod < FIX_METHOD_MIN || srcMethod > FIX_METHOD_MAX) {
                throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR,
                        "已修复处置 srcMethod 须为 1050–1053");
            }
            if (!StringUtils.hasText(command.getRemedDesc())) {
                throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "remedDesc 不能为空");
            }
            if (!StringUtils.hasText(command.getRemedTime())) {
                throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "remedTime 不能为空");
            }
            if (srcMethod == FIX_METHOD_PATCH && !StringUtils.hasText(command.getFixLnk())) {
                throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR,
                        "srcMethod=1050 时 fixLnk 不能为空");
            }
            if ((srcMethod == FIX_METHOD_EQUIV || srcMethod == FIX_METHOD_BLOCK)
                    && !StringUtils.hasText(command.getDefDev())) {
                throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR,
                        "srcMethod=1051/1052 时 defDev 不能为空");
            }
            return;
        }
        if (!StringUtils.hasText(command.getArchiveReason())) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "archiveReason 不能为空");
        }
    }

    private InstanceStateResult executeRemediateStateChange(InstanceItemResult current,
                                                            int targetStat,
                                                            RemediateInstanceCommand command) {
        instanceRepository.updateRemediateState(current.getId(), targetStat, command);

        InstanceStateResult result = new InstanceStateResult();
        result.setVulInfoId(current.getVulInfoId());
        result.setVulInfoStat(targetStat);
        result.setLvRsn(targetStat == STAT_FIXED ? null : command.getLvRsn());
        result.setTransferTime(resolveTransferTime(command.getTransferTime()));
        result.setSrcMethod(command.getSrcMethod());
        if (targetStat == STAT_FIXED) {
            result.setRemedDesc(command.getRemedDesc());
        } else {
            result.setArchiveReason(command.getArchiveReason());
        }
        return result;
    }

    private InstanceStateResult executeStateChange(InstanceItemResult current,
                                                   int targetStat,
                                                   Integer srcMethod,
                                                   String remedDesc,
                                                   String transferTime) {
        String method = srcMethod != null ? String.valueOf(srcMethod) : null;
        instanceRepository.updateInstanceState(current.getId(), targetStat, method, remedDesc);

        InstanceStateResult result = new InstanceStateResult();
        result.setVulInfoId(current.getVulInfoId());
        result.setVulInfoStat(targetStat);
        result.setTransferTime(resolveTransferTime(transferTime));
        result.setSrcMethod(srcMethod);
        result.setRemedDesc(remedDesc);
        return result;
    }

    private String resolveTransferTime(String transferTime) {
        if (StringUtils.hasText(transferTime)) {
            return transferTime.trim();
        }
        return String.valueOf(System.currentTimeMillis() / 1000);
    }

    private static OpenVulnInstanceAudit withOperationCase(OpenVulnInstanceAudit audit) {
        String caseId = OperationCaseContext.getCaseId();
        if (StringUtils.hasText(caseId)) {
            return audit.caseId(caseId);
        }
        return audit;
    }
}
