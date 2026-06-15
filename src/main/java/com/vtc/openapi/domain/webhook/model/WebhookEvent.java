package com.vtc.openapi.domain.webhook.model;

import java.util.Map;

/**
 * Webhook 出站事件载荷。
 */
public class WebhookEvent {

    private String eventType;
    private String eventId;
    private String timestamp;
    private String partnerId;
    private Map<String, Object> data;

    public WebhookEvent() {
    }

    public WebhookEvent(String eventType, String eventId, String timestamp,
                        String partnerId, Map<String, Object> data) {
        this.eventType = eventType;
        this.eventId = eventId;
        this.timestamp = timestamp;
        this.partnerId = partnerId;
        this.data = data;
    }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    public String getPartnerId() { return partnerId; }
    public void setPartnerId(String partnerId) { this.partnerId = partnerId; }
    public Map<String, Object> getData() { return data; }
    public void setData(Map<String, Object> data) { this.data = data; }
}