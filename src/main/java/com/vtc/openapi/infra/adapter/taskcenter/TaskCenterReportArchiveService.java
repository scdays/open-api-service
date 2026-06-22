package com.vtc.openapi.infra.adapter.taskcenter;

import com.vtc.openapi.domain.export.model.ExportDataType;
import com.vtc.openapi.domain.export.model.ExportStage;
import com.vtc.openapi.domain.export.model.OpenExportFileType;
import com.vtc.openapi.domain.export.model.entity.OpenExportDO;
import com.vtc.openapi.domain.export.model.entity.OpenExportFileDO;
import com.vtc.openapi.domain.export.repository.IOpenExportRepository;
import com.vtc.openapi.domain.task.model.entity.OpenTaskDO;
import com.vtc.openapi.domain.task.model.entity.OpenTaskSubDO;
import com.vtc.openapi.domain.task.repository.IOpenTaskRepository;
import com.vtc.openapi.domain.task.repository.IOpenTaskSubRepository;
import com.vtc.openapi.domain.webhook.model.ArtifactReadyEvent;
import com.vtc.openapi.domain.webhook.service.business.IWebhookPublishService;
import com.vtc.openapi.infra.config.OpenApiProperties;
import com.vtc.openapi.infra.export.ExportDownloadUrlBuilder;
import com.vtc.openapi.infra.export.ExportFileStorageAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

/**
 * 子任务扫描完成后：从 SFTP 下载原始报告并上传至开放平台文件服务，归档成功后下发 ARTIFACT_READY webhook。
 * <p>原始报告复用 open_export/open_export_file 存储，downloadUrl 走现有 /exports/{exportId}/download 接口。</p>
 */
@Service
@ConditionalOnProperty(name = "open-api.engine.adapter-mode", havingValue = "task-center")
public class TaskCenterReportArchiveService {

    private static final Logger log = LoggerFactory.getLogger(TaskCenterReportArchiveService.class);

    private static final String ARTIFACT_SOURCE_SCANNER_RAW = "SCANNER_RAW";
    private static final String EXPORT_STAGE_TASK_COMPLETED = "TASK_COMPLETED";
    private static final String EXPORT_STAGE_VERIFY_SCAN = "VERIFY_SCAN";
    private static final String EXPORT_STAGE_VERIFY_FIX_SCAN = "VERIFY_FIX_SCAN";
    private static final String STATUS_READY = "READY";

    private final IOpenTaskSubRepository openTaskSubRepository;
    private final TaskCenterSftpReportDownloader sftpDownloader;
    private final ExportFileStorageAdapter fileStorage;
    private final OpenApiProperties properties;
    private final IWebhookPublishService webhookPublishService;
    private final ExportDownloadUrlBuilder downloadUrlBuilder;
    private final IOpenTaskRepository openTaskRepository;
    private final IOpenExportRepository exportRepository;

    public TaskCenterReportArchiveService(IOpenTaskSubRepository openTaskSubRepository,
                                          TaskCenterSftpReportDownloader sftpDownloader,
                                          ExportFileStorageAdapter fileStorage,
                                          OpenApiProperties properties,
                                          IWebhookPublishService webhookPublishService,
                                          ExportDownloadUrlBuilder downloadUrlBuilder,
                                          IOpenTaskRepository openTaskRepository,
                                          IOpenExportRepository exportRepository) {
        this.openTaskSubRepository = openTaskSubRepository;
        this.sftpDownloader = sftpDownloader;
        this.fileStorage = fileStorage;
        this.properties = properties;
        this.webhookPublishService = webhookPublishService;
        this.downloadUrlBuilder = downloadUrlBuilder;
        this.openTaskRepository = openTaskRepository;
        this.exportRepository = exportRepository;
    }

    /**
     * @return true 表示可继续后续 VTC 结果拉取 / 编排；false 表示仍需等待 download_report_finish 或重试
     */
    public boolean ensureArchived(OpenTaskSubDO sub) {
        if (sub == null || !StringUtils.hasText(sub.getSubId())) {
            return false;
        }
        if (!properties.getTaskCenter().getReportArchive().isEnabled()) {
            return true;
        }
        if (TaskCenterSubSupport.REPORT_ARCHIVED.equals(sub.getReportArchiveStatus())) {
            return true;
        }
        if (!requiresRawReport(sub)) {
            return true;
        }
        if (!StringUtils.hasText(sub.getReportDownloadPath())) {
            markWaitingPath(sub);
            log.debug("task-center report archive waiting path subId={}", sub.getSubId());
            return false;
        }
        return archiveNow(sub);
    }

