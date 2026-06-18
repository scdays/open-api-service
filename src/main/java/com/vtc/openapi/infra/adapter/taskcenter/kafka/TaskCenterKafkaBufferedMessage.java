package com.vtc.openapi.infra.adapter.taskcenter.kafka;

import lombok.Data;

/**
 * Kafka 入队 Redis 后的缓冲消息（对齐 vuln-model：Stream 快速 ack，业务异步消费）。
 */
@Data
public class TaskCenterKafkaBufferedMessage {

    private TaskCenterKafkaMessageType type;
    private String payload;
    private long receivedAt;
    private int retryCount;

    public static TaskCenterKafkaBufferedMessage of(TaskCenterKafkaMessageType type, String payload) {
        TaskCenterKafkaBufferedMessage msg = new TaskCenterKafkaBufferedMessage();
        msg.setType(type);
        msg.setPayload(payload);
        msg.setReceivedAt(System.currentTimeMillis());
        msg.setRetryCount(0);
        return msg;
    }
}
