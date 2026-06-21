package com.vtc.openapi.infra.adapter.taskcenter;

import com.vtc.openapi.domain.open.OpenApiConstants;
import com.vtc.openapi.domain.open.OpenApiException;
import com.vtc.openapi.domain.instance.model.entity.OpenVerifyFixJobDO;
import com.vtc.openapi.domain.instance.repository.IOpenVerifyFixJobRepository;
import com.vtc.openapi.domain.instance.service.business.IVerifyFixJobDomainService;
import com.vtc.openapi.domain.task.model.entity.OpenTaskSubDO;
import com.vtc.openapi.domain.task.repository.IOpenTaskScanResultRepository;
import com.vtc.openapi.domain.task.repository.IOpenTaskSubRepository;
import com.vtc.openapi.infra.feign.dto.taskcenter.TaskCenterSurveyBundle;
import com.vtc.openapi.ui.dto.admin.OpenTaskSurveyRefetchResultDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 修复核验复扫进度：复用 open_task_sub(phase=3) + 与创建任务相同的 Kafka/轮询回收。
 */
@Service
@ConditionalOnProperty(name = "open-api.engine.adapter-mode", havingValue = "task-center")
public class TaskCenterVerifyFixProgressService {

    private static final Logger log = LoggerFactory.getLogger(TaskCenterVerifyFixProgressService.class);

    private final IOpenVerifyFixJobRepository verifyFixJobRepository;
    private final IOpenTaskSubRepository openTaskSubRepository;
    private final IOpenTaskScanResultRepository scanResultRepository;
    private final TaskCenterSurveyPersistService surveyPersistService;
    private final TaskCenterSurveyResolver surveyResolver;
    private final TaskCenterSurveyFetchService surveyFetchService;
    private final TaskCenterVulnFingerprintMapper fingerprintMapper;
    private final IVerifyFixJobDomainService verifyFixJobDomainService;
    private final TaskCenterVerifyFixOrchestrator verifyFixOrchestrator;
    private final TaskCenterReportArchiveService reportArchiveService;

    @Autowired
    @Lazy
    private TaskCenterVerifyFixProgressService self;

    public TaskCenterVerifyFixProgressService(IOpenVerifyFixJobRepository verifyFixJobRepository,
                                              IOpenTaskSubRepository openTaskSubRepository,
                                              IOpenTaskScanResultRepository scanResultRepository,
                                              TaskCenterSurveyPersistService surveyPersistService,
                                              TaskCenterSurveyResolver surveyResolver,
                                              TaskCenterSurveyFetchService surveyFetchService,
                                              TaskCenterVulnFingerprintMapper fingerprintMapper,
                                              IVerifyFixJobDomainService verifyFixJobDomainService,
                                              TaskCenterVerifyFixOrchestrator verifyFixOrchestrator,
                                              TaskCenterReportArchiveService reportArchiveService) {
        this.verifyFixJobRepository = verifyFixJobRepository;
        this.openTaskSubRepository = openTaskSubRepository;
        this.scanResultRepository = scanResultRepository;
        this.surveyPersistService = surveyPersistService;
        this.surveyResolver = surveyResolver;
        this.surveyFetchService = surveyFetchService;
        this.fingerprintMapper = fingerprintMapper;
        this.verifyFixJobDomainService = verifyFixJobDomainService;
        this.verifyFixOrchestrator = verifyFixOrchestrator;
        this.reportArchiveService = reportArchiveService;
    }

