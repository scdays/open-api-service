package com.vtc.openapi.infra.dev;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

/**
 * In-memory webhook test inbox (cleared on process restart).
 */
@Component
public class WebhookTestInbox {

    private static final int MAX_SIZE = 200;

    private final Deque<WebhookTestReceipt> receipts = new ConcurrentLinkedDeque<>();

    public void add(WebhookTestReceipt receipt) {
        receipts.addFirst(receipt);
        while (receipts.size() > MAX_SIZE) {
            receipts.removeLast();
        }
    }

    public List<WebhookTestReceipt> list(String eventType, int limit) {
        int cap = Math.max(1, Math.min(limit, MAX_SIZE));
        return receipts.stream()
                .filter(r -> eventType == null || eventType.equals(r.getEventType()))
                .limit(cap)
                .collect(Collectors.toList());
    }

    public long count(String eventType) {
        if (eventType == null) {
            return receipts.size();
        }
        return receipts.stream().filter(r -> eventType.equals(r.getEventType())).count();
    }

    public void clear() {
        receipts.clear();
    }

    public static WebhookTestReceipt newReceipt(String rawBody, String signature, String timestamp) {
        WebhookTestReceipt receipt = new WebhookTestReceipt();
        receipt.setReceivedAt(Instant.now().toString());
        receipt.setRawBody(rawBody);
        receipt.setSignature(signature);
        receipt.setTimestamp(timestamp);
        return receipt;
    }

    public static class WebhookTestReceipt {
        private String receivedAt;
        private String eventId;
        private String eventType;
        private String partnerId;
        private String occurredAt;
        private Object body;
        private String rawBody;
        private String signature;
        private String timestamp;
        private Boolean signatureValid;

        public String getReceivedAt() {
            return receivedAt;
        }

        public void setReceivedAt(String receivedAt) {
            this.receivedAt = receivedAt;
        }

        public String getEventId() {
            return eventId;
        }

        public void setEventId(String eventId) {
            this.eventId = eventId;
        }

        public String getEventType() {
            return eventType;
        }

        public void setEventType(String eventType) {
            this.eventType = eventType;
        }

        public String getPartnerId() {
            return partnerId;
        }

        public void setPartnerId(String partnerId) {
            this.partnerId = partnerId;
        }

        public String getOccurredAt() {
            return occurredAt;
        }

        public void setOccurredAt(String occurredAt) {
            this.occurredAt = occurredAt;
        }

        public Object getBody() {
            return body;
        }

        public void setBody(Object body) {
            this.body = body;
        }

        public String getRawBody() {
            return rawBody;
        }

        public void setRawBody(String rawBody) {
            this.rawBody = rawBody;
        }

        public String getSignature() {
            return signature;
        }

        public void setSignature(String signature) {
            this.signature = signature;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(String timestamp) {
            this.timestamp = timestamp;
        }

        public Boolean getSignatureValid() {
            return signatureValid;
        }

        public void setSignatureValid(Boolean signatureValid) {
            this.signatureValid = signatureValid;
        }
    }
}
