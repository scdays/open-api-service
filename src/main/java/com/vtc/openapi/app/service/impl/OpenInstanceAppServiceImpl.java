package com.vtc.openapi.app.service.impl;

import com.vtc.openapi.app.open.InvocationPipeline;
import com.vtc.openapi.app.service.IOpenInstanceAppService;
import com.vtc.openapi.domain.instance.model.command.RemediateInstanceCommand;
import com.vtc.openapi.domain.instance.model.command.SearchInstanceCommand;
import com.vtc.openapi.domain.instance.model.command.VerifyFixInstanceCommand;
import com.vtc.openapi.domain.instance.model.command.VerifyInstanceCommand;
import com.vtc.openapi.domain.instance.model.result.InstanceItemResult;
import com.vtc.openapi.domain.instance.model.result.InstancePageResult;
import com.vtc.openapi.domain.instance.model.result.InstanceStateResult;
import com.vtc.openapi.domain.instance.service.business.IInstanceDomainService;
import com.vtc.openapi.domain.instance.service.business.IVerifyFixJobDomainService;
import com.vtc.openapi.domain.open.OpenApiConstants;
import com.vtc.openapi.domain.open.OpenApiException;
import com.vtc.openapi.domain.open.OpenApiOperations;
import com.vtc.openapi.domain.partner.context.PartnerContext;
import com.vtc.openapi.ui.dto.ApiResponse;
import com.vtc.openapi.ui.dto.open.instance.InstanceBatchFailedItem;
import com.vtc.openapi.ui.dto.open.instance.InstanceBatchOperationRequest;
import com.vtc.openapi.ui.dto.open.instance.InstanceBatchOperationRequest.BatchItem;
import com.vtc.openapi.ui.dto.open.instance.InstanceBatchOperationResponse;
import com.vtc.openapi.ui.dto.open.instance.InstanceDetailDto;
import com.vtc.openapi.ui.dto.open.instance.InstanceDto;
import com.vtc.openapi.ui.dto.open.instance.InstanceOperationResponse;
import com.vtc.openapi.ui.dto.open.instance.InstanceSearchRequest;
import com.vtc.openapi.ui.dto.open.instance.InstanceSearchResponse;
import com.vtc.openapi.ui.dto.open.instance.RemediateInstanceRequest;
import com.vtc.openapi.ui.dto.open.instance.VerifyFixInstanceRequest;
import com.vtc.openapi.ui.dto.open.instance.VerifyInstanceRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 实例应用服务：DTO 转换 + InvocationPipeline 编排 + 批量聚合。
 */
@Service
public class OpenInstanceAppServiceImpl implements IOpenInstanceAppService {

    /** 批量操作最大条数（与 OpenAPI maxItems 一致） */
    private static final int MAX_BATCH_SIZE = 500;

    private final InvocationPipeline invocationPipeline;
    private final IInstanceDomainService instanceDomainService;
    private final IVerifyFixJobDomainService verifyFixJobDomainService;

    public OpenInstanceAppServiceImpl(InvocationPipeline invocationPipeline,
                                      IInstanceDomainService instanceDomainService,
                                      IVerifyFixJobDomainService verifyFixJobDomainService) {
        this.invocationPipeline = invocationPipeline;
        this.instanceDomainService = instanceDomainService;
        this.verifyFixJobDomainService = verifyFixJobDomainService;
    }