    @Transactional(rollbackFor = Exception.class)
    public OpenTaskSurveyRefetchResultDto refetchRescanSub(String jobId, String subId) {
        if (!StringUtils.hasText(jobId) || !StringUtils.hasText(subId)) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "jobId / subId 不能为空");
        }
        OpenVerifyFixJobDO job = verifyFixJobRepository.findByJobId(jobId.trim());
        if (job == null) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "修复核验任务不存在");
        }
        OpenTaskSubDO sub = openTaskSubRepository.findBySubId(subId.trim());
        if (sub == null || !jobId.trim().equals(sub.getVerifyFixJobId())
                || sub.getScanPhase() == null
                || sub.getScanPhase() != TaskCenterSubSupport.PHASE_VERIFY_FIX) {
            throw new OpenApiException(OpenApiConstants.CODE_PARAM_ERROR, "复扫子任务不存在或不属于该 job");
        }
        if (!StringUtils.hasText(sub.getSurveyId())) {
            throw new OpenApiException(OpenApiConstants.CODE_STATE_INVALID, "子任务尚无 surveyId，无法重新获取");
        }
        OpenTaskSurveyRefetchResultDto dto = new OpenTaskSurveyRefetchResultDto();
        dto.setTaskId(sub.getTaskId());
        dto.setSubId(sub.getSubId());
        int clearedScanRows = scanResultRepository.deleteBySubId(sub.getSubId());
        persistVerifyFixSubResults(sub);
        int persistedScanRows = scanResultRepository.listBySubId(sub.getSubId(), null).size();
        dto.setSuccess(true);
        dto.setClearedScanRows(clearedScanRows);
        dto.setPersistedScanRows(persistedScanRows);
        dto.setMessage("已重新获取复扫结果，落库 " + persistedScanRows + " 条");
        tryCompareWhenAllSubsReady(job.getJobId());
        return dto;
    }

    public void pollActiveJobs() {
        try {
            verifyFixOrchestrator.retryDispatchFailed();
        } catch (Exception ex) {
            log.warn("verify-fix dispatch retry failed: {}", ex.getMessage());
        }
        for (OpenTaskSubDO sub : openTaskSubRepository.listRunningVerifyFixSubs(50)) {
            try {
                // 通过 self 代理调用，使每个 sub 在独立事务中处理，避免单 sub 失败回滚整批
                self.refreshVerifyFixSub(sub);
            } catch (Exception ex) {
                log.warn("verify-fix poll failed subId={}: {}", sub.getSubId(), ex.getMessage());
            }
        }
    }

    /**
     * Kafka task_finish_topic：与创建任务相同入口，由 TaskCenterKafkaRecycleService 按 scan_phase 分流。
     */
    @Transactional(rollbackFor = Exception.class)
    public void onVerifyFixSubFinished(OpenTaskSubDO sub, String centerPlanId, String surveyId) {
        if (sub == null || !StringUtils.hasText(sub.getVerifyFixJobId())) {
            return;
        }
        if (!StringUtils.hasText(surveyId)) {
            return;
        }
        if (TaskCenterSubSupport.STATUS_FINISHED.equals(sub.getStatus())
                && StringUtils.hasText(sub.getSurveyId())
                && sub.getSurveyId().equals(surveyId)
                && sub.getProgress() != null && sub.getProgress() >= 100) {
            tryCompleteAfterReportArchived(sub);
            return;
        }
        if (StringUtils.hasText(centerPlanId) && !StringUtils.hasText(sub.getCenterPlanId())) {
            sub.setCenterPlanId(centerPlanId);
        }
        sub.setSurveyId(surveyId);
        sub.setStatus(TaskCenterSubSupport.STATUS_FINISHED);
        sub.setProgress(100);
        sub.setUpdatedAt(new Date());
        openTaskSubRepository.updateSub(sub);
        tryCompleteAfterReportArchived(sub);
    }

    /**
     * 兼容旧 VFS- 前缀 job.center_sub_id 回调。
     */
    @Transactional(rollbackFor = Exception.class)
    public void onLegacyKafkaTaskFinished(String legacySubId, String centerPlanId, String surveyId) {
        if (!StringUtils.hasText(legacySubId) || !StringUtils.hasText(surveyId)) {
            return;
        }
        OpenVerifyFixJobDO job = verifyFixJobRepository.findByCenterSubId(legacySubId);
        if (job == null) {
            log.warn("verify-fix legacy kafka: job not found centerSubId={}", legacySubId);
            return;
        }
        OpenTaskSubDO sub = openTaskSubRepository.findBySubId(legacySubId);
        if (sub != null && StringUtils.hasText(sub.getVerifyFixJobId())) {
            onVerifyFixSubFinished(sub, centerPlanId, surveyId);
            return;
        }
        if (IVerifyFixJobDomainService.STATUS_FINISHED.equals(job.getStatus())) {
            return;
        }
        if (StringUtils.hasText(centerPlanId) && !StringUtils.hasText(job.getCenterPlanId())) {
            job.setCenterPlanId(centerPlanId);
        }
        job.setSurveyId(surveyId);
        job.setStatus(IVerifyFixJobDomainService.STATUS_RUNNING);
        job.setUpdatedAt(new Date());
        verifyFixJobRepository.updateJob(job);
        completeFromSurveyId(job.getJobId(), surveyId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void refreshVerifyFixSub(OpenTaskSubDO sub) {
        if (sub == null || sub.getScanPhase() == null
                || sub.getScanPhase() != TaskCenterSubSupport.PHASE_VERIFY_FIX) {
            return;
        }
        if (TaskCenterSubSupport.STATUS_FINISHED.equals(sub.getStatus())
                || TaskCenterSubSupport.STATUS_FAILED.equals(sub.getStatus())) {
            return;
        }
        if (!StringUtils.hasText(sub.getCenterPlanId())) {
            return;
        }
        TaskCenterSurveyResolver.SurveyPollResult poll = surveyResolver.pollSurvey(sub.getCenterPlanId());
        boolean changed = false;
        if (StringUtils.hasText(poll.getSurveyId()) && !poll.getSurveyId().equals(sub.getSurveyId())) {
            sub.setSurveyId(poll.getSurveyId());
            changed = true;
        }
        if (poll.getProgress() > 0 && (sub.getProgress() == null || poll.getProgress() > sub.getProgress())) {
            sub.setProgress(poll.getProgress());
            changed = true;
        }
        if (poll.isFinished()) {
            if (StringUtils.hasText(poll.getSurveyId())) {
                sub.setSurveyId(poll.getSurveyId());
            }
            sub.setStatus(TaskCenterSubSupport.STATUS_FINISHED);
            sub.setProgress(100);
            changed = true;
            sub.setUpdatedAt(new Date());
            openTaskSubRepository.updateSub(sub);
            tryCompleteAfterReportArchived(sub);
            return;
        }
        if (TaskCenterSubSupport.STATUS_PENDING.equals(sub.getStatus())) {
            sub.setStatus(TaskCenterSubSupport.STATUS_RUNNING);
            changed = true;
        }
        if (changed) {
            sub.setUpdatedAt(new Date());
            openTaskSubRepository.updateSub(sub);
        }
    }

    /**
     * 子任务 FINISHED 后：先归档 SFTP 原始报告，再落库复扫结果并触发比对。
     */
    @Transactional(rollbackFor = Exception.class)
    public void tryCompleteAfterReportArchived(OpenTaskSubDO sub) {
        if (sub == null || !StringUtils.hasText(sub.getSurveyId()) || !StringUtils.hasText(sub.getVerifyFixJobId())) {
            return;
        }
        if (!TaskCenterSubSupport.STATUS_FINISHED.equals(sub.getStatus())) {
            return;
        }
        if (!reportArchiveService.ensureArchived(sub)) {
            return;
        }
        OpenTaskSubDO latest = openTaskSubRepository.findBySubId(sub.getSubId());
        if (latest == null) {
            return;
        }
        completeFromSub(latest);
    }

    private void completeFromSub(OpenTaskSubDO sub) {
        if (!StringUtils.hasText(sub.getSurveyId()) || !StringUtils.hasText(sub.getVerifyFixJobId())) {
            return;
        }
        persistVerifyFixSubResults(sub);
        tryCompareWhenAllSubsReady(sub.getVerifyFixJobId());
    }

    private void persistVerifyFixSubResults(OpenTaskSubDO sub) {
        if (sub == null || !StringUtils.hasText(sub.getSurveyId()) || !StringUtils.hasText(sub.getSubId())) {
            return;
        }
        try {
            scanResultRepository.deleteBySubId(sub.getSubId());
            surveyPersistService.persistSubSurveyResults(sub);
        } catch (Exception ex) {
            log.warn("verify-fix survey persist failed subId={}: {}", sub.getSubId(), ex.getMessage());
        }
    }

    private void completeFromSurveyId(String jobId, String surveyId) {
        tryCompareWhenAllSubsReady(jobId);
    }

    private void tryCompareWhenAllSubsReady(String jobId) {
        if (!StringUtils.hasText(jobId)) {
            return;
        }
        OpenVerifyFixJobDO job = verifyFixJobRepository.findByJobId(jobId);
        if (job == null || IVerifyFixJobDomainService.STATUS_FINISHED.equals(job.getStatus())
                || IVerifyFixJobDomainService.STATUS_FAILED.equals(job.getStatus())) {
            return;
        }
        List<OpenTaskSubDO> subs = openTaskSubRepository.listByVerifyFixJobId(jobId);
        if (subs.isEmpty()) {
            return;
        }
        boolean allFinished = true;
        for (OpenTaskSubDO sub : subs) {
            if (!TaskCenterSubSupport.STATUS_FINISHED.equals(sub.getStatus())
                    || !StringUtils.hasText(sub.getSurveyId())) {
                allFinished = false;
                break;
            }
        }
        if (!allFinished) {
            log.debug("verify-fix waiting sibling subs jobId={} subs={}", jobId, subs.size());
            return;
        }
        Set<String> keys = new HashSet<>();
        for (OpenTaskSubDO sub : subs) {
            TaskCenterSurveyBundle bundle = surveyFetchService.fetchAll(sub.getSurveyId());
            if (bundle != null && bundle.getVulnScanResultList() != null) {
                keys.addAll(fingerprintMapper.buildFingerprintKeys(bundle.getVulnScanResultList()));
            }
        }
        verifyFixJobDomainService.completeFromRescanCompare(jobId, keys);
        log.info("verify-fix compare done jobId={} subs={} unionKeys={}", jobId, subs.size(), keys.size());
    }
}
