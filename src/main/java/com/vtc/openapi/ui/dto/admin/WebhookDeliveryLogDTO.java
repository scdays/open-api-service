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
