package com.vtc.openapi.domain.instance.service.business.impl;

import com.vtc.openapi.domain.instance.model.command.RemediateInstanceCommand;
import com.vtc.openapi.domain.instance.model.command.SearchInstanceCommand;
import com.vtc.openapi.domain.instance.model.command.VerifyFixInstanceCommand;
import com.vtc.openapi.domain.instance.model.command.VerifyInstanceCommand;
import com.vtc.openapi.domain.instance.model.result.InstanceItemResult;
import com.vtc.openapi.domain.instance.model.result.InstancePageResult;
import com.vtc.openapi.domain.instance.model.result.InstanceStateResult;
import com.vtc.openapi.domain.instance.repository.IInstanceRepository;
import com.vtc.openapi.domain.instance.service.business.IInstanceDomainService;
import com.vtc.openapi.domain.instance.service.business.IInstanceScanFollowUpService;
import com.vtc.openapi.domain.open.OpenApiConstants;
import com.vtc.openapi.domain.open.OpenApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

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
    /** remediate 允许的前置状态：已验证有效(2)、核验未修复(7) */
    private static final Set<Integer> REMEDIATE_ALLOWED_STATES = new HashSet<>(Arrays.asList(2, 7));
    /** verify-fix 允许的前置状态：已修复(5) */
    private static final Set<Integer> VERIFY_FIX_ALLOWED_STATES = new HashSet<>(Collections.singletonList(5));

    private static final int STAT_VALIDATED_TRUE = 2;
    private static final int STAT_VALIDATED_FALSE = 3;
    private static final int STAT_FIXED = 5;
    private static final int STAT_VERIFIED_FIXED = 6;
    private static final int STAT_VERIFIED_UNFIXED = 7;

    private static final String VERIFY_RESULT_VALID = "VALID";
    private static final String VERIFY_RESULT_FALSE_POSITIVE = "FALSE_POSITIVE";
    private static final String VERIFY_FIX_RESULT_CONFIRMED = "FIX_CONFIRMED";
    private static final String VERIFY_FIX_RESULT_FAILED = "FIX_FAILED";

    private final IInstanceRepository instanceRepository;
    private final IInstanceScanFollowUpService scanFollowUpService;

    public InstanceDomainServiceImpl(IInstanceRepository instanceRepository,
                                     IInstanceScanFollowUpService scanFollowUpService) {
        this.instanceRepository = instanceRepository;
        this.scanFollowUpService = scanFollowUpService;
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
        InstanceStateResult result = executeStateChange(partnerId, current, targetStat, null, null);
        scanFollowUpService.scheduleVerifyScan(partnerId, current.getVulInfoId());
        return result;
    }

    @Override
    public InstanceStateResult remediate(String partnerId, RemediateInstanceCommand command) {
        InstanceItemResult current = requireInstance(partnerId, command.getVulInfoId());
        int currentStat = requireStat(current);

        if (!REMEDIATE_ALLOWED_STATES.contains(currentStat)) {
            if (currentStat == STAT_FIXED) {
                throw new OpenApiException(OpenApiConstants.CODE_DUPLICATE_OP,
                        "实例已修复");
            }
            throw new OpenApiException(OpenApiConstants.CODE_STATE_INVALID,
                    "实例当前状态不允许修复，当前状态: " + currentStat);
        }

        return executeStateChange(partnerId, current, STAT_FIXED, command.getSrcMethod(), command.getRemedDesc());
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

        int previousStat = requireStat(current);
        int targetStat = resolveVerifyFixTarget(command.getVerifyResult());
        InstanceStateResult result = executeStateChange(partnerId, current, targetStat, null, null);
        scanFollowUpService.scheduleVerifyFixScan(
                partnerId, current.getVulInfoId(), previousStat, targetStat, null);
        return result;
    }

    /** 校验实例存在且归属 Partner */
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

    /** 执行状态变更 */
    private InstanceStateResult executeStateChange(String partnerId, InstanceItemResult current,
                                                    int targetStat, String srcMethod, String remedDesc) {
        int previousStat = current.getVulInfoStat();
        instanceRepository.updateInstanceState(current.getId(), targetStat, srcMethod, remedDesc);

        InstanceStateResult result = new InstanceStateResult();
        result.setVulInfoId(current.getVulInfoId());
        result.setPreviousStat(previousStat);
        result.setCurrentStat(targetStat);
        return result;
    }
}