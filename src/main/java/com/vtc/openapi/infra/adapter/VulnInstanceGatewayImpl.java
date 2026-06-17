package com.vtc.openapi.infra.adapter;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.vtc.openapi.domain.instance.model.command.RemediateInstanceCommand;
import com.vtc.openapi.domain.instance.model.command.SearchInstanceCommand;
import com.vtc.openapi.domain.instance.model.result.InstanceItemResult;
import com.vtc.openapi.domain.instance.model.result.InstancePageResult;
import com.vtc.openapi.domain.open.OpenApiConstants;
import com.vtc.openapi.domain.open.OpenApiException;
import com.vtc.openapi.infra.feign.IVulPassInstanceFeign;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "open-api.engine.adapter-mode", havingValue = "vul-pass", matchIfMissing = true)
public class VulnInstanceGatewayImpl implements IVulnInstanceGateway {

    private static final Logger log = LoggerFactory.getLogger(VulnInstanceGatewayImpl.class);

    private final IVulPassInstanceFeign instanceFeign;

    public VulnInstanceGatewayImpl(IVulPassInstanceFeign instanceFeign) {
        this.instanceFeign = instanceFeign;
    }

    @Override
    public InstancePageResult searchInstances(SearchInstanceCommand command) {
        Map<String, Object> params = buildSearchParams(command);
        try {
            String body = instanceFeign.pageInstances(params);
            return parsePageResult(body);
        } catch (OpenApiException ex) {
            throw ex;
        } catch (FeignException ex) {
            log.warn("vul-pass pageInstances failed: status={} msg={}", ex.status(), ex.getMessage());
            throw new OpenApiException(OpenApiConstants.CODE_ENGINE_FAILED, "Engine query instances failed");
        } catch (Exception ex) {
            log.warn("vul-pass pageInstances error", ex);
            throw new OpenApiException(OpenApiConstants.CODE_ENGINE_FAILED, "Engine query instances failed");
        }
    }

    @Override
    public InstanceItemResult findByVulInfoId(String vulInfoId) {
        Map<String, Object> params = new HashMap<>();
        params.put("current", "1");
        params.put("size", "1");
        params.put("vulInfoIds", vulInfoId);
        try {
            String body = instanceFeign.pageInstances(params);
            InstancePageResult page = parsePageResult(body);
            if (page.getItems() == null || page.getItems().isEmpty()) {
                return null;
            }
            return page.getItems().get(0);
        } catch (OpenApiException ex) {
            throw ex;
        } catch (FeignException ex) {
            log.warn("vul-pass findByVulInfoId failed: vulInfoId={} status={}", vulInfoId, ex.status());
            throw new OpenApiException(OpenApiConstants.CODE_ENGINE_FAILED, "Engine query instance detail failed");
        } catch (Exception ex) {
            log.warn("vul-pass findByVulInfoId error: vulInfoId={}", vulInfoId, ex);
            throw new OpenApiException(OpenApiConstants.CODE_ENGINE_FAILED, "Engine query instance detail failed");
        }
    }

    @Override
    public void updateInstance(Long id, int vulInfoStat, String method, String remedDesc) {
        Map<String, Object> body = new HashMap<>();
        body.put("id", id);
        body.put("vulInfoStat", vulInfoStat);
        if (StringUtils.hasText(method)) {
            body.put("method", method);
        }
        if (StringUtils.hasText(remedDesc)) {
            body.put("remedDesc", remedDesc);
        }
        invokeUpdate(body, id);
    }

    @Override
    public void updateRemediateInstance(Long id, int vulInfoStat, RemediateInstanceCommand command) {
        Map<String, Object> body = new HashMap<>();
        body.put("id", id);
        body.put("vulInfoStat", vulInfoStat);
        if (command == null) {
            invokeUpdate(body, id);
            return;
        }
        if (command.getSrcMethod() != null) {
            body.put("method", String.valueOf(command.getSrcMethod()));
        }
        if (StringUtils.hasText(command.getRemedDesc())) {
            body.put("remedDesc", command.getRemedDesc());
        }
        if (StringUtils.hasText(command.getFixLnk())) {
            body.put("fixLnk", command.getFixLnk());
        }
        if (StringUtils.hasText(command.getRemedTime())) {
            body.put("remedTime", command.getRemedTime());
        }
        if (StringUtils.hasText(command.getDefDev())) {
            body.put("defDev", command.getDefDev());
        }
        if (command.getLvRsn() != null) {
            body.put("lvRsn", command.getLvRsn());
        }
        if (StringUtils.hasText(command.getArchiveReason())) {
            body.put("archiveReason", command.getArchiveReason());
        }
        invokeUpdate(body, id);
    }

