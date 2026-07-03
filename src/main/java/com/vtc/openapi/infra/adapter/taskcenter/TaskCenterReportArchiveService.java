package com.vtc.openapi.infra.adapter.taskcenter;

import com.vtc.openapi.domain.artifact.model.ArtifactSource;
import com.vtc.openapi.domain.artifact.model.entity.OpenArtifactDO;
import com.vtc.openapi.domain.artifact.repository.IOpenArtifactRepository;
import com.vtc.openapi.domain.artifact.service.business.IArtifactWebhookCoordinator;
import com.vtc.openapi.domain.export.model.entity.OpenExportDO;
import com.vtc.openapi.domain.export.repository.IOpenExportRepository;
import com.vtc.openapi.domain.task.model.entity.OpenTaskDO;
import com.vtc.openapi.domain.task.model.entity.OpenTaskSubDO;
import com.vtc.openapi.domain.task.repository.IOpenTaskRepository;
import com.vtc.openapi.domain.task.repository.IOpenTaskSubRepository;
import com.vtc.openapi.infra.config.OpenApiProperties;
import com.vtc.openapi.infra.export.ExportDownloadUrlBuilder;
import com.vtc.openapi.infra.export.ExportFileStorageAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

/**
 * 子任务扫描完成后：从 SFTP 下载原始报告并上传至开放平台文件服务。
 * <p>ARTIFACT_READY 由 {@link com.vtc.openapi.domain.artifact.service.business.IArtifactWebhookCoordinator}
 * 在外发 EXPORT_READY 之后投递，保证 payload 含 exportId。</p>
 */
@Service
@ConditionalOnProperty(name = "open-api.engine.adapter-mode", havingValue = "task-center")
public class TaskCenterReportArchiveService {

    private static final Logger log = LoggerFactory.getLogger(TaskCenterReportArchiveService.class);

    private static final String EXPORT_STAGE_TASK_COMPLETED = "TASK_COMPLETED";
    private static final String EXPORT_STAGE_VERIFY_SCAN = "VERIFY_SCAN";
    private static final String EXPORT_STAGE_VERIFY_FIX_SCAN = "VERIFY_FIX_SCAN";
    private static final String STATUS_READY = "READY";

    private final IOpenTaskSubRepository openTaskSubRepository;
    private final TaskCenterSftpReportDownloader sftpDownloader;
    private final ExportFileStorageAdapter fileStorage;
    private final OpenApiProperties properties;
    private final ExportDownloadUrlBuilder downloadUrlBuilder;
    private final IOpenTaskRepository openTaskRepository;
    private final IOpenExportRepository exportRepository;
    private final IOpenArtifactRepository artifactRepository;
    private final IArtifactWebhookCoordinator artifactWebhookCoordinator;

