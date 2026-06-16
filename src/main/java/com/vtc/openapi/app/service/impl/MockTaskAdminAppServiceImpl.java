package com.vtc.openapi.app.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.vtc.openapi.app.service.IMockTaskAdminAppService;
import com.vtc.openapi.domain.instance.repository.IOpenVulnInstanceRepository;
import com.vtc.openapi.domain.instance.service.business.IInstanceIngestDomainService;
import com.vtc.openapi.domain.open.OpenApiConstants;
import com.vtc.openapi.domain.open.OpenApiException;
import com.vtc.openapi.domain.task.model.entity.OpenTaskDO;
import com.vtc.openapi.domain.task.model.support.TaskTypeSupport;
import com.vtc.openapi.domain.task.repository.IOpenTaskRepository;
import com.vtc.openapi.domain.task.service.MockTaskCompletionCoordinator;
import com.vtc.openapi.infra.adapter.mock.MockEngineFixtureLoader;
import com.vtc.openapi.infra.adapter.mock.MockReportImportRunner;
import com.vtc.openapi.infra.adapter.mock.MockTaskDataPathResolver;
import com.vtc.openapi.infra.config.OpenApiProperties;
import com.vtc.openapi.infra.converter.InstanceItemConverter;
import com.vtc.openapi.domain.instance.model.result.InstanceItemResult;
import com.vtc.openapi.ui.dto.ApiResponse;
import com.vtc.openapi.ui.dto.admin.MockBundleStatusDto;
import com.vtc.openapi.ui.dto.admin.MockDispatchPacketDto;
import com.vtc.openapi.ui.dto.admin.MockImportPreviewItemDto;
import com.vtc.openapi.ui.dto.admin.MockImportPreviewResultDto;
import com.vtc.openapi.ui.dto.admin.MockImportReportResultDto;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Service
@ConditionalOnProperty(name = "open-api.engine.adapter-mode", havingValue = "mock")
public class MockTaskAdminAppServiceImpl implements IMockTaskAdminAppService {

    private final IOpenTaskRepository openTaskRepository;
    private final IOpenVulnInstanceRepository vulnInstanceRepository;
    private final MockReportImportRunner reportImportRunner;
    private final MockEngineFixtureLoader fixtureLoader;
    private final MockTaskDataPathResolver pathResolver;
    private final OpenApiProperties properties;
    private final IInstanceIngestDomainService instanceIngestDomainService;
    private final MockTaskCompletionCoordinator taskCompletionCoordinator;

    public MockTaskAdminAppServiceImpl(IOpenTaskRepository openTaskRepository,
                                       IOpenVulnInstanceRepository vulnInstanceRepository,
                                       MockReportImportRunner reportImportRunner,
                                       MockEngineFixtureLoader fixtureLoader,
                                       MockTaskDataPathResolver pathResolver,
                                       OpenApiProperties properties,
                                       IInstanceIngestDomainService instanceIngestDomainService,
                                       MockTaskCompletionCoordinator taskCompletionCoordinator) {
        this.openTaskRepository = openTaskRepository;
        this.vulnInstanceRepository = vulnInstanceRepository;
        this.reportImportRunner = reportImportRunner;
        this.fixtureLoader = fixtureLoader;
        this.pathResolver = pathResolver;
        this.properties = properties;
        this.instanceIngestDomainService = instanceIngestDomainService;
        this.taskCompletionCoordinator = taskCompletionCoordinator;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<MockImportReportResultDto> importReport(String taskId, MultipartFile file, boolean force) {
        OpenTaskDO task = requireTask(taskId);
        byte[] xmlBytes = readXmlBytes(file);
        if (Boolean.TRUE.equals(task.getInstancesIngested()) && !force) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR,
                    "instances already ingested; pass force=true to re-import");
        }
        if (force && Boolean.TRUE.equals(task.getInstancesIngested())) {
            vulnInstanceRepository.deleteByPartnerAndTaskId(task.getPartnerId(), task.getTaskId());
            task.setInstancesIngested(false);
            task.setIngestError(null);
            task.setUpdatedAt(new Date());
            openTaskRepository.updateById(task);
        }

        int instanceCount;
        try {
            instanceCount = reportImportRunner.importXmlReport(task, xmlBytes);
        } catch (IOException ex) {
            throw new OpenApiException(OpenApiConstants.CODE_ENGINE_FAILED, "import XML failed: " + ex.getMessage());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new OpenApiException(OpenApiConstants.CODE_ENGINE_FAILED, "import XML interrupted");
        }

        fixtureLoader.reload();

        Date now = new Date();
        task.setStatus("FINISHED");
        task.setProgress(100);
        if (task.getStartedAt() == null) {
            task.setStartedAt(now);
        }
        task.setFinishedAt(now);
        task.setUpdatedAt(now);
        openTaskRepository.updateById(task);