    /**
     * 手动重新归档：按现有 reportDownloadPath 重新下载并上传。
     * 要求 reportDownloadPath 已存在；若仍无路径，需等待 vuln-task-center 推送 download_report_finish。
     *
     * @return true=已尝试归档（无论成败）；false=无可归档路径或非 vuln 子任务
     */
    public boolean retryArchive(OpenTaskSubDO sub) {
        if (sub == null || !StringUtils.hasText(sub.getSubId())) {
            return false;
        }
        if (!requiresRawReport(sub)) {
            return false;
        }
        if (!StringUtils.hasText(sub.getReportDownloadPath())) {
            return false;
        }
        return archiveNow(sub);
    }

    private boolean archiveNow(OpenTaskSubDO sub) {
        try {
            TaskCenterReportPathSupport.ParsedReportPath parsed =
                    TaskCenterReportPathSupport.parse(sub.getReportDownloadPath());
            if (parsed == null) {
                markFailed(sub, "无效的报告路径: " + sub.getReportDownloadPath());
                log.warn("task-center report archive skipped invalid path subId={} path={}",
                        sub.getSubId(), sub.getReportDownloadPath());
                return false;
            }
            byte[] bytes = sftpDownloader.download(sub.getReportDownloadPath());
            String fileName = TaskCenterReportPathSupport.buildArchiveFileName(
                    sub.getSubId(), parsed.getFileName());
            String fileKey = fileStorage.upload(bytes, fileName);
            String fileFormat = TaskCenterReportPathSupport.inferFileFormat(fileName);
            String contentType = TaskCenterReportPathSupport.contentTypeForFormat(fileFormat);
            String bucket = fileStorage.getBucket();
            Date now = new Date();

            // 复用 open_export/open_export_file 存储原始报告：同 sub 的 RAW_SCAN_ARCHIVE 记录存在则 update-in-place
            OpenExportDO exportRow = exportRepository.findBySubAndStage(
                    sub.getPartnerId(), sub.getSubId(), ExportStage.RAW_SCAN_ARCHIVE);
            if (exportRow == null) {
                String exportId = "EXP-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
                String downloadUrl = downloadUrlBuilder.build(exportId, bucket, fileKey);
                exportRow = new OpenExportDO();
                exportRow.setExportId(exportId);
                exportRow.setPartnerId(sub.getPartnerId());
                exportRow.setTaskId(sub.getTaskId());
                exportRow.setExtTaskId(resolveExtTaskId(sub));
                exportRow.setFormat(fileFormat);
                exportRow.setExportStage(ExportStage.RAW_SCAN_ARCHIVE);
                exportRow.setDataType(ExportDataType.fromCenterTaskType(sub.getCenterTaskType()));
                exportRow.setStatus(STATUS_READY);
                exportRow.setDownloadUrl(downloadUrl);
                exportRow.setSubId(sub.getSubId());
                exportRow.setGeneratedAt(now);
                exportRow.setCreatedAt(now);
                exportRow.setUpdatedAt(now);
                exportRepository.saveExport(exportRow);

                OpenExportFileDO fileRow = new OpenExportFileDO();
                fileRow.setExportId(exportId);
                fileRow.setRealTaskId(sub.getTaskId());
                fileRow.setPartnerId(sub.getPartnerId());
                fileRow.setFilePosition(bucket);
                fileRow.setFileField(fileKey);
                fileRow.setFileMetadata(fileName);
                fileRow.setFileType(OpenExportFileType.fromFormat(fileFormat));
                fileRow.setCreateTime(now);
                exportRepository.saveExportFile(fileRow);
            } else {
                // update-in-place：复用原 exportId，downloadUrl 稳定不变，partner 旧链接仍有效
                String downloadUrl = downloadUrlBuilder.build(exportRow.getExportId(), bucket, fileKey);
                exportRow.setFormat(fileFormat);
                exportRow.setDataType(ExportDataType.fromCenterTaskType(sub.getCenterTaskType()));
                exportRow.setStatus(STATUS_READY);
                exportRow.setDownloadUrl(downloadUrl);
                exportRow.setUpdatedAt(now);
                exportRepository.updateExport(exportRow);

                OpenExportFileDO fileRow = exportRepository.findFileByExportId(exportRow.getExportId());
                if (fileRow == null) {
                    fileRow = new OpenExportFileDO();
                    fileRow.setExportId(exportRow.getExportId());
                    fileRow.setRealTaskId(sub.getTaskId());
                    fileRow.setPartnerId(sub.getPartnerId());
                    fileRow.setFilePosition(bucket);
                    fileRow.setFileField(fileKey);
                    fileRow.setFileMetadata(fileName);
                    fileRow.setFileType(OpenExportFileType.fromFormat(fileFormat));
                    fileRow.setCreateTime(now);
                    exportRepository.saveExportFile(fileRow);
                } else {
                    fileRow.setFilePosition(bucket);
                    fileRow.setFileField(fileKey);
                    fileRow.setFileMetadata(fileName);
                    fileRow.setFileType(OpenExportFileType.fromFormat(fileFormat));
                    fileRow.setUpdateTime(now);
                    exportRepository.updateExportFile(fileRow);
                }
            }

            // 子任务归档状态
            sub.setReportArchiveStatus(TaskCenterSubSupport.REPORT_ARCHIVED);
            sub.setReportArchiveError(null);
            sub.setUpdatedAt(now);
            openTaskSubRepository.updateSub(sub);
            log.info("task-center report archived subId={} exportId={} fileKey={} bytes={}",
                    sub.getSubId(), exportRow.getExportId(), fileKey, bytes.length);

            // best-effort 发布 ARTIFACT_READY：独立 try/catch，异常不影响已落库的 ARCHIVED 状态
            try {
                publishArtifactReadyForSub(sub, exportRow, fileName, fileFormat, contentType, bytes.length);
            } catch (Exception wh) {
                log.warn("task-center ARTIFACT_READY publish failed subId={}: {}",
                        sub.getSubId(), wh.getMessage());
            }
            return true;
        } catch (Exception ex) {
            markFailed(sub, ex.getMessage());
            log.warn("task-center report archive failed subId={} path={}: {}",
                    sub.getSubId(), sub.getReportDownloadPath(), ex.getMessage());
            return false;
        }
    }

