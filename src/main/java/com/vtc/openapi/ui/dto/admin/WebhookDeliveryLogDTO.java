package com.vtc.openapi.ui.dto.admin;

import com.botany.spore.ddd.ui.dto.BaseDTO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("WebhookDeliveryLogDTO")
public class WebhookDeliveryLogDTO extends BaseDTO {

    @ApiModelProperty("日志 ID")
    private Long id;

    @ApiModelProperty("Partner ID")
    private String partnerId;

    @ApiModelProperty("事件类型")
    private String eventType;

    @ApiModelProperty("事件 ID（幂等键）")
    private String eventId;

    @ApiModelProperty("关联资源类型 TASK/INSTANCE/EXPORT")
    private String resourceType;

    @ApiModelProperty("关联资源 ID")
    private String resourceId;

    @ApiModelProperty("关联 taskId（EXPORT 等事件用于跳转调用治理）")
    private String relatedTaskId;

    @ApiModelProperty("外发 exportId（EXPORT_READY）")
    private String exportId;

    @ApiModelProperty("外发格式 xml/json（EXPORT_READY）")
    private String exportFormat;

    @ApiModelProperty("外发阶段（EXPORT_READY）")
    private String exportStage;

    @ApiModelProperty("Partner 侧 downloadUrl（Webhook payload）")
    private String partnerDownloadUrl;

    @ApiModelProperty("是否可通过平台管理 API 下载外发文件")
    private Boolean exportDownloadable;

    @ApiModelProperty("回调地址")
    private String callbackUrl;

    @ApiModelProperty("HTTP 状态码")
    private Integer httpStatus;

    @ApiModelProperty("重试次数")
    private Integer retryCount;

    @ApiModelProperty("投递状态")
    private String status;

    @ApiModelProperty("创建时间")
    private Date createdAt;

    @ApiModelProperty("下次重试时间")
    private Date nextRetryAt;
}
