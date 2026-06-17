package com.vtc.openapi.ui.dto.open.instance;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.Map;
import lombok.Data;

/**
 * 处置实例请求（§5.4.1）。
 */
@Data
@ApiModel(description = "处置实例请求")
public class RemediateInstanceRequest {

    @ApiModelProperty(value = "处置目标状态：5=已修复，9=修复失败/备案；未传时由 lvRsn 推断")
    private Integer vulInfoStat;
    @ApiModelProperty(value = "处置方式", required = true)
    private Integer srcMethod;
    @ApiModelProperty(value = "→5 时必填：修复方案说明")
    private String remedDesc;
    @ApiModelProperty(value = "srcMethod=1050 时必填：补丁链接")
    private String fixLnk;
    @ApiModelProperty(value = "srcMethod=1051/1052 时必填：防护/阻断设备")
    private String defDev;
    @ApiModelProperty(value = "→5 时必填，如 3日、2周")
    private String remedTime;
    @ApiModelProperty(value = "→9 时必填：未修复原因")
    private Integer lvRsn;
    @ApiModelProperty(value = "→9 时必填：企业内部备案说明")
    private String archiveReason;
    @ApiModelProperty(value = "备案审批人")
    private String approvedBy;
    @ApiModelProperty(value = "备案时间")
    private String recordAt;
    @ApiModelProperty(value = "省侧扩展 JSON")
    private Map<String, Object> provincialFields;
    @ApiModelProperty(value = "派单角色", required = true)
    private Integer srcTktRole;
    @ApiModelProperty(value = "处置角色", required = true)
    private Integer dstTktRole;
    @ApiModelProperty(value = "派单人部门", required = true)
    private String assignerDept;
    @ApiModelProperty(value = "派单人邮箱；与 assignerPhone 至少填一项")
    private String assignerEmail;
    @ApiModelProperty(value = "派单人电话；与 assignerEmail 至少填一项")
    private String assignerPhone;
    @ApiModelProperty(value = "处置人部门", required = true)
    private String handlerDept;
    @ApiModelProperty(value = "处置人邮箱；与 handlerPhone 至少填一项")
    private String handlerEmail;
    @ApiModelProperty(value = "处置人电话；与 handlerEmail 至少填一项")
    private String handlerPhone;
    @ApiModelProperty(value = "状态变更时间（Unix 秒字符串）")
    private String transferTime;
    @ApiModelProperty(value = "备注")
    private String remark;
}
