package com.vtc.openapi.ui.dto.admin;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * Webhook 事件业务详情 DTO。
 * <p>
 * 用于前端推送记录 tab 调用 /internal/admin/webhook-event-details 获取业务侧事件详情，
 * 通过 event_id 关联 platform-admin 投递记录。
 *
 * @author asset-security
 */
@Data
@ApiModel("WebhookEventDetailDto")
public class WebhookEventDetailDto {

    @ApiModelProperty("Webhook 事件ID")
    private String eventId;

    @ApiModelProperty("事件类型")
    private String eventType;

    @ApiModelProperty("合作方ID")
    private String partnerId;

    // ========== EXPORT_READY 字段 ==========

    @ApiModelProperty("外发ID（EXPORT_READY）")
    private String exportId;

    @ApiModelProperty("外发格式（EXPORT_READY）")
    private String exportFormat;

    @ApiModelProperty("外发阶段（EXPORT_READY / ARTIFACT_READY）")
    private String exportStage;

    // ========== ARTIFACT_READY 字段 ==========

    @ApiModelProperty("产物ID（ARTIFACT_READY）")
    private String artifactId;

    @ApiModelProperty("产物格式（ARTIFACT_READY）")
    private String artifactFormat;

    // ========== 通用字段 ==========

    @ApiModelProperty("下载URL")
    private String downloadUrl;

    @ApiModelProperty("是否可下载（基于 partner_webhook_config.downloadable_stages）")
    private Boolean downloadable;

    @ApiModelProperty("简要描述")
    private String summary;
}