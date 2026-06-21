package com.vtc.openapi.infra.adapter.taskcenter.kafka;

import com.vtc.openapi.infra.config.OpenApiProperties;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * vuln-task-center Kafka 入站：仅快速写入 Redis 缓冲队列，业务由 {@link TaskCenterKafkaConsumeScheduler} 异步处理。
 */
@Component
@ConditionalOnProperty(prefix = "open-api.task-center.kafka", name = "enabled", havingValue = "true")
public class TaskCenterKafkaListener {

    private static final Logger log = LoggerFactory.getLogger(TaskCenterKafkaListener.class);

    private final TaskCenterKafkaBufferQueue bufferQueue;
    private final OpenApiProperties properties;

    public TaskCenterKafkaListener(TaskCenterKafkaBufferQueue bufferQueue,
                                   OpenApiProperties properties) {
        this.bufferQueue = bufferQueue;
        this.properties = properties;
    }

    @KafkaListener(
            topics = "${open-api.task-center.kafka.topic-task-finish:task_finish_topic}",
            groupId = "${open-api.task-center.kafka.group-id:open-api-task-center}")
    public void onTaskFinish(ConsumerRecord<?, String> record, Acknowledgment ack) {
        enqueue(TaskCenterKafkaMessageType.TASK_FINISH, record, ack);
    }

    @KafkaListener(
            topics = "${open-api.task-center.kafka.topic-download-report-finish:download_report_finish_topic}",
            groupId = "${open-api.task-center.kafka.group-id:open-api-task-center}")
    public void onReportDownloadFinish(ConsumerRecord<?, String> record, Acknowledgment ack) {
        enqueue(TaskCenterKafkaMessageType.REPORT_DOWNLOAD, record, ack);
    }

    private void enqueue(TaskCenterKafkaMessageType type, ConsumerRecord<?, String> record, Acknowledgment ack) {
        if (!properties.getTaskCenter().getKafka().isEnabled()) {
            acknowledge(ack);
            return;
        }
        String payload = record != null ? record.value() : null;
        if (payload == null || payload.trim().isEmpty()) {
            acknowledge(ack);
            return;
        }
        try {
            bufferQueue.offer(type, payload);
        } catch (Exception ex) {
            log.error("task-center kafka enqueue failed type={} offset={}: {}",
                    type, record != null ? record.offset() : null, ex.getMessage(), ex);
            // 不 ack：抛出异常由 Spring Kafka 错误处理器重试，避免消息在 Redis 写入失败时丢失
            throw new RuntimeException("kafka buffer enqueue failed, will retry", ex);
        }
        acknowledge(ack);
    }

    private void acknowledge(Acknowledgment ack) {
        if (ack != null) {
            ack.acknowledge();
        }
    }
}
