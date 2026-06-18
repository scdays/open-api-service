package com.vtc.openapi.infra.adapter.taskcenter.kafka;

import com.alibaba.fastjson.JSON;
import com.vtc.redis.redis.RedisQueueHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * vuln-task-center Kafka 消息 Redis 缓冲队列（vuln-model 同款生产者/消费者解耦）。
 */
@Component
@ConditionalOnProperty(name = "open-api.engine.adapter-mode", havingValue = "task-center")
public class TaskCenterKafkaBufferQueue {

    private static final Logger log = LoggerFactory.getLogger(TaskCenterKafkaBufferQueue.class);
    static final String QUEUE_KEY = "open-api:task-center:kafka";

    private final RedisQueueHelper redisQueueHelper;

    public TaskCenterKafkaBufferQueue(RedisQueueHelper redisQueueHelper) {
        this.redisQueueHelper = redisQueueHelper;
    }

    public void offer(TaskCenterKafkaMessageType type, String payload) {
        if (type == null || !StringUtils.hasText(payload)) {
            return;
        }
        TaskCenterKafkaBufferedMessage message = TaskCenterKafkaBufferedMessage.of(type, payload);
        redisQueueHelper.push(QUEUE_KEY, JSON.toJSONString(message));
        log.debug("task-center kafka buffered type={}", type);
    }

    public List<TaskCenterKafkaBufferedMessage> pollBatch(int maxSize) {
        List<TaskCenterKafkaBufferedMessage> batch = new ArrayList<>();
        int cap = maxSize > 0 ? maxSize : 1;
        for (int i = 0; i < cap; i++) {
            String raw = redisQueueHelper.pull(QUEUE_KEY);
            if (!StringUtils.hasText(raw)) {
                break;
            }
            try {
                TaskCenterKafkaBufferedMessage message = JSON.parseObject(raw, TaskCenterKafkaBufferedMessage.class);
                if (message != null && message.getType() != null && StringUtils.hasText(message.getPayload())) {
                    batch.add(message);
                }
            } catch (Exception ex) {
                log.warn("task-center kafka buffer pop parse failed: {}", ex.getMessage());
            }
        }
        return batch;
    }

    public void requeue(TaskCenterKafkaBufferedMessage message) {
        if (message == null || message.getType() == null || !StringUtils.hasText(message.getPayload())) {
            return;
        }
        message.setRetryCount(message.getRetryCount() + 1);
        redisQueueHelper.push(QUEUE_KEY, JSON.toJSONString(message));
    }
}
