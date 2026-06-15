package com.vtc.openapi.domain.instance.model.result;

import lombok.Data;

/**
 * 实例单条查询结果（领域层内部通用）。
 */
@Data
public class InstanceItemResult {

    /** vul-pass 内部自增主键，写操作 PUT 时必填 */
    private Long id;
    private String vulInfoId;
    private String vulId;
    private Integer vulInfoStat;
    private Integer lvRsn;
    private String vulName;
    private Integer vulLevel;
    private String orgVulId;
    private String vulNetAddr;
    private Integer vulPort;
    private String vulSvc;
    private Integer isAccess;
    private String transferTime;
    private String vulnDisposalId;
    private Integer vulAddrType;
    private String assetId;
    private String assetName;
    private String vulInstCpe;
    private String vulInstVendor;
    private String vulInstClass;
    private String vulInstName;
    private String vulInstVer;
    private String remedDesc;
    private String fixLnk;
    private String remedTime;
    private Integer method;
    private String vulTransProto;
    private String unitType;
    private String extVulnRef;
}
