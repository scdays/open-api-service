package com.vtc.openapi.infra.adapter.taskcenter;

import com.vtc.openapi.domain.instance.model.entity.OpenVerifyFixJobDO;
import com.vtc.openapi.domain.instance.repository.IOpenVerifyFixJobRepository;
import com.vtc.openapi.domain.instance.service.business.IVerifyFixJobDomainService;
import com.vtc.openapi.infra.feign.dto.taskcenter.TaskCenterSurveyBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.Set;

/**
 * 轮询修复核验 VTC 复扫进度，完成后指纹比对并触发 Webhook。
 */
@Service
@ConditionalOnProperty(name = "open-api.engine.adapter-mode", havingValue = "task-center")
public class TaskCenterVerifyFixProgressService {

    private static final Logger log = LoggerFactory.getLogger(TaskCenterVerifyFixProgressService.class);

    private final IOpenVerifyFixJobRepository verifyFixJobRepository;
    private final TaskCenterSurveyResolver surveyResolver;
    private final TaskCenterSurveyFetchService surveyFetchService;
    private final TaskCenterVulnFingerprintMapper fingerprintMapper;
    private final IVerifyFixJobDomainService verifyFixJobDomainService;
    private final TaskCenterVerifyFixOrchestrator verifyFixOrchestrator;

    public TaskCenterVerifyFixProgressService(IOpenVerifyFixJobRepository verifyFixJobRepository,
                                              TaskCenterSurveyResolver surveyResolver,
                                              TaskCenterSurveyFetchService surveyFetchService,
                                              TaskCenterVulnFingerprintMapper fingerprintMapper,
                                              IVerifyFixJobDomainService verifyFixJobDomainService,
                                              TaskCenterVerifyFixOrchestrator verifyFixOrchestrator) {
        this.verifyFixJobRepository = verifyFixJobRepository;
        this.surveyResolver = surveyResolver;
        this.surveyFetchService = surveyFetchService;
        this.fingerprintMapper = fingerprintMapper;
        this.verifyFixJobDomainService = verifyFixJobDomainService;
        this.verifyFixOrchestrator = verifyFixOrchestrator;
    }

    @Transactional(rollbackFor = Exception.class)
    public void pollActiveJobs() {
        try {
            verifyFixOrchestrator.retryDispatchFailed();
        } catch (Exception ex) {
            log.warn("verify-fix dispatch retry failed: {}", ex.getMessage());
        }
        for (OpenVerifyFixJobDO job : verifyFixJobRepository.listActiveVtcJobs(30)) {
            try {
                refreshJob(job);
            } catch (Exception ex) {
                log.warn("verify-fix poll failed jobId={}: {}", job.getJobId(), ex.getMessage());
            }
        }
    }

    /**
     * Kafka task_finish_topic：修复核验复扫完成。
     */
    @Transactional(rollbackFor = Exception.class)
    public void onKafkaTaskFinished(String centerSubId, String centerPlanId, String surveyId) {
        if (!StringUtils.hasText(centerSubId) || !StringUtils.hasText(surveyId)) {
            return;
        }
        OpenVerifyFixJobDO job = verifyFixJobRepository.findByCenterSubId(centerSubId);
        if (job == null) {
            log.warn("verify-fix kafka: job not found centerSubId={}", centerSubId);
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
        completeFromSurvey(job);
    }

    private void refreshJob(OpenVerifyFixJobDO job) {
        if (job == null || !StringUtils.hasText(job.getCenterPlanId())) {
            return;
        }
        if (IVerifyFixJobDomainService.STATUS_FINISHED.equals(job.getStatus())
                || IVerifyFixJobDomainService.STATUS_FAILED.equals(job.getStatus())) {
            return;
        }
        TaskCenterSurveyResolver.SurveyPollResult poll = surveyResolver.pollSurvey(job.getCenterPlanId());
        boolean changed = false;
        if (StringUtils.hasText(poll.getSurveyId()) && !poll.getSurveyId().equals(job.getSurveyId())) {
            job.setSurveyId(poll.getSurveyId());
            changed = true;
        }
        if (poll.getProgress() > 0 && (job.getProgress() == null || poll.getProgress() > job.getProgress())) {
            job.setProgress(poll.getProgress());
            changed = true;
        }
        if (poll.isFinished()) {
            if (StringUtils.hasText(poll.getSurveyId())) {
                job.setSurveyId(poll.getSurveyId());
            }
            completeFromSurvey(job);
            return;
        }
        if (IVerifyFixJobDomainService.STATUS_PENDING.equals(job.getStatus())) {
            job.setStatus(IVerifyFixJobDomainService.STATUS_RUNNING);
            changed = true;
        }
        if (changed) {
            job.setUpdatedAt(new Date());
            verifyFixJobRepository.updateJob(job);
        }
    }

    private void completeFromSurvey(OpenVerifyFixJobDO job) {
        String surveyId = job.getSurveyId();
        if (!StringUtils.hasText(surveyId)) {
            return;
        }
        TaskCenterSurveyBundle bundle = surveyFetchService.fetchAll(surveyId);
        Set<String> keys = fingerprintMapper.buildFingerprintKeys(bundle.getVulnScanResultList());
        job.setRescanImported(true);
        job.setProgress(100);
        job.setUpdatedAt(new Date());
        verifyFixJobRepository.updateJob(job);
        verifyFixJobDomainService.completeFromRescanCompare(job.getJobId(), keys);
        log.info("verify-fix vtc compare completed jobId={} surveyId={} vulnRows={}",
                job.getJobId(), surveyId, bundle.getVulnScanResultList().size());
    }
}
