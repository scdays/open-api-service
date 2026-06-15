package com.vtc.openapi.domain.webhook.model;

import java.util.Map;

/**
 * Webhook 出站事件（内部），由 Listener 转为契约 JSON 后投递。
 */
public class WebhookEvent {

    private String eventType;
    private String eventId;
    private String occurredAt;
    private String partnerId;
    private Map<String, Object> payload;

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getOccurredAt() { return occurredAt; }
    public void setOccurredAt(String occurredAt) { this.occurredAt = occurredAt; }
    public String getPartnerId() { return partnerId; }
    public void setPartnerId(String partnerId) { this.partnerId = partnerId; }
    public Map<String, Object> getPayload() { return payload; }
    public void setPayload(Map<String, Object> payload) { this.payload = payload; }

    /** @deprecated 使用 {@link #getPayload()} */
    @Deprecated
    public Map<String, Object> getData() { return payload; }

    /** @deprecated 使用 {@link #setPayload(Map)} */
    @Deprecated
    public void setData(Map<String, Object> data) { this.payload = data; }

    /** @deprecated 使用 {@link #getOccurredAt()} */
    @Deprecated
    public String getTimestamp() { return occurredAt; }

    /** @deprecated 使用 {@link #setOccurredAt(String)} */
    @Deprecated
    public void setTimestamp(String timestamp) { this.occurredAt = timestamp; }
}
