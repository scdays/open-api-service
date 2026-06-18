package com.vtc.openapi.ui.dto.admin;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@ApiModel("OpenTaskWorkspaceDto")
public class OpenTaskWorkspaceDto {

    @ApiModelProperty("任务基础信息")
    private OpenTaskAdminDto task;

    @ApiModelProperty("扫描目标 hosts")
    private String targetHosts;

    @ApiModelProperty("排查阶段子任务")
    private List<OpenTaskSubDto> surveySubs;

    @ApiModelProperty("验证阶段子任务")
    private List<OpenTaskSubDto> verifySubs;

    @ApiModelProperty("实例统计 vulInfoStat -> count")
    private Map<String, Long> instanceStatCounts;

    @ApiModelProperty("实例预览（最多 50 条）")
    private List<OpenTaskInstanceBriefDto> instances;

    @ApiModelProperty("Webhook 投递摘要")
    private List<WebhookDeliveryLogDTO> webhookDeliveries;

    @ApiModelProperty("全链路时序事件")
    private List<OpenTaskTimelineEventDto> timeline;
}
