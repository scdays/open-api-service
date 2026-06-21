package com.vtc.openapi.infra.adapter.taskcenter;

import com.vtc.openapi.domain.task.model.entity.OpenTaskSubDO;
import com.vtc.openapi.domain.task.repository.IOpenTaskSubRepository;
import com.vtc.openapi.infra.feign.dto.taskcenter.DownloadReportFinishKafkaEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;

/**
 * 消费 download_report_finish_topic：记录 VTC 报告 FTP 路径。
 */
@Service
@ConditionalOnProperty(name = "open-api.engine.adapter-mode", havingValue = "task-center")
public class TaskCenterReportRecycleService {

    private static final Logger log = LoggerFactory.getLogger(TaskCenterReportRecycleService.class);

    private final IOpenTaskSubRepository openTaskSubRepository;
    private final TaskCenterSubRecycleCoordinator subRecycleCoordinator;

    public TaskCenterReportRecycleService(IOpenTaskSubRepository openTaskSubRepository,
                                          TaskCenterSubRecycleCoordinator subRecycleCoordinator) {
        this.openTaskSubRepository = openTaskSubRepository;
        this.subRecycleCoordinator = subRecycleCoordinator;
    }

    @Transactional(rollbackFor = Exception.class)
    public void onReportDownloadFinish(DownloadReportFinishKafkaEvent event) {
        if (event == null || !StringUtils.hasText(event.getExtTaskId())) {
            return;
        }
        String openSubId = TaskCenterSocKeys.parseOpenSubId(event.getExtTaskId());
        if (!StringUtils.hasText(openSubId)) {
            log.debug("task-center kafka report skipped extTaskId={}", event.getExtTaskId());
            return;
        }
        OpenTaskSubDO sub = openTaskSubRepository.findBySubId(openSubId);
        if (sub == null) {
            log.warn("task-center kafka report: sub not found extTaskId={}", event.getExtTaskId());
            return;
        }
        if (StringUtils.hasText(event.getDownloadPath())
                && event.getDownloadPath().equals(sub.getReportDownloadPath())) {
            return;
        }
        sub.setReportDownloadPath(TaskCenterTaskOrchestrator.truncateError(event.getDownloadPath()));
        // 收到报告路径后置为待归档；若此前为失败/等待路径状态，重置为 PENDING
        if (!TaskCenterSubSupport.REPORT_ARCHIVED.equals(sub.getReportArchiveStatus())) {
            sub.setReportArchiveStatus(TaskCenterSubSupport.REPORT_PENDING);
            sub.setReportArchiveError(null);
        }
        sub.setUpdatedAt(new Date());
        openTaskSubRepository.updateSub(sub);
        log.info("task-center kafka report path saved subId={} path={}", sub.getSubId(), sub.getReportDownloadPath());
        subRecycleCoordinator.tryRecycleSub(sub.getSubId());
    }
}