    private void invokeUpdate(Map<String, Object> body, Long id) {
        try {
            instanceFeign.updateInstance(body);
        } catch (OpenApiException ex) {
            throw ex;
        } catch (FeignException ex) {
            log.warn("vul-pass updateInstance failed: id={} status={}", id, ex.status());
            throw new OpenApiException(OpenApiConstants.CODE_ENGINE_FAILED, "Engine update instance failed");
        } catch (Exception ex) {
            log.warn("vul-pass updateInstance error: id={}", id, ex);
            throw new OpenApiException(OpenApiConstants.CODE_ENGINE_FAILED, "Engine update instance failed");
        }
    }

    private Map<String, Object> buildSearchParams(SearchInstanceCommand command) {
        Map<String, Object> params = new HashMap<>();
        params.put("current", String.valueOf(command.getPage()));
        params.put("size", String.valueOf(command.getSize()));
        if (StringUtils.hasText(command.getTaskId())) {
            params.put("taskId", command.getTaskId());
        }
        if (!CollectionUtils.isEmpty(command.getVulInfoStatList())) {
            params.put("vulInfoStat", command.getVulInfoStatList());
        }
        if (StringUtils.hasText(command.getVulNetAddr())) {
            params.put("vulNetAddr", command.getVulNetAddr());
        }
        if (StringUtils.hasText(command.getAssetName())) {
            params.put("assetName", command.getAssetName());
        }
        if (StringUtils.hasText(command.getVulName())) {
            params.put("vulName", command.getVulName());
        }
        if (StringUtils.hasText(command.getOrgVulId())) {
            params.put("orgVulId", command.getOrgVulId());
        }
        if (StringUtils.hasText(command.getVulId())) {
            params.put("vulId", command.getVulId());
        }
        if (command.getIsAccess() != null) {
            params.put("isAccess", String.valueOf(command.getIsAccess()));
        }
        if (StringUtils.hasText(command.getUnitType())) {
            params.put("unitType", command.getUnitType());
        }
        return params;
    }

    private InstancePageResult parsePageResult(String body) {
        if (!StringUtils.hasText(body)) {
            InstancePageResult result = new InstancePageResult();
            result.setItems(new ArrayList<>());
            result.setTotal(0L);
            return result;
        }
        JSONObject root = JSON.parseObject(body);
        JSONArray records = resolveRecords(root);
        InstancePageResult result = new InstancePageResult();
        if (records == null || records.isEmpty()) {
            result.setItems(new ArrayList<>());
            result.setTotal(0L);
            return result;
        }
        Long total = root.getLong("total");
        if (total == null && root.containsKey("data")) {
            Object data = root.get("data");
            if (data instanceof JSONObject) {
                total = ((JSONObject) data).getLong("total");
            }
        }
        result.setTotal(total != null ? total : (long) records.size());
        List<InstanceItemResult> items = new ArrayList<>();
        for (int i = 0; i < records.size(); i++) {
            items.add(mapItem(records.getJSONObject(i)));
        }
        result.setItems(items);
        return result;
    }

    private JSONArray resolveRecords(JSONObject root) {
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
        return records;
    }

    private InstanceItemResult mapItem(JSONObject src) {
        InstanceItemResult item = new InstanceItemResult();
        item.setId(src.getLong("id"));
        item.setVulInfoId(src.getString("vulInfoId"));
        item.setVulId(src.getString("vulId"));
        item.setVulInfoStat(src.getInteger("vulInfoStat"));
        item.setLvRsn(src.getInteger("lvRsn"));
        item.setVulName(src.getString("vulName"));
        item.setOrgVulId(src.getString("orgVulId"));
        item.setVulNetAddr(src.getString("vulNetAddr"));
        item.setVulPort(src.getInteger("vulPort"));
        item.setVulSvc(src.getString("vulSvc"));
        item.setIsAccess(src.getInteger("isAccess"));
        item.setAssetId(src.getString("assetId"));
        item.setAssetName(src.getString("assetName"));
        item.setVulInstCpe(src.getString("vulInstCpe"));
        item.setVulInstVendor(src.getString("vulInstVendor"));
        item.setVulInstClass(src.getString("vulInstClass"));
        item.setVulInstName(src.getString("vulInstName"));
        item.setVulInstVer(src.getString("vulInstVer"));
        item.setRemedDesc(src.getString("remedDesc"));
        item.setFixLnk(src.getString("fixLnk"));
        item.setRemedTime(src.getString("remedTime"));
        item.setMethod(src.getInteger("method"));
        item.setVulAddrType(src.getInteger("vulAddrType"));
        item.setVulTransProto(src.getString("vulTransProto"));
        item.setUnitType(src.getString("unitType"));
        return item;
    }
}
