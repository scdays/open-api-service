package com.vtc.openapi.domain.webhook.model;

import lombok.Data;

/**
 * ARTIFACT_READY 事件值对象（API §6.2/§6.6 扫描报告产物就绪）。
 */
@Data
public class ArtifactReadyEvent {

    private String partnerId;
    /** 产物记录 ID */
    private String artifactId;
    /** 平台任务 ID */
    private String taskId;
    /** Partner 任务键 */
    private String extTaskId;
    /** 同阶段关联的规范化外发 ID */
    private String exportId;
    /** TASK_COMPLETED / VERIFY_SCAN / VERIFY_FIX_SCAN */
    private String exportStage;
    /** SCANNER_RAW / PLATFORM_REPORT */
    private String artifactSource;
    /** 平台内部报告类型码 */
    private Integer reportTypeCode;
    /** 建议下载文件名 */
    private String fileName;
    /** xml / xlsx / pdf / zip */
    private String fileFormat;
    /** 下载 Content-Type */
    private String contentType;
    /** 文件大小（字节） */
    private Long byteSize;
    /** 预签名下载 URL */
    private String downloadUrl;
}
