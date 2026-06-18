package com.vtc.openapi.ui.dto.admin;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("漏洞实例状态跃迁日志")
public class OpenVulnInstanceStateLogDto {

    @ApiModelProperty("日志 ID")
    private Long id;

    @ApiModelProperty("vulInfoID")
    private String vulInfoId;

    @ApiModelProperty("任务 ID")
    private String taskId;

    @ApiModelProperty("子任务/作业 ID")
    private String subId;

    @ApiModelProperty("扫描阶段 1=排查 2=验证")
    private Integer scanPhase;

    @ApiModelProperty("变更前状态")
    private Integer prevStat;

    @ApiModelProperty("变更后状态")
    private Integer vulInfoStat;

    @ApiModelProperty("变更原因")
    private String changeReason;

    @ApiModelProperty("验证合并策略")
    private String verifyMergeStrategy;

    @ApiModelProperty("扫描器命中数")
    private Integer scannerHitCount;

    @ApiModelProperty("识别时间戳")
    private String transferTime;

    @ApiModelProperty("运营案件 ID")
    private String caseId;

    @ApiModelProperty("记录时间")
    private String createdAt;
}