    private void publishArtifactReadyForSub(OpenTaskSubDO sub, OpenExportDO exportRow,
                                             String fileName, String fileFormat, String contentType, int byteSize) {
        if (!properties.getWebhook().isEnabled()) {
            return;
        }
        String extTaskId = resolveExtTaskId(sub);

        ArtifactReadyEvent event = new ArtifactReadyEvent();
        event.setPartnerId(sub.getPartnerId());
        event.setArtifactId(buildArtifactId(sub.getSubId()));
        event.setTaskId(sub.getTaskId());
        event.setExtTaskId(extTaskId);
        event.setVerifyFixJobId(sub.getVerifyFixJobId());
        event.setExportId(exportRow.getExportId());
        event.setExportStage(resolveExportStage(sub.getScanPhase()));
        event.setArtifactSource(ARTIFACT_SOURCE_SCANNER_RAW);
        event.setFileName(fileName);
        event.setFileFormat(fileFormat);
        event.setContentType(contentType);
        event.setByteSize((long) byteSize);
        event.setDownloadUrl(exportRow.getDownloadUrl());
        webhookPublishService.publishArtifactReady(event);
    }

    private String resolveExtTaskId(OpenTaskSubDO sub) {
        OpenTaskDO task = openTaskRepository.findByTaskId(sub.getTaskId());
        return task != null ? task.getExtTaskId() : null;
    }

    private String buildArtifactId(String subId) {
        String safeSub = subId != null ? subId.replaceAll("[^A-Za-z0-9_-]", "_") : "sub";
        String stamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        return "ART-" + stamp + "-" + safeSub;
    }

    private String resolveExportStage(Integer scanPhase) {
        if (scanPhase == null) {
            return EXPORT_STAGE_TASK_COMPLETED;
        }
        if (scanPhase == TaskCenterSubSupport.PHASE_VERIFY) {
            return EXPORT_STAGE_VERIFY_SCAN;
        }
        if (scanPhase == TaskCenterSubSupport.PHASE_VERIFY_FIX) {
            return EXPORT_STAGE_VERIFY_FIX_SCAN;
        }
        return EXPORT_STAGE_TASK_COMPLETED;
    }

    private void markWaitingPath(OpenTaskSubDO sub) {
        if (!TaskCenterSubSupport.REPORT_WAITING_PATH.equals(sub.getReportArchiveStatus())) {
            sub.setReportArchiveStatus(TaskCenterSubSupport.REPORT_WAITING_PATH);
            sub.setReportArchiveError(null);
            sub.setUpdatedAt(new Date());
            openTaskSubRepository.updateSub(sub);
        }
    }

    private void markFailed(OpenTaskSubDO sub, String message) {
        sub.setReportArchiveStatus(TaskCenterSubSupport.REPORT_FAILED);
        sub.setReportArchiveError(TaskCenterTaskOrchestrator.truncateError(message));
        sub.setUpdatedAt(new Date());
        openTaskSubRepository.updateSub(sub);
    }

    static boolean requiresRawReport(OpenTaskSubDO sub) {
        if (sub == null) {
            return false;
        }
        String type = sub.getCenterTaskType();
        return !StringUtils.hasText(type) || "vuln".equalsIgnoreCase(type.trim());
    }
}
