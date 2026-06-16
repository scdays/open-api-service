package com.vtc.openapi.domain.export.service.business.impl;

import com.vtc.openapi.domain.export.model.ExportStage;
import com.vtc.openapi.domain.export.model.OpenExportFileType;
import com.vtc.openapi.domain.export.model.ReportTemplateCatalog;
import com.vtc.openapi.domain.export.model.entity.OpenExportDO;
import com.vtc.openapi.domain.export.model.entity.OpenExportFileDO;
import com.vtc.openapi.domain.export.repository.IOpenExportRepository;
import com.vtc.openapi.domain.export.service.MockTaskExportAssembler;
import com.vtc.openapi.domain.export.service.business.IExportAssemblyDomainService;
import com.vtc.openapi.domain.export.service.business.VerifyFixItem;
import com.vtc.openapi.domain.instance.model.entity.OpenVulnInstanceDO;
import com.vtc.openapi.domain.instance.repository.IOpenVulnInstanceRepository;
import com.vtc.openapi.domain.task.model.entity.OpenTaskDO;
import com.vtc.openapi.domain.task.repository.IOpenTaskRepository;
import com.vtc.openapi.domain.webhook.service.business.IWebhookPublishService;
import com.vtc.openapi.infra.config.OpenApiProperties;
import com.vtc.openapi.infra.export.ExportDownloadUrlBuilder;
import com.vtc.openapi.infra.export.ExportFileStorageAdapter;
import com.vtc.openapi.infra.export.TaskExportJsonSerializer;
import com.vtc.openapi.infra.export.TaskExportXmlSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ExportAssemblyDomainServiceImpl implements IExportAssemblyDomainService {

    private static final Logger log = LoggerFactory.getLogger(ExportAssemblyDomainServiceImpl.class);
    private static final String STATUS_READY = "READY";
    private static final String STATUS_FAILED = "FAILED";

    private final IOpenTaskRepository openTaskRepository;
    private final IOpenVulnInstanceRepository vulnInstanceRepository;
    private final IOpenExportRepository exportRepository;
    private final MockTaskExportAssembler assembler;
    private final TaskExportJsonSerializer jsonSerializer;
    private final TaskExportXmlSerializer xmlSerializer;
    private final ExportFileStorageAdapter fileStorage;
    private final ExportDownloadUrlBuilder downloadUrlBuilder;
    private final IWebhookPublishService webhookPublishService;
    private final OpenApiProperties properties;

    public ExportAssemblyDomainServiceImpl(IOpenTaskRepository openTaskRepository,
                                           IOpenVulnInstanceRepository vulnInstanceRepository,
                                           IOpenExportRepository exportRepository,
                                           MockTaskExportAssembler assembler,
                                           TaskExportJsonSerializer jsonSerializer,
                                           TaskExportXmlSerializer xmlSerializer,
                                           ExportFileStorageAdapter fileStorage,
                                           ExportDownloadUrlBuilder downloadUrlBuilder,
                                           IWebhookPublishService webhookPublishService,
                                           OpenApiProperties properties) {
        this.openTaskRepository = openTaskRepository;
        this.vulnInstanceRepository = vulnInstanceRepository;
        this.exportRepository = exportRepository;
        this.assembler = assembler;
        this.jsonSerializer = jsonSerializer;
        this.xmlSerializer = xmlSerializer;
        this.fileStorage = fileStorage;
        this.downloadUrlBuilder = downloadUrlBuilder;
        this.webhookPublishService = webhookPublishService;
        this.properties = properties;
    }

    @Override
    @Async
    public void assembleForTaskCompleted(OpenTaskDO task) {
        if (task == null || !properties.getExport().isEnabled()) {
            return;
        }
        assembleInternal(task, ExportStage.TASK_COMPLETED, null);
    }

    @Override
    @Async
    public void assembleForVerifyScan(String partnerId, String taskId) {
        if (!properties.getExport().isEnabled()) {
            return;
        }
        OpenTaskDO task = requireTask(partnerId, taskId);
        if (task == null) {
            return;
        }
        assembleInternal(task, ExportStage.VERIFY_SCAN, null);
    }

    @Override
    @Async
    public void assembleForVerifyFixScan(String partnerId, String taskId, String verifyFixJobId,
                                         List<VerifyFixItem> items) {
        if (!properties.getExport().isEnabled()) {
            return;
        }
        OpenTaskDO task = requireTask(partnerId, taskId);
        if (task == null) {
            return;
        }
        assembleInternal(task, ExportStage.VERIFY_FIX_SCAN, verifyFixJobId);
        if (!CollectionUtils.isEmpty(items)) {
            webhookPublishService.publishVerifyFixCompleted(partnerId, verifyFixJobId, null, items);
        }
    }

    private OpenTaskDO requireTask(String partnerId, String taskId) {
        OpenTaskDO task = openTaskRepository.findByTaskId(taskId);
        if (task == null || !partnerId.equals(task.getPartnerId())) {
            log.warn("export assembly skipped, task missing or partner mismatch: partnerId={} taskId={}", partnerId, taskId);
            return null;
        }
        return task;
    }

    private void assembleInternal(OpenTaskDO task, String exportStage, String verifyFixJobId) {
        try {
            List<OpenVulnInstanceDO> instances = vulnInstanceRepository.listByPartnerAndTask(
                    task.getPartnerId(), task.getTaskId(), task.getExtTaskId());
            Date generatedAt = new Date();
            Date expiresAt = addDays(generatedAt, properties.getExport().getTtlDays());

            for (String format : ReportTemplateCatalog.resolveFormats(task.getReportTemplateId())) {
                publishFormat(task, exportStage, verifyFixJobId, instances, format, generatedAt, expiresAt);
            }
        } catch (Exception ex) {
            log.error("export assembly failed: taskId={} stage={}", task.getTaskId(), exportStage, ex);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    protected void publishFormat(OpenTaskDO task, String exportStage, String verifyFixJobId,
                                 List<OpenVulnInstanceDO> instances, String format,
                                 Date generatedAt, Date expiresAt) {
        OpenExportDO existing = exportRepository.findByStageAndFormat(
                task.getPartnerId(), task.getTaskId(), exportStage, format);
        if (existing != null && STATUS_READY.equals(existing.getStatus())) {
            log.debug("export already ready, skip: taskId={} stage={} format={}", task.getTaskId(), exportStage, format);
            return;
        }

        String exportId = "EXP-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String fileName = "export-" + task.getTaskId() + "-" + exportId + "." + format;

        OpenExportDO row = new OpenExportDO();
        row.setExportId(exportId);
        row.setPartnerId(task.getPartnerId());
        row.setTaskId(task.getTaskId());
        row.setExtTaskId(task.getExtTaskId());
        row.setReportTemplateId(task.getReportTemplateId());
        row.setFormat(format);
        row.setExportStage(exportStage);
        row.setDataType(com.vtc.openapi.domain.export.model.ExportDataType.fromScanTemplateId(task.getScanTemplateId()));
        row.setStatus(STATUS_FAILED);
        row.setRecordCount(CollectionUtils.isEmpty(instances) ? 0 : instances.size());
        row.setGeneratedAt(generatedAt);
        row.setExpiresAt(expiresAt);
        row.setVerifyFixJobId(verifyFixJobId);
        row.setCreatedAt(generatedAt);
        row.setUpdatedAt(generatedAt);
        exportRepository.saveExport(row);

        try {
            Map<String, Object> document = assembler.assemble(
                    task, exportStage, format, instances, exportId, generatedAt, expiresAt);
            byte[] bytes = "json".equals(format)
                    ? jsonSerializer.serialize(document)
                    : xmlSerializer.serialize(document);

            String fileKey = fileStorage.upload(bytes, fileName);
            String bucket = fileStorage.getBucket();
            String downloadUrl = downloadUrlBuilder.build(exportId, bucket, fileKey);

            OpenExportDO persisted = exportRepository.findByExportId(exportId);
            if (persisted == null) {
                throw new IllegalStateException("export row not found: " + exportId);
            }
            persisted.setStatus(STATUS_READY);
            persisted.setDownloadUrl(downloadUrl);
            persisted.setUpdatedAt(new Date());
            exportRepository.updateExport(persisted);

            OpenExportFileDO fileRow = new OpenExportFileDO();
            fileRow.setExportId(exportId);
            fileRow.setRealTaskId(task.getTaskId());
            fileRow.setPartnerId(task.getPartnerId());
            fileRow.setFilePosition(bucket);
            fileRow.setFileField(fileKey);
            fileRow.setFileMetadata(fileName);
            fileRow.setFileType(OpenExportFileType.fromFormat(format));
            fileRow.setCreateTime(new Date());
            exportRepository.saveExportFile(fileRow);

            webhookPublishService.publishExportReady(task, persisted);
        } catch (Exception ex) {
            OpenExportDO failed = exportRepository.findByExportId(exportId);
            if (failed != null) {
                failed.setErrorMessage(truncate(ex.getMessage(), 1000));
                failed.setUpdatedAt(new Date());
                exportRepository.updateExport(failed);
            }
            log.warn("export file generation failed: taskId={} format={}: {}", task.getTaskId(), format, ex.getMessage());
        }
    }

    private static Date addDays(Date base, int days) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(base);
        cal.add(Calendar.DAY_OF_MONTH, days);
        return cal.getTime();
    }

    private static String truncate(String msg, int max) {
        if (!StringUtils.hasText(msg) || msg.length() <= max) {
            return msg;
        }
        return msg.substring(0, max);
    }
}
