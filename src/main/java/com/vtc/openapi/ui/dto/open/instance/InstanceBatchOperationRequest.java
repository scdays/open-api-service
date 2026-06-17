package com.vtc.openapi.ui.dto.open.instance;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import java.util.Map;
import lombok.Data;

/**
 * 实例批量操作请求（§5.3.2 / §5.4.2 / §5.5.2）。
 */
@Data
@ApiModel(description = "实例批量操作请求")
public class InstanceBatchOperationRequest {

    @ApiModelProperty(value = "操作人（批量验证时必填）")
    private String operator;
    @ApiModelProperty(value = "批量操作条目，最多 500 条", required = true)
    private List<BatchItem> items;

    @Data
    @ApiModel(description = "批量操作单条")
    public static class BatchItem {

        @ApiModelProperty(value = "实例 ID", required = true)
        private String vulInfoID;
        // --- verify（§5.3.2）---
        @ApiModelProperty(value = "漏洞类型 1/2")
        private Integer vulnType;
        @ApiModelProperty(value = "VALID / FALSE_POSITIVE")
        private String verifyResult;
        @ApiModelProperty(value = "verifyResult=VALID 时必填")
        private Integer srcMethod;
        // --- remediate（§5.4.2）---
        @ApiModelProperty(value = "处置目标状态：5 或 9")
        private Integer vulInfoStat;
        @ApiModelProperty(value = "修复方案说明")
        private String remedDesc;
        @ApiModelProperty(value = "补丁链接")
        private String fixLnk;
        @ApiModelProperty(value = "防护/阻断设备")
        private String defDev;
        @ApiModelProperty(value = "修复耗时")
        private String remedTime;
        @ApiModelProperty(value = "未修复原因")
        private Integer lvRsn;
        @ApiModelProperty(value = "备案说明")
        private String archiveReason;
        @ApiModelProperty(value = "备案审批人")
        private String approvedBy;
        @ApiModelProperty(value = "备案时间")
        private String recordAt;
        @ApiModelProperty(value = "省侧扩展 JSON")
        private Map<String, Object> provincialFields;
        @ApiModelProperty(value = "派单角色")
        private Integer srcTktRole;
        @ApiModelProperty(value = "处置角色")
        private Integer dstTktRole;
        @ApiModelProperty(value = "派单人部门")
        private String assignerDept;
        @ApiModelProperty(value = "派单人邮箱")
        private String assignerEmail;
        @ApiModelProperty(value = "派单人电话")
        private String assignerPhone;
        @ApiModelProperty(value = "处置人部门")
        private String handlerDept;
        @ApiModelProperty(value = "处置人邮箱")
        private String handlerEmail;
        @ApiModelProperty(value = "处置人电话")
        private String handlerPhone;
        // --- 共用 ---
        @ApiModelProperty(value = "状态变更时间")
        private String transferTime;
        @ApiModelProperty(value = "备注")
        private String remark;
    }
}
