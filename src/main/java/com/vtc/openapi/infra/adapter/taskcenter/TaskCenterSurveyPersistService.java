package com.vtc.openapi.infra.adapter.taskcenter;

import com.vtc.openapi.domain.task.model.entity.OpenTaskDO;
import com.vtc.openapi.domain.task.model.entity.OpenTaskScanResultDO;
import com.vtc.openapi.domain.task.model.entity.OpenTaskSubDO;
import com.vtc.openapi.domain.task.repository.IOpenTaskScanResultRepository;
import com.vtc.openapi.domain.task.repository.IOpenTaskRepository;
import com.vtc.openapi.infra.feign.dto.taskcenter.TaskCenterSurveyBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;

/**
 * 子任务完成后从 VTC 拉取存活/端口结果并落库（对齐 TaskExport §5.6.5）。
 */
@Service
@ConditionalOnProperty(name = "open-api.engine.adapter-mode", havingValue = "task-center")
public class TaskCenterSurveyPersistService {

    private static final Logger log = LoggerFactory.getLogger(TaskCenterSurveyPersistService.class);

    private final IOpenTaskRepository openTaskRepository;
    private final IOpenTaskScanResultRepository scanResultRepository;
    private final TaskCenterSurveyFetchService surveyFetchService;
    private final TaskCenterExportRowBuilder exportRowBuilder;

    public TaskCenterSurveyPersistService(IOpenTaskRepository openTaskRepository,
                                          IOpenTaskScanResultRepository scanResultRepository,
                                          TaskCenterSurveyFetchService surveyFetchService,
                                          TaskCenterExportRowBuilder exportRowBuilder) {
        this.openTaskRepository = openTaskRepository;
        this.scanResultRepository = scanResultRepository;
        this.surveyFetchService = surveyFetchService;
        this.exportRowBuilder = exportRowBuilder;
    }

    @Transactional(rollbackFor = Exception.class)
    public void persistSubSurveyResults(OpenTaskSubDO sub) {
        if (sub == null || !StringUtils.hasText(sub.getSurveyId())) {
            return;
        }
        OpenTaskDO task = openTaskRepository.findByTaskId(sub.getTaskId());
        if (task == null) {
            return;
        }
        TaskCenterSurveyBundle bundle = surveyFetchService.fetchAll(sub.getSurveyId());
        List<String> taskHosts = TaskCenterExportRowBuilder.parseTaskHosts(task.getTargetsJson());
        List<OpenTaskScanResultDO> rows = exportRowBuilder.buildPersistRows(
                task, sub, bundle, taskHosts, new Date());
        if (rows.isEmpty()) {
            log.info("task-center survey persist skipped empty bundle taskId={} subId={} surveyId={}",
                    sub.getTaskId(), sub.getSubId(), sub.getSurveyId());
            return;
        }
        scanResultRepository.upsertBatch(rows);
        log.info("task-center survey persist ok taskId={} subId={} live+port rows={}",
                sub.getTaskId(), sub.getSubId(), rows.size());
    }
}
