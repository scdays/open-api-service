package com.vtc.openapi.infra.adapter.taskcenter;

import com.vtc.openapi.domain.instance.model.entity.OpenVerifyFixJobDO;
import com.vtc.openapi.domain.instance.model.entity.OpenVerifyFixJobItemDO;
import com.vtc.openapi.domain.instance.repository.IOpenVerifyFixJobRepository;
import com.vtc.openapi.domain.instance.service.business.IVerifyFixJobDomainService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;

/**
 * 修复核验受理落库后异步下发 VTC，失败不回滚、不阻断 Partner 响应。
 */
@Service
@ConditionalOnProperty(name = "open-api.engine.adapter-mode", havingValue = "task-center")
public class TaskCenterVerifyFixPostAcceptDispatcher {

    private static final Logger log = LoggerFactory.getLogger(TaskCenterVerifyFixPostAcceptDispatcher.class);

    private final IOpenVerifyFixJobRepository verifyFixJobRepository;
    private final TaskCenterVerifyFixOrchestrator orchestrator;

    public TaskCenterVerifyFixPostAcceptDispatcher(IOpenVerifyFixJobRepository verifyFixJobRepository,
                                                   TaskCenterVerifyFixOrchestrator orchestrator) {
        this.verifyFixJobRepository = verifyFixJobRepository;
        this.orchestrator = orchestrator;
    }

    public void scheduleRescanDispatch(String jobId) {
        if (!StringUtils.hasText(jobId)) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    dispatchRescanNow(jobId);
                }
            });
            return;
        }
        dispatchRescanNow(jobId);
    }

    public void dispatchRescanNow(String jobId) {
        try {
            OpenVerifyFixJobDO job = verifyFixJobRepository.findByJobId(jobId);
            if (job == null) {
                log.warn("verify-fix dispatch skipped, job not found: {}", jobId);
                return;
            }
            List<OpenVerifyFixJobItemDO> items = verifyFixJobRepository.listItemsByJobId(jobId);
            orchestrator.dispatchRescan(job, items);
        } catch (Exception ex) {
            log.error("verify-fix rescan dispatch unexpected error jobId={}", jobId, ex);
            markDispatchFailed(jobId, ex.getMessage());
        }
    }

    private void markDispatchFailed(String jobId, String message) {
        OpenVerifyFixJobDO job = verifyFixJobRepository.findByJobId(jobId);
        if (job == null) {
            return;
        }
        job.setStatus(IVerifyFixJobDomainService.STATUS_DISPATCH_FAILED);
        job.setErrorMessage(TaskCenterTaskOrchestrator.truncateError(message));
        job.setUpdatedAt(new Date());
        verifyFixJobRepository.updateJob(job);
    }
}