        instanceIngestDomainService.tryIngestOnTaskFinished(task);
        // Always schedule after commit: covers ingest failure and manual FINISHED confirmation.
        taskCompletionCoordinator.scheduleNotify(task.getTaskId());
        return ApiResponse.ok(buildImportResult(task.getTaskId(), instanceCount));
    }

    @Override
    public ApiResponse<MockImportPreviewResultDto> previewReport(String taskId, MultipartFile file, int sampleSize) {
        OpenTaskDO task = requireTask(taskId);
        byte[] xmlBytes = readXmlBytes(file);
        MockReportImportRunner.MockXmlParseOutcome outcome;
        try {
            outcome = reportImportRunner.previewXmlReport(task, xmlBytes, sampleSize);
        } catch (IOException ex) {
            throw new OpenApiException(OpenApiConstants.CODE_ENGINE_FAILED, "preview XML failed: " + ex.getMessage());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new OpenApiException(OpenApiConstants.CODE_ENGINE_FAILED, "preview XML interrupted");
        }

        MockImportPreviewResultDto dto = new MockImportPreviewResultDto();
        dto.setTaskId(task.getTaskId());
        dto.setTotalCount(outcome.getTotalCount());
        dto.setSampleSize(outcome.getSamples().size());
        for (JSONObject inst : outcome.getSamples()) {
            InstanceItemResult item = InstanceItemConverter.fromJson(inst);
            MockImportPreviewItemDto row = new MockImportPreviewItemDto();
            row.setVulName(item.getVulName());
            row.setVulNetAddr(item.getVulNetAddr());
            row.setVulPort(item.getVulPort());
            row.setVulLevel(item.getVulLevel());
            row.setVulInfoStat(item.getVulInfoStat());
            row.setOrgVulId(item.getOrgVulId());
            dto.getSamples().add(row);
        }
        return ApiResponse.ok(dto);
    }

    @Override
    public ApiResponse<MockBundleStatusDto> getBundleStatus(String taskId) {
        OpenTaskDO task = requireTask(taskId);
        MockBundleStatusDto dto = new MockBundleStatusDto();
        dto.setTaskId(task.getTaskId());
        dto.setPartnerId(task.getPartnerId());
        dto.setStatus(task.getStatus());
        dto.setIngestMode(properties.getEngine().getMock().getIngestMode());
        dto.setInstancesIngested(task.getInstancesIngested());
        dto.setIngestError(task.getIngestError());
        dto.setPersistedInstanceCount(vulnInstanceRepository.countByPartnerAndTaskId(
                task.getPartnerId(), task.getTaskId()));
        try {
            MockReportImportRunner.BundleFileStatus files = reportImportRunner.readBundleFiles(task.getTaskId());
            dto.setHasSourceXml(files.isHasSourceXml());
            dto.setBundleInstanceCount(files.getBundleInstanceCount());
            dto.setBundleId(files.getBundleId());
            dto.setImportedAt(files.getImportedAt());
            dto.setTaskBundleDir(files.getTaskBundleDir());
        } catch (IOException ex) {
            dto.setTaskBundleDir(pathResolver.taskBundleDir(task.getTaskId()).toAbsolutePath().toString());
        }
        return ApiResponse.ok(dto);
    }

    @Override
    public ApiResponse<MockDispatchPacketDto> getDispatchPacket(String taskId) {
        OpenTaskDO task = requireTask(taskId);
        MockDispatchPacketDto dto = new MockDispatchPacketDto();
        dto.setTaskId(task.getTaskId());
        dto.setExtTaskId(task.getExtTaskId());
        dto.setEngineTaskId(task.getEngineTaskId());
        dto.setPartnerId(task.getPartnerId());
        dto.setTaskName(task.getTaskName());
        dto.setStatus(task.getStatus());
        dto.setScanTemplateId(task.getScanTemplateId());
        dto.setReportTemplateId(task.getReportTemplateId());
        dto.setVulnType(task.getVulnType());
        dto.setTargetsJson(task.getTargetsJson());
        dto.setOptionsJson(task.getOptionsJson());
        dto.setIngestMode(properties.getEngine().getMock().getIngestMode());
        dto.setTaskBundleDir(pathResolver.taskBundleDir(task.getTaskId()).toAbsolutePath().toString());
        dto.setTargets(parseTargets(task.getTargetsJson()));
        return ApiResponse.ok(dto);
    }

    private MockImportReportResultDto buildImportResult(String taskId, int instanceCount) {
        OpenTaskDO fresh = openTaskRepository.findByTaskId(taskId);
        MockImportReportResultDto dto = new MockImportReportResultDto();
        dto.setTaskId(taskId);
        dto.setBundleId("task-" + taskId);
        dto.setInstanceCount(instanceCount);
        dto.setStatus(fresh != null ? fresh.getStatus() : "FINISHED");
        dto.setInstancesIngested(fresh != null && Boolean.TRUE.equals(fresh.getInstancesIngested()));
        dto.setIngestError(fresh != null ? fresh.getIngestError() : null);
        return dto;
    }

    private OpenTaskDO requireTask(String taskId) {
        if (!StringUtils.hasText(taskId)) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "taskId is required");
        }
        OpenTaskDO task = openTaskRepository.findByTaskId(taskId.trim());
        if (task == null) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "task not found");
        }
        return task;
    }

    private static byte[] readXmlBytes(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "XML file is required");
        }
        try {
            return file.getBytes();
        } catch (IOException ex) {
            throw new OpenApiException(OpenApiConstants.CODE_ENGINE_FAILED, "read XML failed: " + ex.getMessage());
        }
    }

    private static List<String> parseTargets(String targetsJson) {
        if (!StringUtils.hasText(targetsJson)) {
            return Collections.emptyList();
        }
        try {
            JSONObject root = JSON.parseObject(targetsJson);
            if (root == null) {
                return Collections.emptyList();
            }
            String hosts = root.getString("hosts");
            if (!StringUtils.hasText(hosts)) {
                return Collections.emptyList();
            }
            return TaskTypeSupport.splitHosts(hosts);
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
    }
}
