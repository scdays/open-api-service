package com.vtc.openapi.domain.instance.service.business.impl;

import com.alibaba.fastjson.JSONObject;
import com.vtc.openapi.domain.instance.model.entity.OpenVulnInstanceDO;
import com.vtc.openapi.domain.instance.repository.IOpenVulnInstanceRepository;
import com.vtc.openapi.domain.instance.service.business.IInstanceIngestDomainService;
import com.vtc.openapi.domain.task.model.entity.OpenTaskDO;
import com.vtc.openapi.domain.task.repository.IOpenTaskRepository;
import com.vtc.openapi.infra.adapter.mock.MockEngineBundle;
import com.vtc.openapi.infra.adapter.mock.MockFixtureResolver;
import com.vtc.openapi.infra.config.OpenApiProperties;
import com.vtc.openapi.infra.converter.InstanceItemConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
@ConditionalOnProperty(name = "open-api.engine.adapter-mode", havingValue = "mock")
public class InstanceIngestDomainServiceImpl implements IInstanceIngestDomainService {

    private static final Logger log = LoggerFactory.getLogger(InstanceIngestDomainServiceImpl.class);

    private static final String INGEST_SUCCESS = "SUCCESS";
    private static final String INGEST_FAILED = "FAILED";
    private static final String INGEST_SKIPPED = "SKIPPED";

    private final MockFixtureResolver fixtureResolver;
    private final IOpenVulnInstanceRepository vulnInstanceRepository;
    private final IOpenTaskRepository openTaskRepository;
    private final OpenApiProperties properties;

    public InstanceIngestDomainServiceImpl(MockFixtureResolver fixtureResolver,
                                           IOpenVulnInstanceRepository vulnInstanceRepository,
                                           IOpenTaskRepository openTaskRepository,
                                           OpenApiProperties properties) {
        this.fixtureResolver = fixtureResolver;
        this.vulnInstanceRepository = vulnInstanceRepository;
        this.openTaskRepository = openTaskRepository;
        this.properties = properties;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void tryIngestOnTaskFinished(OpenTaskDO task) {
        if (task == null || !StringUtils.hasText(task.getTaskId())) {
            return;
        }
        if (!properties.getEngine().getMock().isAutoIngestInstancesOnFinish()) {
            markIngestSkipped(task, "auto-ingest disabled");
            return;
        }
        if (Boolean.TRUE.equals(task.getInstancesIngested())) {
            return;
        }
        if (vulnInstanceRepository.existsByPartnerAndTaskId(task.getPartnerId(), task.getTaskId())) {
            markIngested(task, INGEST_SKIPPED, null);
            return;
        }

        MockEngineBundle bundle = fixtureResolver.resolve(
                task.getExtTaskId(), task.getTaskId(), task.getTaskName());
        if (bundle == null || CollectionUtils.isEmpty(bundle.getInstances())) {
            markIngestFailed(task, "no matching mock bundle or empty instances");
            return;
        }

        Integer reportTemplateId = task.getReportTemplateId() != null
                ? task.getReportTemplateId() : parseReportTemplateId(task);
        Date now = new Date();
        List<OpenVulnInstanceDO> rows = new ArrayList<>();
        int seq = 0;
        for (JSONObject inst : bundle.getInstances()) {
            seq++;
            JSONObject snap = JSONObject.parseObject(inst.toJSONString());
            String platformVulInfoId = buildPlatformVulInfoId(task.getTaskId(), snap, seq);
            snap.put("vulInfoID", platformVulInfoId);
            snap.put("vulInfoId", platformVulInfoId);

            OpenVulnInstanceDO row = new OpenVulnInstanceDO();
            row.setPartnerId(task.getPartnerId());
            row.setTaskId(task.getTaskId());
            row.setExtTaskId(task.getExtTaskId());
            row.setEngineTaskId(task.getEngineTaskId());
            row.setScanTemplateId(task.getScanTemplateId());
            row.setReportTemplateId(reportTemplateId);
            row.setBundleId(bundle.getBundleId());
            row.setIngestStatus(INGEST_SUCCESS);
            row.setIngestAt(now);
            row.setVulInfoId(platformVulInfoId);
            row.setCreatedAt(now);
            row.setUpdatedAt(now);
            InstanceItemConverter.toPersistRow(row, snap);
            rows.add(row);
        }

        vulnInstanceRepository.batchInsert(rows);
        markIngested(task, INGEST_SUCCESS, null);
        log.info("Mock ingest finished: taskId={} bundle={} count={}",
                task.getTaskId(), bundle.getBundleId(), rows.size());
    }

    private void markIngestSkipped(OpenTaskDO task, String reason) {
        OpenTaskDO persisted = requirePersistedTask(task);
        if (persisted == null) {
            return;
        }
        persisted.setIngestError(reason);
        persisted.setUpdatedAt(new Date());
        openTaskRepository.updateById(persisted);
    }

    private void markIngestFailed(OpenTaskDO task, String error) {
        OpenTaskDO persisted = requirePersistedTask(task);
        if (persisted == null) {
            return;
        }
        persisted.setInstancesIngested(false);
        persisted.setIngestError(error);
        persisted.setUpdatedAt(new Date());
        openTaskRepository.updateById(persisted);
        log.warn("Mock ingest failed: taskId={} reason={}", task.getTaskId(), error);
    }

    private void markIngested(OpenTaskDO task, String status, String note) {
        OpenTaskDO persisted = requirePersistedTask(task);
        if (persisted == null) {
            return;
        }
        persisted.setInstancesIngested(true);
        persisted.setIngestError(note);
        persisted.setUpdatedAt(new Date());
        openTaskRepository.updateById(persisted);
    }

    private OpenTaskDO requirePersistedTask(OpenTaskDO task) {
        if (task == null || !StringUtils.hasText(task.getTaskId())) {
            return null;
        }
        if (task.getId() != null) {
            return task;
        }
        return openTaskRepository.findByTaskId(task.getTaskId());
    }

    private static String buildPlatformVulInfoId(String taskId, JSONObject snap, int seq) {
        String raw = InstanceItemConverter.fromJson(snap).getVulInfoId();
        if (!StringUtils.hasText(raw)) {
            raw = "VI-" + seq;
        }
        String candidate = taskId + "-" + raw;
        if (candidate.length() <= 64) {
            return candidate;
        }
        return taskId + "-VI-" + seq;
    }

    private static Integer parseReportTemplateId(OpenTaskDO task) {
        if (task == null || !StringUtils.hasText(task.getOptionsJson())) {
            return null;
        }
        try {
            JSONObject options = JSONObject.parseObject(task.getOptionsJson());
            return options != null ? options.getInteger("reportTemplateId") : null;
        } catch (Exception ignored) {
            return null;
        }
    }
}
