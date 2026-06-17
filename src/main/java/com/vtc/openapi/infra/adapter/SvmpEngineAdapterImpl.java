package com.vtc.openapi.infra.adapter;



import com.alibaba.fastjson.JSON;

import com.alibaba.fastjson.JSONArray;

import com.alibaba.fastjson.JSONObject;

import com.vtc.openapi.infra.adapter.dto.SvmpTaskCreateRequest;

import com.vtc.openapi.infra.adapter.dto.SvmpTaskCreateResult;

import com.vtc.openapi.infra.adapter.dto.SvmpTaskProgressResult;

import com.vtc.openapi.domain.open.OpenApiConstants;

import com.vtc.openapi.domain.open.OpenApiException;

import com.vtc.openapi.infra.config.OpenApiProperties;

import com.vtc.openapi.infra.feign.IVulPassScanTaskFeign;

import com.vtc.openapi.infra.feign.dto.VulPassDispatchAsset;

import com.vtc.openapi.infra.feign.dto.VulPassDispatchRequest;

import feign.FeignException;

import org.slf4j.Logger;

import org.slf4j.LoggerFactory;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import org.springframework.util.CollectionUtils;

import org.springframework.util.StringUtils;



import java.util.ArrayList;

import java.util.Collections;

import java.util.List;

import java.util.Map;

import java.util.stream.Collectors;



/**

 * 开放平台逻辑契约 ↔ vul-pass 真实 REST 翻译层。

 * <ul>

 *   <li>创建：{@code POST /task/create} → {@code POST /vul-scan-task/dispatch}</li>

 *   <li>进度：{@code GET /task/progress} → {@code GET /vul-scan-task/page2?id=}</li>

 * </ul>

 */

@Component
@ConditionalOnExpression("'${open-api.engine.adapter-mode:vul-pass}'.equals('vul-pass') "
        + "&& !'${open-api.svmp.orchestration.enabled:false}'.equals('true')")
public class SvmpEngineAdapterImpl implements SvmpEngineAdapter {



    private static final Logger log = LoggerFactory.getLogger(SvmpEngineAdapterImpl.class);



    private final IVulPassScanTaskFeign vulPassFeign;

    private final OpenApiProperties properties;



    public SvmpEngineAdapterImpl(IVulPassScanTaskFeign vulPassFeign, OpenApiProperties properties) {

        this.vulPassFeign = vulPassFeign;

        this.properties = properties;

    }



    @Override

    public SvmpTaskCreateResult createTask(SvmpTaskCreateRequest request) {

        OpenApiProperties.Svmp.Dispatch dispatchCfg = properties.getSvmp().getDispatch();

        String orderId = resolveOrderId(request, dispatchCfg);

        if (!StringUtils.hasText(orderId)) {

            throw new OpenApiException(OpenApiConstants.CODE_ENGINE_FAILED,

                    "未配置 open-api.svmp.dispatch.order-id，无法调用 vul-pass 下发任务");

        }

        VulPassDispatchRequest dispatchReq = buildDispatchRequest(request, dispatchCfg, orderId);

        try {

            String body = vulPassFeign.dispatch(dispatchReq);

            assertVulPassSuccess(body, "vul-pass 下发任务失败");

            String engineTaskId = resolveEngineTaskId(orderId);

            return new SvmpTaskCreateResult(engineTaskId);

        } catch (OpenApiException ex) {

            throw ex;

        } catch (FeignException ex) {

            log.warn("vul-pass dispatch failed: status={} msg={}", ex.status(), ex.getMessage());

            throw new OpenApiException(OpenApiConstants.CODE_ENGINE_FAILED, "引擎创建任务失败");

        } catch (Exception ex) {

            log.warn("vul-pass dispatch error", ex);

            throw new OpenApiException(OpenApiConstants.CODE_ENGINE_FAILED, "引擎创建任务失败");

        }

    }



    @Override