    @Override
    public ApiResponse<InstanceSearchResponse> searchInstances(InstanceSearchRequest request,
                                                               String exportProfile) {
        if (request == null) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "请求体不能为空");
        }
        if (request.getPage() == null || request.getPage() < 1) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "page 必须为正整数");
        }
        if (request.getSize() == null || request.getSize() < 1 || request.getSize() > 1000) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "size 必须为 1–1000");
        }

        return invocationPipeline.invoke(OpenApiOperations.SEARCH_INSTANCES, ctx -> {
            String partnerId = PartnerContext.requirePartnerId();
            SearchInstanceCommand command = toCommand(request);
            if (StringUtils.hasText(exportProfile)) {
                command.setExportProfile(exportProfile.trim());
            }
            InstancePageResult result = instanceDomainService.search(partnerId, command);
            return toSearchResponse(result);
        });
    }

    @Override
    public ApiResponse<InstanceDetailDto> getInstance(String vulInfoId) {
        if (vulInfoId == null || vulInfoId.trim().isEmpty()) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "vulInfoID 不能为空");
        }

        return invocationPipeline.invoke(OpenApiOperations.GET_INSTANCE, ctx -> {
            String partnerId = PartnerContext.requirePartnerId();
            InstanceItemResult result = instanceDomainService.getByVulInfoId(partnerId, vulInfoId);
            if (result == null) {
                throw new OpenApiException(OpenApiConstants.CODE_CROSS_PARTNER,
                        "实例不存在或无权访问");
            }
            return toDetailDto(result);
        });
    }

    @Override
    public ApiResponse<InstanceOperationResponse> verifyInstance(String vulInfoId, VerifyInstanceRequest request) {
        if (vulInfoId == null || vulInfoId.trim().isEmpty()) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "vulInfoID 不能为空");
        }

        return invocationPipeline.invoke(OpenApiOperations.VERIFY_INSTANCE, ctx -> {
            String partnerId = PartnerContext.requirePartnerId();
            VerifyInstanceCommand command = toVerifyCommand(vulInfoId, request);
            InstanceStateResult result = instanceDomainService.verify(partnerId, command);
            return toOperationResponse(result);
        });
    }

    @Override
    public ApiResponse<InstanceOperationResponse> remediateInstance(String vulInfoId, RemediateInstanceRequest request) {
        if (vulInfoId == null || vulInfoId.trim().isEmpty()) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "vulInfoID 不能为空");
        }

        return invocationPipeline.invoke(OpenApiOperations.REMEDIATE_INSTANCE, ctx -> {
            String partnerId = PartnerContext.requirePartnerId();
            RemediateInstanceCommand command = toRemediateCommand(vulInfoId, request);
            InstanceStateResult result = instanceDomainService.remediate(partnerId, command);
            return toOperationResponse(result);
        });
    }

    @Override
    public ApiResponse<InstanceOperationResponse> verifyFixInstance(String vulInfoId, VerifyFixInstanceRequest request) {
        if (vulInfoId == null || vulInfoId.trim().isEmpty()) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "vulInfoID 不能为空");
        }

        return invocationPipeline.invoke(OpenApiOperations.VERIFY_FIX_INSTANCE, ctx -> {
            String partnerId = PartnerContext.requirePartnerId();
            VerifyFixInstanceCommand command = new VerifyFixInstanceCommand();
            command.setVulInfoId(vulInfoId);
            if (request != null) {
                command.setTransferTime(request.getTransferTime());
                command.setRemark(request.getRemark());
            }
            InstanceStateResult result = instanceDomainService.verifyFix(partnerId, command);
            return toOperationResponse(result);
        });
    }

    @Override
    public ApiResponse<InstanceBatchOperationResponse> verifyInstanceBatch(InstanceBatchOperationRequest request) {
        validateBatchRequest(request);
        if (!StringUtils.hasText(request.getOperator())) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "operator 不能为空");
        }
        return invocationPipeline.invoke(OpenApiOperations.VERIFY_INSTANCE_BATCH, ctx -> {
            String partnerId = PartnerContext.requirePartnerId();
            return executeBatch(partnerId, request.getItems(), (pid, item) -> {
                VerifyInstanceCommand cmd = new VerifyInstanceCommand();
                cmd.setVulInfoId(item.getVulInfoID());
                cmd.setVulnType(item.getVulnType());
                cmd.setVerifyResult(item.getVerifyResult());
                cmd.setSrcMethod(item.getSrcMethod());
                cmd.setTransferTime(item.getTransferTime());
                cmd.setOperator(request.getOperator());
                cmd.setRemark(item.getRemark());
                return instanceDomainService.verify(pid, cmd);
            });
        });
    }

    @Override
    public ApiResponse<InstanceBatchOperationResponse> remediateInstanceBatch(InstanceBatchOperationRequest request) {
        validateBatchRequest(request);
        return invocationPipeline.invoke(OpenApiOperations.REMEDIATE_INSTANCE_BATCH, ctx -> {
            String partnerId = PartnerContext.requirePartnerId();
            return executeBatch(partnerId, request.getItems(), (pid, item) -> {
                RemediateInstanceCommand cmd = toRemediateCommand(item.getVulInfoID(), item);
                return instanceDomainService.remediate(pid, cmd);
            });
        });
    }

    @Override
    public ApiResponse<InstanceBatchOperationResponse> verifyFixInstanceBatch(InstanceBatchOperationRequest request) {
        validateBatchRequest(request);
        return invocationPipeline.invoke(OpenApiOperations.VERIFY_FIX_INSTANCE_BATCH, ctx -> {
            String partnerId = PartnerContext.requirePartnerId();
            boolean allAsync = request.getItems().stream()
                    .noneMatch(item -> StringUtils.hasText(item.getVerifyResult()));
            if (allAsync) {
                String batchId = "batch-" + System.currentTimeMillis();
                List<VerifyFixInstanceCommand> commands = new ArrayList<>();
                for (BatchItem item : request.getItems()) {
                    VerifyFixInstanceCommand cmd = new VerifyFixInstanceCommand();
                    cmd.setVulInfoId(item.getVulInfoID());
                    cmd.setTransferTime(item.getTransferTime());
                    cmd.setRemark(item.getRemark());
                    cmd.setBatchId(batchId);
                    commands.add(cmd);
                }
                List<InstanceStateResult> results = verifyFixJobDomainService.acceptBatch(
                        partnerId, batchId, commands);
                InstanceBatchOperationResponse response = new InstanceBatchOperationResponse();
                response.setSuccess(results.stream()
                        .map(this::toOperationResponse)
                        .collect(Collectors.toList()));
                response.setFailed(new ArrayList<>());
                return response;
            }
            return executeBatch(partnerId, request.getItems(), (pid, item) -> {
                VerifyFixInstanceCommand cmd = new VerifyFixInstanceCommand();
                cmd.setVulInfoId(item.getVulInfoID());
                cmd.setVerifyResult(item.getVerifyResult());
                cmd.setTransferTime(item.getTransferTime());
                cmd.setRemark(item.getRemark());
                return instanceDomainService.verifyFix(pid, cmd);
            });
        });
    }

    private void validateBatchRequest(InstanceBatchOperationRequest request) {
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "items 不能为空");
        }
        if (request.getItems().size() > MAX_BATCH_SIZE) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR,
                    "批量操作最多 " + MAX_BATCH_SIZE + " 条");
        }
    }

    @FunctionalInterface
    private interface BatchSingleHandler {
        InstanceStateResult execute(String partnerId, BatchItem item) throws OpenApiException;
    }

    private InstanceBatchOperationResponse executeBatch(String partnerId,
                                                       List<BatchItem> items,
                                                       BatchSingleHandler handler) {
        List<InstanceOperationResponse> successList = new ArrayList<>();
        List<InstanceBatchFailedItem> failedList = new ArrayList<>();

        for (BatchItem item : items) {
            try {
                InstanceStateResult result = handler.execute(partnerId, item);
                successList.add(toOperationResponse(result));
            } catch (OpenApiException ex) {
                InstanceBatchFailedItem failed = new InstanceBatchFailedItem();
                failed.setVulInfoID(item.getVulInfoID());
                failed.setCode(ex.getCode());
                failed.setMessage(ex.getMessage());
                failedList.add(failed);
            }
        }

        InstanceBatchOperationResponse resp = new InstanceBatchOperationResponse();
        resp.setSuccess(successList);
        resp.setFailed(failedList);
        return resp;
    }

    private SearchInstanceCommand toCommand(InstanceSearchRequest request) {
        SearchInstanceCommand cmd = new SearchInstanceCommand();
        cmd.setTaskId(request.getTaskId());
        cmd.setExtTaskId(request.getExtTaskId());
        cmd.setVulInfoStatList(request.getVulInfoStatList());
        cmd.setVulLevelList(request.getVulLevelList());
        cmd.setPage(request.getPage());
        cmd.setSize(request.getSize());
        return cmd;
    }

    private VerifyInstanceCommand toVerifyCommand(String vulInfoId, VerifyInstanceRequest request) {
        VerifyInstanceCommand command = new VerifyInstanceCommand();
        command.setVulInfoId(vulInfoId);
        if (request != null) {
            command.setVulnType(request.getVulnType());
            command.setVerifyResult(request.getVerifyResult());
            command.setSrcMethod(request.getSrcMethod());
            command.setTransferTime(request.getTransferTime());
            command.setOperator(request.getOperator());
            command.setRemark(request.getRemark());
        }
        return command;
    }

    private InstanceSearchResponse toSearchResponse(InstancePageResult result) {
        InstanceSearchResponse resp = new InstanceSearchResponse();
        resp.setPage(result.getPage());
        resp.setSize(result.getSize());
        resp.setTotal(result.getTotal());
        if (result.getItems() != null) {
            resp.setItems(result.getItems().stream()
                    .map(this::toInstanceDto)
                    .collect(Collectors.toList()));
        }
        return resp;
    }

    private InstanceDto toInstanceDto(InstanceItemResult r) {
        InstanceDto dto = new InstanceDto();
        dto.setVulInfoID(r.getVulInfoId());
        dto.setVulID(r.getVulId());
        dto.setVulInfoStat(r.getVulInfoStat());
        dto.setLvRsn(r.getLvRsn());
        dto.setVulName(r.getVulName());
        dto.setVulLevel(r.getVulLevel());
        dto.setOrgVulId(r.getOrgVulId());
        dto.setVulNetAddr(r.getVulNetAddr());
        dto.setVulPort(r.getVulPort());
        dto.setVulSvc(r.getVulSvc());
        dto.setIsAccess(r.getIsAccess());
        dto.setTransferTime(r.getTransferTime());
        dto.setVulnDisposalId(r.getVulnDisposalId());
        dto.setExtVulnRef(r.getExtVulnRef());
        return dto;
    }

    private InstanceDetailDto toDetailDto(InstanceItemResult r) {
        InstanceDetailDto dto = new InstanceDetailDto();
        dto.setVulInfoID(r.getVulInfoId());
        dto.setVulID(r.getVulId());
        dto.setVulInfoStat(r.getVulInfoStat());
        dto.setLvRsn(r.getLvRsn());
        dto.setVulName(r.getVulName());
        dto.setVulLevel(r.getVulLevel());
        dto.setOrgVulId(r.getOrgVulId());
        dto.setVulNetAddr(r.getVulNetAddr());
        dto.setVulPort(r.getVulPort());
        dto.setVulSvc(r.getVulSvc());
        dto.setIsAccess(r.getIsAccess());
        dto.setTransferTime(r.getTransferTime());
        dto.setVulnDisposalId(r.getVulnDisposalId());
        dto.setExtVulnRef(r.getExtVulnRef());
        dto.setVulAddrType(r.getVulAddrType());
        dto.setAssetID(r.getAssetId());
        dto.setAssetName(r.getAssetName());
        dto.setVulInstCpe(r.getVulInstCpe());
        dto.setVulInstVendor(r.getVulInstVendor());
        dto.setVulInstClass(r.getVulInstClass());
        dto.setVulInstName(r.getVulInstName());
        dto.setVulInstVer(r.getVulInstVer());
        dto.setRemedDesc(r.getRemedDesc());
        dto.setFixLnk(r.getFixLnk());
        dto.setDefDev(r.getDefDev());
        dto.setRemedTime(r.getRemedTime());
        dto.setSrcMethod(r.getMethod());
        dto.setVulTransProto(r.getVulTransProto());
        dto.setArchiveReason(r.getArchiveReason());
        dto.setProvincialFields(r.getProvincialFields());
        return dto;
    }

    private RemediateInstanceCommand toRemediateCommand(String vulInfoId, RemediateInstanceRequest request) {
        RemediateInstanceCommand command = new RemediateInstanceCommand();
        command.setVulInfoId(vulInfoId);
        if (request != null) {
            fillRemediateCommand(command, request);
        }
        return command;
    }

    private RemediateInstanceCommand toRemediateCommand(String vulInfoId, BatchItem item) {
        RemediateInstanceCommand command = new RemediateInstanceCommand();
        command.setVulInfoId(vulInfoId);
        if (item != null) {
            command.setVulInfoStat(item.getVulInfoStat());
            command.setSrcMethod(item.getSrcMethod());
            command.setRemedDesc(item.getRemedDesc());
            command.setFixLnk(item.getFixLnk());
            command.setDefDev(item.getDefDev());
            command.setRemedTime(item.getRemedTime());
            command.setLvRsn(item.getLvRsn());
            command.setArchiveReason(item.getArchiveReason());
            command.setApprovedBy(item.getApprovedBy());
            command.setRecordAt(item.getRecordAt());
            command.setProvincialFields(item.getProvincialFields());
            command.setSrcTktRole(item.getSrcTktRole());
            command.setDstTktRole(item.getDstTktRole());
            command.setAssignerDept(item.getAssignerDept());
            command.setAssignerEmail(item.getAssignerEmail());
            command.setAssignerPhone(item.getAssignerPhone());
            command.setHandlerDept(item.getHandlerDept());
            command.setHandlerEmail(item.getHandlerEmail());
            command.setHandlerPhone(item.getHandlerPhone());
            command.setTransferTime(item.getTransferTime());
            command.setRemark(item.getRemark());
        }
        return command;
    }

    private static void fillRemediateCommand(RemediateInstanceCommand command, RemediateInstanceRequest request) {
        command.setVulInfoStat(request.getVulInfoStat());
        command.setSrcMethod(request.getSrcMethod());
        command.setRemedDesc(request.getRemedDesc());
        command.setFixLnk(request.getFixLnk());
        command.setDefDev(request.getDefDev());
        command.setRemedTime(request.getRemedTime());
        command.setLvRsn(request.getLvRsn());
        command.setArchiveReason(request.getArchiveReason());
        command.setApprovedBy(request.getApprovedBy());
        command.setRecordAt(request.getRecordAt());
        command.setProvincialFields(request.getProvincialFields());
        command.setSrcTktRole(request.getSrcTktRole());
        command.setDstTktRole(request.getDstTktRole());
        command.setAssignerDept(request.getAssignerDept());
        command.setAssignerEmail(request.getAssignerEmail());
        command.setAssignerPhone(request.getAssignerPhone());
        command.setHandlerDept(request.getHandlerDept());
        command.setHandlerEmail(request.getHandlerEmail());
        command.setHandlerPhone(request.getHandlerPhone());
        command.setTransferTime(request.getTransferTime());
        command.setRemark(request.getRemark());
    }

    private InstanceOperationResponse toOperationResponse(InstanceStateResult result) {
        InstanceOperationResponse resp = new InstanceOperationResponse();
        resp.setVulInfoID(result.getVulInfoId());
        resp.setVulInfoStat(result.getVulInfoStat());
        resp.setLvRsn(result.getLvRsn());
        resp.setTransferTime(result.getTransferTime());
        resp.setSrcMethod(result.getSrcMethod());
        resp.setRemedDesc(result.getRemedDesc());
        resp.setArchiveReason(result.getArchiveReason());
        resp.setVerifyFixJobId(result.getVerifyFixJobId());
        resp.setVerifyFixStatus(result.getVerifyFixStatus());
        resp.setMessage(result.getMessage());
        return resp;
    }
}
