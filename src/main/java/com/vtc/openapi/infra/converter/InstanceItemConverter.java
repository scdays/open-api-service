package com.vtc.openapi.infra.converter;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.botany.spore.ddd.infra.utils.convertor.ConvertHelper;
import com.vtc.openapi.domain.instance.model.entity.OpenVulnInstanceDO;
import com.vtc.openapi.domain.instance.model.result.InstanceItemResult;
import org.springframework.util.StringUtils;

/**
 * Maps fixture snapshot JSON and persisted rows to {@link InstanceItemResult}.
 */
public final class InstanceItemConverter {

    private InstanceItemConverter() {
    }

    public static InstanceItemResult fromSnapshot(OpenVulnInstanceDO row) {
        if (row == null) {
            return null;
        }
        InstanceItemResult item = new InstanceItemResult();
        item.setId(row.getId());
        item.setVulInfoId(row.getVulInfoId());
        if (StringUtils.hasText(row.getSnapshotJson())) {
            JSONObject snap = JSON.parseObject(row.getSnapshotJson());
            if (snap != null) {
                mapJsonFields(item, snap);
            }
        }
        if (row.getVulInfoStat() != null) {
            item.setVulInfoStat(row.getVulInfoStat());
        }
        if (StringUtils.hasText(row.getVulnDisposalId())) {
            item.setVulnDisposalId(row.getVulnDisposalId());
        }
        return item;
    }

    public static InstanceItemResult fromJson(JSONObject src) {
        if (src == null) {
            return null;
        }
        InstanceItemResult item = new InstanceItemResult();
        item.setId(src.getLong("id"));
        mapJsonFields(item, src);
        return item;
    }

    public static void mapJsonFields(InstanceItemResult item, JSONObject src) {
        if (item == null || src == null) {
            return;
        }
        item.setVulInfoId(firstOf(src.getString("vulInfoID"), src.getString("vulInfoId")));
        item.setVulId(firstOf(src.getString("vulID"), src.getString("vulId")));
        item.setVulInfoStat(src.getInteger("vulInfoStat"));
        item.setLvRsn(src.getInteger("lvRsn"));
        item.setVulName(src.getString("vulName"));
        item.setVulLevel(src.getInteger("vulLevel"));
        item.setOrgVulId(resolveOrgVulId(src));
        item.setVulNetAddr(src.getString("vulNetAddr"));
        item.setVulPort(src.getInteger("vulPort"));
        item.setVulSvc(src.getString("vulSvc"));
        item.setIsAccess(src.getInteger("isAccess"));
        item.setTransferTime(src.getString("transferTime"));
        item.setVulnDisposalId(src.getString("vulnDisposalId"));
        item.setAssetId(firstOf(src.getString("assetID"), src.getString("assetId")));
        item.setAssetName(src.getString("assetName"));
        item.setVulInstCpe(src.getString("vulInstCpe"));
        item.setVulInstVendor(src.getString("vulInstVendor"));
        item.setVulInstClass(src.getString("vulInstClass"));
        item.setVulInstName(src.getString("vulInstName"));
        item.setVulInstVer(src.getString("vulInstVer"));
        item.setRemedDesc(src.getString("remedDesc"));
        item.setFixLnk(src.getString("fixLnk"));
        item.setDefDev(src.getString("defDev"));
        item.setRemedTime(src.getString("remedTime"));
        item.setMethod(src.getInteger("srcMethod") != null ? src.getInteger("srcMethod") : src.getInteger("method"));
        item.setVulAddrType(src.getInteger("vulAddrType"));
        item.setVulTransProto(src.getString("vulTransProto"));
        item.setUnitType(src.getString("unitType"));
        item.setExtVulnRef(src.getString("extVulnRef"));
        item.setArchiveReason(src.getString("archiveReason"));
        if (src.getJSONObject("provincialFields") != null) {
            item.setProvincialFields(src.getJSONObject("provincialFields").getInnerMap());
        }
    }

    public static OpenVulnInstanceDO toPersistRow(OpenVulnInstanceDO target, JSONObject src) {
        if (target == null || src == null) {
            return target;
        }
        target.setSnapshotJson(src.toJSONString());
        target.setVulInfoStat(src.getInteger("vulInfoStat"));
        String disposalId = src.getString("vulnDisposalId");
        if (!StringUtils.hasText(disposalId)) {
            disposalId = firstOf(src.getString("vulInfoID"), src.getString("vulInfoId"));
        }
        target.setVulnDisposalId(disposalId);
        return target;
    }

    public static OpenVulnInstanceDO convertRow(OpenVulnInstanceDO row) {
        return ConvertHelper.convert(row, OpenVulnInstanceDO.class);
    }

    private static String firstOf(String... values) {
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

    /**
     * 组织漏洞编号：task-center 原始结果 CVE 在 cve 字段，orgVulId 可能为厂商漏洞 ID。
     */
    public static String resolveOrgVulId(JSONObject src) {
        if (src == null) {
            return null;
        }
        return firstOf(src.getString("cve"), src.getString("CVE"), src.getString("orgVulId"));
    }
}
