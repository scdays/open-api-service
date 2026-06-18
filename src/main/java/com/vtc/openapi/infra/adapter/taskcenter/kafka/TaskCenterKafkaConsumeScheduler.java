package com.vtc.openapi.infra.adapter.taskcenter.kafka;

import com.alibaba.fastjson.JSON;
import com.vtc.openapi.infra.adapter.taskcenter.TaskCenterKafkaRecycleService;
import com.vtc.openapi.infra.adapter.taskcenter.TaskCenterReportRecycleService;
import com.vtc.openapi.infra.config.OpenApiProperties;
import com.vtc.openapi.infra.feign.dto.taskcenter.DownloadReportFinishKafkaEvent;
import com.vtc.openapi.infra.feign.dto.taskcenter.TaskFinishKafkaEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 从 Redis 拉取 Kafka 缓冲消息并执行业务回收（避免 Kafka 消费线程长阻塞）。
 */
@Component
@ConditionalOnProperty(name = "open-api.engine.adapter-mode", havingValue = "task-center")
public class TaskCenterKafkaConsumeScheduler {

    private static final Logger log = LoggerFactory.getLogger(TaskCenterKafkaConsumeScheduler.class);

    private final TaskCenterKafkaBufferQueue bufferQueue;
    private final TaskCenterKafkaRecycleService kafkaRecycleService;
    private final TaskCenterReportRecycleService reportRecycleService;
    private final OpenApiProperties properties;

    public TaskCenterKafkaConsumeScheduler(TaskCenterKafkaBufferQueue bufferQueue,
                                           TaskCenterKafkaRecycleService kafkaRecycleService,
                                           TaskCenterReportRecycleService reportRecycleService,
                                           OpenApiProperties properties) {
        this.bufferQueue = bufferQueue;
        this.kafkaRecycleService = kafkaRecycleService;
        this.reportRecycleService = reportRecycleService;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${open-api.task-center.kafka.queue-poll-interval-ms:2000}")
    public void consumeBufferedMessages() {
        if (!properties.getTaskCenter().getKafka().isEnabled()
                || !properties.getTaskCenter().getKafka().isQueueConsumeEnabled()) {
            return;
        }
        int batchSize = properties.getTaskCenter().getKafka().getQueueBatchSize();
        int maxRetry = properties.getTaskCenter().getKafka().getMaxRetry();
        List<TaskCenterKafkaBufferedMessage> batch = bufferQueue.pollBatch(batchSize);
        for (TaskCenterKafkaBufferedMessage message : batch) {
            try {
                dispatch(message);
            } catch (Exception ex) {
                log.warn("task-center kafka buffer consume failed type={} retry={}: {}",
                        message.getType(), message.getRetryCount(), ex.getMessage());
                if (message.getRetryCount() < maxRetry) {
                    bufferQueue.requeue(message);
                } else {
                    log.error("task-center kafka buffer dropped after retries type={} payload={}",
                            message.getType(), message.getPayload());
                }
            }
        }
    }

    private void dispatch(TaskCenterKafkaBufferedMessage message) {
        switch (message.getType()) {
            case TASK_FINISH:
                TaskFinishKafkaEvent finish = JSON.parseObject(message.getPayload(), TaskFinishKafkaEvent.class);
                if (finish == null || finish.getExtTaskId() == null) {
                    log.warn("task-center kafka buffer invalid task_finish: {}", message.getPayload());
                    return;
                }
                kafkaRecycleService.onTaskFinish(finish);
                return;
            case REPORT_DOWNLOAD:
                DownloadReportFinishKafkaEvent report = JSON.parseObject(
                        message.getPayload(), DownloadReportFinishKafkaEvent.class);
                if (report == null || report.getExtTaskId() == null) {
                    log.warn("task-center kafka buffer invalid download_report: {}", message.getPayload());
                    return;
                }
                reportRecycleService.onReportDownloadFinish(report);
                return;
            default:
                log.warn("task-center kafka buffer unknown type: {}", message.getType());
        }
    }
}
