package com.vtc.openapi.ui.dto.admin;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("WebhookDeliveryLogDetailDTO")
public class WebhookDeliveryLogDetailDTO extends WebhookDeliveryLogDTO {

    @ApiModelProperty("?????? JSON")
    private String payloadJson;

    @ApiModelProperty("??????????????")
    private String payloadJsonFormatted;

    @ApiModelProperty("????????/??????")
    private List<WebhookDeliveryRetryHistoryDto> retryHistory;

    @ApiModelProperty("?????????? API ??????????")
    private List<RelatedInvocationDto> relatedInvocations;

    @Data
    @ApiModel("WebhookRelatedInvocationDto")
    public static class RelatedInvocationDto {
        private String invocationId;
        private String operationId;
        private String resourceType;
        private String resourceId;
        private Integer responseCode;
        private String startedAt;
    }

    @Data
    @ApiModel("WebhookDeliveryRetryHistoryDto")
    public static class WebhookDeliveryRetryHistoryDto {
        private Long id;
        private Integer retryCount;
        private Integer httpStatus;
        private String status;
        private String createdAt;
        private boolean manualRetry;
        private String triggerSource;
        private String triggerSourceLabel;
    }
}