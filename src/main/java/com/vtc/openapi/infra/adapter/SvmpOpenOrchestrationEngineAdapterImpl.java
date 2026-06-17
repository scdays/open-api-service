package com.vtc.openapi.infra.adapter;

import com.vtc.openapi.domain.open.OpenApiConstants;
import com.vtc.openapi.domain.open.OpenApiException;
import com.vtc.openapi.infra.adapter.dto.SvmpTaskCreateRequest;
import com.vtc.openapi.infra.adapter.dto.SvmpTaskCreateResult;
import com.vtc.openapi.infra.adapter.dto.SvmpTaskProgressResult;
import com.vtc.openapi.infra.feign.IVulPassOpenTaskFeign;
import com.vtc.openapi.infra.feign.dto.VulPassCreateOpenTaskRequest;
import com.vtc.openapi.infra.feign.dto.VulPassCreateOpenTaskResponse;
import com.vtc.openapi.infra.feign.dto.VulPassOpenScanTargets;
import com.vtc.openapi.infra.feign.dto.VulPassOpenTaskProgressResponse;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * 开放平台 → vul-pass OPEN 编排 Internal API（SOC 双扫，不经考核 dispatch）。
 */
@Component
@ConditionalOnExpression("'${open-api.engine.adapter-mode:vul-pass}'.equals('vul-pass') "
        + "&& '${open-api.svmp.orchestration.enabled:false}'.equals('true')")
public class SvmpOpenOrchestrationEngineAdapterImpl implements SvmpEngineAdapter {

    private static final Logger log = LoggerFactory.getLogger(SvmpOpenOrchestrationEngineAdapterImpl.class);

    private final IVulPassOpenTaskFeign openTaskFeign;

    public SvmpOpenOrchestrationEngineAdapterImpl(IVulPassOpenTaskFeign openTaskFeign) {
        this.openTaskFeign = openTaskFeign;
    }

    @Override
    public SvmpTaskCreateResult createTask(SvmpTaskCreateRequest request) {
        Map<String, Object> options = request.getOptions();
        String partnerId = stringOption(options, "partnerId");
        String platformTaskId = stringOption(options, "platformTaskId");
        String extTaskId = stringOption(options, "extTaskId");
        if (!StringUtils.hasText(partnerId) || !StringUtils.hasText(platformTaskId) || !StringUtils.hasText(extTaskId)) {
            throw new OpenApiException(OpenApiConstants.CODE_ENGINE_FAILED,
                    "编排创建缺少 partnerId/platformTaskId/extTaskId");
        }
        VulPassCreateOpenTaskRequest feignReq = new VulPassCreateOpenTaskRequest();
        feignReq.setPartnerId(partnerId);
        feignReq.setPlatformTaskId(platformTaskId);
        feignReq.setExtTaskId(extTaskId);
        feignReq.setTaskName(request.getTaskName());
        feignReq.setVulnType(request.getVulnType());
        feignReq.setScanTemplateId(request.getScanTemplateId());
        feignReq.setScanPolicy(resolveScanPolicy(options));
        feignReq.setSrcMethod(intOption(options, "srcMethod"));
        feignReq.setCallbackUrl(stringOption(options, "callbackUrl"));
        feignReq.setReportTemplateId(intOption(options, "reportTemplateId"));
        VulPassOpenScanTargets targets = new VulPassOpenScanTargets();
        if (!CollectionUtils.isEmpty(request.getTargets())) {
            targets.setHosts(String.join(",", request.getTargets()));
        }
        if (options != null && options.get("auth") instanceof List) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> auth = (List<Map<String, Object>>) options.get("auth");
            targets.setAuth(auth);
        }
        feignReq.setTargets(targets);
        try {
            VulPassCreateOpenTaskResponse response = openTaskFeign.createTask(feignReq);
            if (response == null || !"ACCEPTED".equalsIgnoreCase(response.getStatus())
                    || response.getPassTaskId() == null) {
                String msg = response != null ? response.getMessage() : "empty response";
                throw new OpenApiException(OpenApiConstants.CODE_ENGINE_FAILED,
                        StringUtils.hasText(msg) ? msg : "OPEN 编排创建被拒绝");
            }
            return new SvmpTaskCreateResult(String.valueOf(response.getPassTaskId()));
        } catch (OpenApiException ex) {
            throw ex;
        } catch (FeignException ex) {
            log.warn("vul-pass open orchestration create failed: status={}", ex.status());
            throw new OpenApiException(OpenApiConstants.CODE_ENGINE_FAILED, "OPEN 编排创建失败");
        } catch (Exception ex) {
            log.warn("vul-pass open orchestration create error", ex);
            throw new OpenApiException(OpenApiConstants.CODE_ENGINE_FAILED, "OPEN 编排创建失败");
        }
    }

    @Override
    public SvmpTaskProgressResult getTaskProgress(String engineTaskId) {
        Long passTaskId = parseLongId(engineTaskId);
        try {
            VulPassOpenTaskProgressResponse response = openTaskFeign.getTask(passTaskId);
            if (response == null) {
                throw new OpenApiException(OpenApiConstants.CODE_ENGINE_FAILED, "编排任务不存在");
            }
            SvmpTaskProgressResult result = new SvmpTaskProgressResult();
            result.setStatus(response.getStatus());
            result.setProgress(response.getProgress());
            return result;
        } catch (OpenApiException ex) {
            throw ex;
        } catch (FeignException ex) {
            log.warn("vul-pass open orchestration progress failed: passTaskId={} status={}",
                    passTaskId, ex.status());
            throw new OpenApiException(OpenApiConstants.CODE_ENGINE_FAILED, "编排进度查询失败");
        } catch (Exception ex) {
            log.warn("vul-pass open orchestration progress error: passTaskId={}", passTaskId, ex);
            throw new OpenApiException(OpenApiConstants.CODE_ENGINE_FAILED, "编排进度查询失败");
        }
    }

    private static String resolveScanPolicy(Map<String, Object> options) {
        String policy = stringOption(options, "scanPolicy");
        return StringUtils.hasText(policy) ? policy : "SOC_DUAL";
    }

    private static String stringOption(Map<String, Object> options, String key) {
        if (options == null || !options.containsKey(key)) {
            return null;
        }
        Object val = options.get(key);
        return val != null ? val.toString() : null;
    }

    private static Integer intOption(Map<String, Object> options, String key) {
        if (options == null || !options.containsKey(key)) {
            return null;
        }
        Object val = options.get(key);
        if (val instanceof Number) {
            return ((Number) val).intValue();
        }
        try {
            return Integer.parseInt(val.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static Long parseLongId(String engineTaskId) {
        try {
            return Long.parseLong(engineTaskId.trim());
        } catch (NumberFormatException ex) {
            throw new OpenApiException(OpenApiConstants.CODE_ENGINE_FAILED, "engineTaskId 非法");
        }
    }
}