    public TaskCenterReportArchiveService(IOpenTaskSubRepository openTaskSubRepository,
                                          TaskCenterSftpReportDownloader sftpDownloader,
                                          ExportFileStorageAdapter fileStorage,
                                          OpenApiProperties properties,
                                          ExportDownloadUrlBuilder downloadUrlBuilder,
                                          IOpenTaskRepository openTaskRepository,
                                          IOpenExportRepository exportRepository,
                                          IOpenArtifactRepository artifactRepository,
                                          IArtifactWebhookCoordinator artifactWebhookCoordinator) {
        this.openTaskSubRepository = openTaskSubRepository;
        this.sftpDownloader = sftpDownloader;
        this.fileStorage = fileStorage;
        this.properties = properties;
        this.downloadUrlBuilder = downloadUrlBuilder;
        this.openTaskRepository = openTaskRepository;
        this.exportRepository = exportRepository;
        this.artifactRepository = artifactRepository;
        this.artifactWebhookCoordinator = artifactWebhookCoordinator;
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

            OpenArtifactDO artifact = upsertRawArtifact(sub, fileName, fileFormat, contentType,
                    bucket, fileKey, bytes, now);

            // 子任务归档状态
            sub.setReportArchiveStatus(TaskCenterSubSupport.REPORT_ARCHIVED);
            sub.setReportArchiveError(null);
            sub.setUpdatedAt(now);
            openTaskSubRepository.updateSub(sub);
            log.info("task-center report archived subId={} artifactId={} fileKey={} bytes={}",
                    sub.getSubId(), artifact.getArtifactId(), fileKey, bytes.length);

            // 产物 Webhook：外发 EXPORT_READY 前先标记待发，由 ArtifactWebhookCoordinator 统一投递
            try {
                artifactWebhookCoordinator.onArtifactArchived(sub, artifact);
            } catch (Exception wh) {
                log.warn("task-center ARTIFACT_READY schedule failed subId={}: {}",
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

    private OpenArtifactDO upsertRawArtifact(OpenTaskSubDO sub, String fileName, String fileFormat,
                                             String contentType, String bucket, String fileKey,
                                             byte[] bytes, Date now) {
        String artifactId = buildArtifactId(sub.getSubId());
        String exportStage = resolveExportStage(sub.getScanPhase());
        String downloadUrl = downloadUrlBuilder.buildArtifact(artifactId, bucket, fileKey);
        String extTaskId = resolveExtTaskId(sub);
        OpenArtifactDO artifact = artifactRepository.findBySubTaskAndSource(
                sub.getPartnerId(), sub.getSubId(), ArtifactSource.SCANNER_RAW);
        boolean insert = artifact == null;
        if (insert) {
            artifact = new OpenArtifactDO();
            artifact.setArtifactId(artifactId);
            artifact.setPartnerId(sub.getPartnerId());
            artifact.setTaskId(sub.getTaskId());
            artifact.setSubTaskId(sub.getSubId());
            artifact.setArtifactSource(ArtifactSource.SCANNER_RAW);
            artifact.setCreatedAt(now);
        }
        if (artifact == null) {
            throw new IllegalStateException("产物记录初始化失败: " + sub.getSubId());
        }
        artifact.setExtTaskId(extTaskId);
        artifact.setExportId(resolveRelatedExportId(sub.getPartnerId(), sub.getTaskId(), exportStage));
        artifact.setExportStage(exportStage);
        artifact.setVerifyFixJobId(sub.getVerifyFixJobId());
        artifact.setScannerVendor(sub.getScannerType());
        artifact.setScannerProduct(sub.getScannerType());
        artifact.setFileName(fileName);
        artifact.setFileFormat(fileFormat);
        artifact.setContentType(contentType);
        artifact.setByteSize((long) bytes.length);
        artifact.setChecksum(sha256Hex(bytes));
        artifact.setStatus(STATUS_READY);
        artifact.setGeneratedAt(now);
        artifact.setDownloadUrl(downloadUrl);
        artifact.setErrorMessage(null);
        artifact.setFilePosition(bucket);
        artifact.setFileField(fileKey);
        artifact.setUpdatedAt(now);
        if (insert) {
            artifactRepository.saveArtifact(artifact);
            OpenArtifactDO saved = artifactRepository.findByArtifactId(artifactId);
            return saved != null ? saved : artifact;
        }
        artifactRepository.updateArtifact(artifact);
        return artifact;
    }

    private String resolveExtTaskId(OpenTaskSubDO sub) {
        OpenTaskDO task = openTaskRepository.findByTaskId(sub.getTaskId());
        return task != null ? task.getExtTaskId() : null;
    }

    private String buildArtifactId(String subId) {
        String safeSub = subId != null ? subId.replaceAll("[^A-Za-z0-9_-]", "_") : "sub";
        return "ART-" + safeSub;
    }

    private String resolveRelatedExportId(String partnerId, String taskId, String exportStage) {
        OpenExportDO export = exportRepository.findByStageAndFormat(partnerId, taskId, exportStage, "json");
        if (export == null) {
            export = exportRepository.findByStageAndFormat(partnerId, taskId, exportStage, "xml");
        }
        return export != null ? export.getExportId() : null;
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

    private static String sha256Hex(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes != null ? bytes : new byte[0]);
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            return null;
        }
    }

    static boolean requiresRawReport(OpenTaskSubDO sub) {
        if (sub == null) {
            return false;
        }
        String type = sub.getCenterTaskType();
        return !StringUtils.hasText(type)
                || "vuln".equalsIgnoreCase(type.trim())
                || "port".equalsIgnoreCase(type.trim());
    }
}