    public SvmpTaskProgressResult getTaskProgress(String engineTaskId) {

        if (!StringUtils.hasText(engineTaskId)) {

            throw new OpenApiException(OpenApiConstants.CODE_ENGINE_FAILED, "engineTaskId 为空");

        }

        Long taskId = parseLongId(engineTaskId);

        try {

            String body = vulPassFeign.pageTasks(1, 1, taskId, null);

            JSONObject task = firstTaskRecord(body);

            if (task == null) {

                throw new OpenApiException(OpenApiConstants.CODE_ENGINE_FAILED, "引擎任务不存在");

            }

            return mapTaskProgress(task);

        } catch (OpenApiException ex) {

            throw ex;

        } catch (FeignException ex) {

            log.warn("vul-pass page2 failed: taskId={} status={}", engineTaskId, ex.status());

            throw new OpenApiException(OpenApiConstants.CODE_ENGINE_FAILED, "引擎查询进度失败");

        } catch (Exception ex) {

            log.warn("vul-pass page2 error: taskId={}", engineTaskId, ex);

            throw new OpenApiException(OpenApiConstants.CODE_ENGINE_FAILED, "引擎查询进度失败");

        }

    }



    private VulPassDispatchRequest buildDispatchRequest(SvmpTaskCreateRequest request,

                                                        OpenApiProperties.Svmp.Dispatch dispatchCfg,

                                                        String orderId) {

        VulPassDispatchRequest dispatchReq = new VulPassDispatchRequest();

        dispatchReq.setOrderId(orderId);

        dispatchReq.setTskPhase(resolveIntOption(request, "tskPhase", dispatchCfg.getTskPhase()));

        dispatchReq.setProcMethod(resolveIntOption(request, "procMethod", dispatchCfg.getProcMethod()));

        dispatchReq.setTskName(request.getTaskName());

        dispatchReq.setTskType(10);

        dispatchReq.setTskModel(1);

        dispatchReq.setEngHashes(resolveEngHashes(request, dispatchCfg));

        dispatchReq.setAssetList(buildAssetList(request));

        return dispatchReq;

    }



    private List<VulPassDispatchAsset> buildAssetList(SvmpTaskCreateRequest request) {

        if (CollectionUtils.isEmpty(request.getTargets())) {

            return Collections.emptyList();

        }

        return request.getTargets().stream()

                .filter(StringUtils::hasText)

                .map(String::trim)

                .map(VulPassDispatchAsset::new)

                .collect(Collectors.toList());

    }



    private List<String> resolveEngHashes(SvmpTaskCreateRequest request,

                                            OpenApiProperties.Svmp.Dispatch dispatchCfg) {

        Object fromOptions = optionValue(request, "engHashes");

        if (fromOptions instanceof List) {

            List<?> raw = (List<?>) fromOptions;

            List<String> hashes = new ArrayList<>();

            for (Object item : raw) {

                if (item != null && StringUtils.hasText(item.toString())) {

                    hashes.add(item.toString().trim());

                }

            }

            if (!hashes.isEmpty()) {

                return hashes;

            }

        }

        if (!CollectionUtils.isEmpty(dispatchCfg.getEngHashes())) {

            return dispatchCfg.getEngHashes();

        }

        return Collections.singletonList("NA");

    }



    private String resolveOrderId(SvmpTaskCreateRequest request, OpenApiProperties.Svmp.Dispatch dispatchCfg) {

        Object fromOptions = optionValue(request, "orderId");

        if (fromOptions != null && StringUtils.hasText(fromOptions.toString())) {

            return fromOptions.toString().trim();

        }

        return dispatchCfg.getOrderId();

    }



    private Integer resolveIntOption(SvmpTaskCreateRequest request, String key, Integer defaultValue) {

        Object value = optionValue(request, key);

        if (value instanceof Number) {

            return ((Number) value).intValue();

        }

        if (value != null && StringUtils.hasText(value.toString())) {

            return Integer.parseInt(value.toString().trim());

        }

        return defaultValue;

    }



    private Object optionValue(SvmpTaskCreateRequest request, String key) {

        Map<String, Object> options = request.getOptions();

        if (options == null || !options.containsKey(key)) {

            return null;

        }

        return options.get(key);

    }



