package com.vtc.openapi.ui.dto.admin;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("InvocationDetailDTO")
public class InvocationDetailDTO extends InvocationDTO {

    @ApiModelProperty("调用方 IP")
    private String clientIp;

    @ApiModelProperty("响应 message（成功/失败摘要）")
    private String responseMessage;

    @ApiModelProperty("请求头摘要（文本）")
    private String requestHeadersPreview;

    @ApiModelProperty("请求体摘要（已废弃，请调用 request-body 接口按需获取）")
    @Deprecated
    private String requestBodyPreview;

    @ApiModelProperty("是否存在可二次加载的请求报文")
    private Boolean hasRequestBody;

    @ApiModelProperty("已持久化请求报文大小（字节近似，0 表示仅摘要）")
    private Long requestBodyByteSize;

    @ApiModelProperty("响应头摘要（文本）")
    private String responseHeadersPreview;

    @ApiModelProperty("是否存在可二次加载的响应报文")
    private Boolean hasResponseBody;

    @ApiModelProperty("已持久化响应报文大小（字节近似，0 表示仅摘要）")
    private Long responseBodyByteSize;

    @ApiModelProperty("响应体摘要（已废弃，请调用 response-body 接口按需获取）")
    @Deprecated
    private String responseBodyPreview;

    @ApiModelProperty("调用过程 Timeline")
    private List<InvocationTimelineItemDto> timeline;

    @ApiModelProperty("同 Partner 同资源关联的 Webhook 投递")
    private List<InvocationRelatedWebhookDto> relatedWebhooks;

    @Data
    @ApiModel("InvocationTimelineItemDto")
    public static class InvocationTimelineItemDto {
        private String occurredAt;
        private String message;
        private boolean failed;
    }

    @Data
    @ApiModel("InvocationRelatedWebhookDto")
    public static class InvocationRelatedWebhookDto {
        private Long id;
        private String eventId;
        private String eventType;
        private String resourceType;
        private String resourceId;
        private String status;
        private Integer httpStatus;
        private String createdAt;
    }
}
