package com.vtc.openapi.infra.feign.dto.taskcenter;

import lombok.Data;

/**
 * vuln-task-center Kafka topic: download_report_finish_topic
 */
@Data
public class DownloadReportFinishKafkaEvent {

    /** 下发 SOC 扫描时的 taskId（OPEN-{subId}） */
    private String extTaskId;
    /** FTP 报告下载路径 */
    private String downloadPath;
}
