package com.vtc.openapi.infra.adapter.taskcenter;

import com.vtc.openapi.domain.task.model.entity.OpenTaskSubDO;
import com.vtc.openapi.domain.task.repository.IOpenTaskSubRepository;
import com.vtc.openapi.infra.config.OpenApiProperties;
import com.vtc.openapi.infra.export.ExportFileStorageAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;

/**
 * 子任务扫描完成后：从 SFTP 下载原始报告并上传至开放平台文件服务。
 */
@Service
@ConditionalOnProperty(name = "open-api.engine.adapter-mode", havingValue = "task-center")
public class TaskCenterReportArchiveService {

    private static final Logger log = LoggerFactory.getLogger(TaskCenterReportArchiveService.class);

    private final IOpenTaskSubRepository openTaskSubRepository;
    private final TaskCenterSftpReportDownloader sftpDownloader;
    private final ExportFileStorageAdapter fileStorage;
    private final OpenApiProperties properties;

    public TaskCenterReportArchiveService(IOpenTaskSubRepository openTaskSubRepository,
                                          TaskCenterSftpReportDownloader sftpDownloader,
                                          ExportFileStorageAdapter fileStorage,
                                          OpenApiProperties properties) {
        this.openTaskSubRepository = openTaskSubRepository;
        this.sftpDownloader = sftpDownloader;
        this.fileStorage = fileStorage;
        this.properties = properties;
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
        if (StringUtils.hasText(sub.getReportFileField())) {
            return true;
        }
        if (!requiresRawReport(sub)) {
            return true;
        }
        if (!StringUtils.hasText(sub.getReportDownloadPath())) {
            log.debug("task-center report archive waiting path subId={}", sub.getSubId());
            return false;
        }
        return archiveNow(sub);
    }

    private boolean archiveNow(OpenTaskSubDO sub) {
        try {
            TaskCenterReportPathSupport.ParsedReportPath parsed =
                    TaskCenterReportPathSupport.parse(sub.getReportDownloadPath());
            if (parsed == null) {
                log.warn("task-center report archive skipped invalid path subId={} path={}",
                        sub.getSubId(), sub.getReportDownloadPath());
                return false;
            }
            byte[] bytes = sftpDownloader.download(sub.getReportDownloadPath());
            String fileName = TaskCenterReportPathSupport.buildArchiveFileName(
                    sub.getSubId(), parsed.getFileName());
            String fileKey = fileStorage.upload(bytes, fileName);
            sub.setReportFileField(fileKey);
            sub.setUpdatedAt(new Date());
            openTaskSubRepository.updateSub(sub);
            log.info("task-center report archived subId={} fileKey={} sftpPath={}",
                    sub.getSubId(), fileKey, sub.getReportDownloadPath());
            return true;
        } catch (Exception ex) {
            log.warn("task-center report archive failed subId={} path={}: {}",
                    sub.getSubId(), sub.getReportDownloadPath(), ex.getMessage());
            return false;
        }
    }

    static boolean requiresRawReport(OpenTaskSubDO sub) {
        if (sub == null) {
            return false;
        }
        String type = sub.getCenterTaskType();
        return !StringUtils.hasText(type) || "vuln".equalsIgnoreCase(type.trim());
    }
}
