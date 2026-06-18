package com.vtc.openapi.infra.feign.dto.taskcenter;

import lombok.Data;

/**
 * vuln-task-center Kafka topic: task_finish_topic
 */
@Data
public class TaskFinishKafkaEvent {

    /** 下发 SOC 扫描时的 taskId（OPEN-{subId}） */
    private String extTaskId;
    /** vuln-task-center 计划 ID */
    private String taskId;
    /** vuln-task-center survey 实例 ID */
    private String surveyId;
}
