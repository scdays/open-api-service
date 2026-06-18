package com.vtc.openapi.ui.dto.admin;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel("OperationCaseWorkspaceDto")
public class OperationCaseWorkspaceDto {

    @ApiModelProperty("案件摘要")
    private OperationCaseAdminDto caseSummary;

    @ApiModelProperty("时间线事件")
    private List<OperationCaseEventDto> timeline;

    @ApiModelProperty("关联 API 调用")
    private List<InvocationDTO> invocations;

    @ApiModelProperty("关联 Webhook 投递")
    private List<WebhookDeliveryLogDTO> webhooks;

    @ApiModelProperty("状态跃迁日志（open_vuln_instance_log）")
    private List<OpenVulnInstanceStateLogDto> stateLogs;

    @ApiModelProperty("按 caseType 多态的业务载荷")
    private Object payload;
}