    private String resolveEngineTaskId(String orderId) {

        String body = vulPassFeign.pageTasks(1, 1, null, orderId);

        JSONObject task = firstTaskRecord(body);

        if (task == null) {

            throw new OpenApiException(OpenApiConstants.CODE_ENGINE_FAILED, "vul-pass 下发成功但未找到任务 ID");

        }

        String id = firstNonBlank(task.getString("id"), task.getString("taskId"));

        if (id == null) {

            throw new OpenApiException(OpenApiConstants.CODE_ENGINE_FAILED, "vul-pass 响应缺少任务 ID");

        }

        return id;

    }



    private SvmpTaskProgressResult mapTaskProgress(JSONObject task) {

        SvmpTaskProgressResult result = new SvmpTaskProgressResult();

        Integer progress = task.getInteger("tskProgress");

        if (progress == null) {

            progress = task.getInteger("progress");

        }

        result.setProgress(progress != null ? progress : 0);

        result.setStatus(mapVulPassStatus(task.getInteger("tskStat"), result.getProgress()));

        result.setErrorMessage(firstNonBlank(task.getString("errorMessage"), task.getString("retMsg")));

        return result;

    }



    private String mapVulPassStatus(Integer tskStat, int progress) {

        if (tskStat != null) {

            switch (tskStat) {

                case 1:

                    return "FINISHED";

                case 2:

                    return "FAILED";

                case 3:

                    return "RUNNING";

                case 0:

                default:

                    return progress > 0 ? "RUNNING" : "PENDING";

            }

        }

        if (progress >= 100) {

            return "FINISHED";

        }

        if (progress > 0) {

            return "RUNNING";

        }

        return "PENDING";

    }



    private void assertVulPassSuccess(String body, String defaultMessage) {

        if (body == null || body.trim().isEmpty()) {

            throw new OpenApiException(OpenApiConstants.CODE_ENGINE_FAILED, "引擎响应为空");

        }

        JSONObject root = JSON.parseObject(body);

        Boolean success = root.getBoolean("success");

        if (success != null) {

            if (!success) {

                throw new OpenApiException(OpenApiConstants.CODE_ENGINE_FAILED,

                        firstNonBlank(root.getString("message"), defaultMessage));

            }

            return;

        }

        String retCode = firstNonBlank(root.getString("ret_code"), root.getString("retCode"), root.getString("code"));

        if (retCode != null && !"0".equals(retCode) && !"200".equals(retCode)) {

            throw new OpenApiException(OpenApiConstants.CODE_ENGINE_FAILED,

                    firstNonBlank(root.getString("ret_msg"), root.getString("message"), defaultMessage));

        }

    }



    private JSONObject firstTaskRecord(String body) {

        if (!StringUtils.hasText(body)) {

            return null;

        }

        JSONObject root = JSON.parseObject(body);

        JSONArray records = root.getJSONArray("records");

        if (records == null) {

            records = root.getJSONArray("list");

        }

        if (records == null && root.containsKey("data")) {

            Object data = root.get("data");

            if (data instanceof JSONObject) {

                JSONObject dataObj = (JSONObject) data;

                records = dataObj.getJSONArray("records");

                if (records == null) {

                    records = dataObj.getJSONArray("list");

                }

            } else if (data instanceof JSONArray) {

                records = (JSONArray) data;

            }

        }

        if (records == null || records.isEmpty()) {

            return null;

        }

        return records.getJSONObject(0);

    }



    private Long parseLongId(String engineTaskId) {

        try {

            return Long.parseLong(engineTaskId.trim());

        } catch (NumberFormatException ex) {

            throw new OpenApiException(OpenApiConstants.CODE_ENGINE_FAILED, "engineTaskId 格式无效");

        }

    }



    private static String firstNonBlank(String... values) {

        if (values == null) {

            return null;

        }

        for (String v : values) {

            if (v != null && !v.trim().isEmpty()) {

                return v.trim();

            }

        }

        return null;

    }

}


