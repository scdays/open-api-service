package com.vtc.openapi.infra.adapter.taskcenter;

import com.alibaba.fastjson.JSON;
import com.botany.spore.core.result.Result;
import com.vtc.openapi.domain.open.OpenApiConstants;
import com.vtc.openapi.domain.task.model.entity.OpenTaskDO;
import com.vtc.openapi.domain.task.model.entity.OpenTaskSubDO;
import com.vtc.openapi.domain.task.repository.IOpenTaskRepository;
import com.vtc.openapi.domain.task.repository.IOpenTaskSubRepository;
import com.vtc.openapi.infra.feign.IVulnTaskCenterScanClient;
import com.vtc.openapi.infra.feign.dto.taskcenter.SocOutsideScanRequest;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "open-api.engine.adapter-mode", havingValue = "task-center")
public class TaskCenterTaskOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(TaskCenterTaskOrchestrator.class);
    private static final int ERROR_MAX_LEN = 1000;

    private final IOpenTaskRepository openTaskRepository;
    private final IOpenTaskSubRepository openTaskSubRepository;
    private final IVulnTaskCenterScanClient scanClient;

    public TaskCenterTaskOrchestrator(IOpenTaskRepository openTaskRepository,
                                        IOpenTaskSubRepository openTaskSubRepository,
                                        IVulnTaskCenterScanClient scanClient) {
        this.openTaskRepository = openTaskRepository;
        this.openTaskSubRepository = openTaskSubRepository;
        this.scanClient = scanClient;
    }

    @Transactional(rollbackFor = Exception.class)
    public void dispatchSurveyPhase(OpenTaskDO task) {
        dispatchPhase(task, TaskCenterSubSupport.PHASE_SURVEY);
    }

    @Transactional(rollbackFor = Exception.class)
    public void dispatchVerifyPhase(OpenTaskDO task) {
        dispatchPhase(task, TaskCenterSubSupport.PHASE_VERIFY);
    }

    @Transactional(rollbackFor = Exception.class)
    public void retryDispatchFailed() {
        List<OpenTaskDO> tasks = openTaskRepository.listByStatus(
                OpenApiConstants.TASK_DISPATCH_FAILED, 30);
        for (OpenTaskDO task : tasks) {
            try {
                retryPhaseDispatch(task);
            } catch (Exception ex) {
                log.warn("retry dispatch failed taskId={}: {}", task.getTaskId(), ex.getMessage());
            }
        }
    }

    /**
     * 运营手动重试排查/验证阶段子任务下发（FAILED 且无 planId，或任务 DISPATCH_FAILED / 尚无子任务）。
     */
    @Transactional(rollbackFor = Exception.class)
    public TaskCenterDispatchRetryResult retryDispatchManual(String taskId, Integer scanPhase, String subId) {
        TaskCenterDispatchRetryResult result = new TaskCenterDispatchRetryResult();
        result.setTaskId(taskId);
        try {
            return doRetryDispatchManual(taskId, scanPhase, subId, result);
        } catch (Exception ex) {
            log.error("manual retry dispatch unexpected error taskId={} scanPhase={} subId={}",
                    taskId, scanPhase, subId, ex);
            result.setSuccess(false);
            result.setMessage(userRetryFailedMessage());
            return result;
        }
    }

    private TaskCenterDispatchRetryResult doRetryDispatchManual(String taskId, Integer scanPhase, String subId,
                                                                TaskCenterDispatchRetryResult result) {
        if (!StringUtils.hasText(taskId)) {
            result.setSuccess(false);
            result.setMessage("taskId 不能为空");
            return result;
        }
        OpenTaskDO task = openTaskRepository.findByTaskId(taskId);
        if (task == null) {
            result.setSuccess(false);
            result.setMessage("任务不存在");
            return result;
        }
        if ("FINISHED".equals(task.getStatus()) || "FAILED".equals(task.getStatus())) {
            result.setSuccess(false);
            result.setMessage("任务已终态，不可重试下发");
            result.setTaskStatus(task.getStatus());
            return result;
        }
        int phase = scanPhase != null ? scanPhase : TaskCenterSubSupport.PHASE_SURVEY;
        int retried = 0;
        if (StringUtils.hasText(subId)) {
            OpenTaskSubDO sub = openTaskSubRepository.findBySubId(subId.trim());
            if (sub == null || !taskId.equals(sub.getTaskId())) {
                result.setSuccess(false);
                result.setMessage("子任务不存在");
                return result;
            }
            if (!isRetryable(sub)) {
                result.setSuccess(false);
                result.setMessage("子任务不可重试（需 status=FAILED 且无 centerPlanId）");
                result.setTaskStatus(task.getStatus());
                return result;
            }
            String hosts = extractHosts(task);
            if (!StringUtils.hasText(hosts)) {
                markTaskDispatchFailed(task, "扫描目标为空");
                return fillRetryResult(result, task, phase, 1);
            }
            String phaseLabel = phase == TaskCenterSubSupport.PHASE_VERIFY ? "verify" : "survey";
            retried = 1;
            tryDispatchSub(task, sub, hosts, phaseLabel);
            finalizePhaseDispatch(task, phase);
        } else {
            List<OpenTaskSubDO> subs = openTaskSubRepository.listByTaskIdAndPhase(taskId, phase);
            if (CollectionUtils.isEmpty(subs)) {
                task.setTaskPhase(phase);
                task.setUpdatedAt(new Date());
                openTaskRepository.updateById(task);
                createAndDispatchSubs(task, phase);
            } else {
                for (OpenTaskSubDO sub : subs) {
                    if (isRetryable(sub)) {
                        retried++;
                    }
                }
                if (retried == 0
                        && !OpenApiConstants.TASK_DISPATCH_FAILED.equals(task.getStatus())
                        && !OpenApiConstants.TASK_ACCEPT_ACCEPTED.equals(task.getStatus())) {
                    result.setSuccess(false);
                    result.setMessage("无待重试子任务");
                    result.setTaskStatus(task.getStatus());
                    return result;
                }
                retryFailedSubs(task, phase, subs);
            }
        }
        if (retried == 0 && !StringUtils.hasText(subId)) {
            List<OpenTaskSubDO> created = openTaskSubRepository.listByTaskIdAndPhase(taskId, phase);
            retried = created != null ? created.size() : 0;
        }
        result.setRetriedCount(retried);
        return fillRetryResult(result, task, phase, retried);
    }

    private TaskCenterDispatchRetryResult fillRetryResult(TaskCenterDispatchRetryResult result,
                                                          OpenTaskDO task, int phase, int retried) {
        OpenTaskDO latest = openTaskRepository.findByTaskId(task.getTaskId());
        List<OpenTaskSubDO> subs = openTaskSubRepository.listByTaskIdAndPhase(task.getTaskId(), phase);
        int success = countSuccessful(subs);
        int failed = countRetryable(subs);
        boolean ok = success > 0;
        result.setSuccess(ok);
        result.setTaskStatus(latest != null ? latest.getStatus() : task.getStatus());
        result.setRetriedCount(retried);
        result.setSuccessCount(success);
        result.setFailedCount(failed);
        if (ok && failed == 0) {
            result.setMessage("重试下发成功");
        } else if (ok) {
            log.warn("manual retry dispatch partial success taskId={} phase={} success={} stillFailed={} detail={}",
                    task.getTaskId(), phase, success, failed, summarizeSubErrors(subs));
            result.setMessage("部分扫描器已恢复下发，仍有失败项，请稍后重试");
        } else {
            String detail = latest != null ? latest.getErrorMessage() : null;
            log.warn("manual retry dispatch failed taskId={} phase={} taskError={} subErrors={}",
                    task.getTaskId(), phase, detail, summarizeSubErrors(subs));
            result.setMessage(userRetryFailedMessage());
        }
        return result;
    }

    private static String summarizeSubErrors(List<OpenTaskSubDO> subs) {
        if (CollectionUtils.isEmpty(subs)) {
            return "-";
        }
        StringBuilder sb = new StringBuilder();
        for (OpenTaskSubDO sub : subs) {
            if (TaskCenterSubSupport.STATUS_FAILED.equals(sub.getStatus())
                    && !StringUtils.hasText(sub.getCenterPlanId())) {
                sb.append(sub.getSubId()).append('/').append(sub.getScannerType()).append(':')
                        .append(sub.getErrorMessage()).append(';');
            }
        }
        return sb.length() > 0 ? sb.toString() : "-";
    }

    private static String userRetryFailedMessage() {
        return "重试下发失败，扫描引擎暂不可用，请稍后重试或联系平台运维";
    }

    private void finalizePhaseDispatch(OpenTaskDO task, int scanPhase) {
        List<OpenTaskSubDO> subs = openTaskSubRepository.listByTaskIdAndPhase(task.getTaskId(), scanPhase);
        int success = countSuccessful(subs);
        int failed = countRetryable(subs);
        StringBuilder errors = new StringBuilder();
        for (OpenTaskSubDO sub : subs) {
            if (isRetryable(sub) && StringUtils.hasText(sub.getErrorMessage())) {
                errors.append(sub.getScannerType()).append(':').append(sub.getErrorMessage()).append(';');
            }
        }
        finalizeTaskAfterDispatch(task, success, failed, errors.toString());
    }

    private static int countRetryable(List<OpenTaskSubDO> subs) {
        if (CollectionUtils.isEmpty(subs)) {
            return 0;
        }
        int count = 0;
        for (OpenTaskSubDO sub : subs) {
            if (isRetryable(sub)) {
                count++;
            }
        }
        return count;
    }

    private void dispatchPhase(OpenTaskDO task, int scanPhase) {
        if (task == null || !StringUtils.hasText(task.getTaskId())) {
            return;
        }
        List<OpenTaskSubDO> existing = openTaskSubRepository.listByTaskIdAndPhase(
                task.getTaskId(), scanPhase);
        if (!existing.isEmpty()) {
            if (hasRetryableSubs(existing)) {
                retryFailedSubs(task, scanPhase, existing);
            }
            return;
        }
        task.setTaskPhase(scanPhase);
        task.setUpdatedAt(new Date());
        openTaskRepository.updateById(task);
        createAndDispatchSubs(task, scanPhase);
    }

    private void retryPhaseDispatch(OpenTaskDO task) {
        int phase = task.getTaskPhase() != null ? task.getTaskPhase() : TaskCenterSubSupport.PHASE_SURVEY;
        List<OpenTaskSubDO> subs = openTaskSubRepository.listByTaskIdAndPhase(task.getTaskId(), phase);
        if (CollectionUtils.isEmpty(subs)) {
            createAndDispatchSubs(task, phase);
            return;
        }
        retryFailedSubs(task, phase, subs);
    }

    private void retryFailedSubs(OpenTaskDO task, int scanPhase, List<OpenTaskSubDO> subs) {
        String hosts = extractHosts(task);
        if (!StringUtils.hasText(hosts)) {
            markTaskDispatchFailed(task, "扫描目标为空");
            return;
        }
        String phaseLabel = scanPhase == TaskCenterSubSupport.PHASE_VERIFY ? "verify" : "survey";
        int success = countSuccessful(subs);
        int failed = 0;
        StringBuilder errors = new StringBuilder();
        for (OpenTaskSubDO sub : subs) {
            if (!isRetryable(sub)) {
                if (TaskCenterSubSupport.STATUS_RUNNING.equals(sub.getStatus())
                        || TaskCenterSubSupport.STATUS_FINISHED.equals(sub.getStatus())) {
                    success++;
                }
                continue;
            }
            boolean ok = tryDispatchSub(task, sub, hosts, phaseLabel);
            if (ok) {
                success++;
            } else {
                failed++;
                if (StringUtils.hasText(sub.getErrorMessage())) {
                    errors.append(sub.getScannerType()).append(':').append(sub.getErrorMessage()).append(';');
                }
            }
        }
        finalizeTaskAfterDispatch(task, success, failed, errors.toString());
    }

    private void createAndDispatchSubs(OpenTaskDO task, int scanPhase) {
        String hosts = extractHosts(task);
        if (!StringUtils.hasText(hosts)) {
            markTaskDispatchFailed(task, "扫描目标为空");
            return;
        }
        String centerTaskType = TaskCenterTaskTypeMapper.resolveTaskType(
                task.getVulnType(), task.getScanTemplateId());
        List<String> scannerTypes = TaskCenterScannerPlanner.resolveScannerTypes(task.getScanTemplateId());
        String phaseLabel = scanPhase == TaskCenterSubSupport.PHASE_VERIFY ? "verify" : "survey";
        Date now = new Date();
        int success = 0;
        int failed = 0;
        StringBuilder errors = new StringBuilder();
        for (String scannerType : scannerTypes) {
            OpenTaskSubDO sub = new OpenTaskSubDO();
            sub.setSubId("SUB-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10));
            sub.setTaskId(task.getTaskId());
            sub.setPartnerId(task.getPartnerId());
            sub.setScanPhase(scanPhase);
            sub.setScannerType(scannerType);
            sub.setCenterTaskType(centerTaskType);
            sub.setStatus(TaskCenterSubSupport.STATUS_PENDING);
            sub.setProgress(0);
            sub.setCreatedAt(now);
            sub.setUpdatedAt(now);
            openTaskSubRepository.saveSub(sub);
            if (tryDispatchSub(task, sub, hosts, phaseLabel)) {
                success++;
            } else {
                failed++;
                if (StringUtils.hasText(sub.getErrorMessage())) {
                    errors.append(scannerType).append(':').append(sub.getErrorMessage()).append(';');
                }
            }
        }
        finalizeTaskAfterDispatch(task, success, failed, errors.toString());
    }

    private boolean tryDispatchSub(OpenTaskDO task, OpenTaskSubDO sub, String hosts, String phaseLabel) {
        SocOutsideScanRequest soc = new SocOutsideScanRequest();
        soc.setTaskId(TaskCenterSocKeys.socTaskId(sub.getSubId()));
        soc.setTaskName(task.getTaskName() + "_" + phaseLabel + "_s" + sub.getScannerType());
        soc.setInputIp(hosts);
        soc.setTaskType(sub.getCenterTaskType());
        soc.setScannerType(sub.getScannerType());
        try {
            Result<Map<String, Object>> scanResult = scanClient.createSocScan(soc);
            if (scanResult == null || !Boolean.TRUE.equals(scanResult.getSuccess())) {
                String msg = scanResult != null ? scanResult.getMessage() : "task-center soc scan failed";
                markSubFailed(sub, msg);
                return false;
            }
            String planId = extractPlanId(scanResult.getData());
            sub.setCenterPlanId(planId);
            sub.setStatus(TaskCenterSubSupport.STATUS_RUNNING);
            sub.setErrorMessage(null);
            sub.setUpdatedAt(new Date());
            openTaskSubRepository.updateSub(sub);
            log.info("task-center soc scan accepted taskId={} subId={} scannerType={} planId={}",
                    task.getTaskId(), sub.getSubId(), sub.getScannerType(), planId);
            return true;
        } catch (FeignException ex) {
            markSubFailed(sub, "vuln-task-center 调用失败: HTTP " + ex.status() + " " + ex.getMessage());
            log.warn("task-center soc scan feign failed taskId={} subId={} status={} msg={}",
                    task.getTaskId(), sub.getSubId(), ex.status(), ex.getMessage());
            return false;
        } catch (Exception ex) {
            markSubFailed(sub, "vuln-task-center 调用异常: " + ex.getMessage());
            log.warn("[任务中心] 内部调用扫描引擎异常! [Task ID: {}], [Sub Task ID: {}] [失败原因: {}]", sub.getSubId(), sub.getSubId(), ex.getMessage());
            return false;
        }
    }

    private void markSubFailed(OpenTaskSubDO sub, String message) {
        sub.setStatus(TaskCenterSubSupport.STATUS_FAILED);
        sub.setErrorMessage(truncateError(message));
        sub.setUpdatedAt(new Date());
        openTaskSubRepository.updateSub(sub);
        log.warn("task-center soc scan failed subId={} reason={}", sub.getSubId(), sub.getErrorMessage());
    }

    private void finalizeTaskAfterDispatch(OpenTaskDO task, int success, int failed, String errors) {
        Date now = new Date();
        task.setUpdatedAt(now);
        if (success <= 0) {
            markTaskDispatchFailed(task, StringUtils.hasText(errors) ? errors : "全部扫描器下发失败");
            return;
        }
        task.setStatus("RUNNING");
        if (task.getStartedAt() == null) {
            task.setStartedAt(now);
        }
        if (failed > 0) {
            task.setErrorMessage(truncateError("部分扫描器下发失败: " + errors));
        } else {
            task.setErrorMessage(null);
        }
        task.setProgress(0);
        openTaskRepository.updateById(task);
        if (failed == 0) {
            openTaskRepository.clearErrorMessage(task.getId());
        }
    }

    private void markTaskDispatchFailed(OpenTaskDO task, String message) {
        task.setStatus(OpenApiConstants.TASK_DISPATCH_FAILED);
        task.setErrorMessage(truncateError(message));
        task.setUpdatedAt(new Date());
        openTaskRepository.updateById(task);
        log.warn("task-center dispatch failed taskId={} reason={}", task.getTaskId(), task.getErrorMessage());
    }

    private static boolean hasRetryableSubs(List<OpenTaskSubDO> subs) {
        for (OpenTaskSubDO sub : subs) {
            if (isRetryable(sub)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRetryable(OpenTaskSubDO sub) {
        return TaskCenterSubSupport.STATUS_FAILED.equals(sub.getStatus())
                && !StringUtils.hasText(sub.getCenterPlanId());
    }

    private static int countSuccessful(List<OpenTaskSubDO> subs) {
        int count = 0;
        for (OpenTaskSubDO sub : subs) {
            if (TaskCenterSubSupport.STATUS_RUNNING.equals(sub.getStatus())
                    || TaskCenterSubSupport.STATUS_FINISHED.equals(sub.getStatus())) {
                count++;
            }
        }
        return count;
    }

    public static String truncateError(String message) {
        if (!StringUtils.hasText(message)) {
            return message;
        }
        String trimmed = message.trim();
        return trimmed.length() <= ERROR_MAX_LEN ? trimmed : trimmed.substring(0, ERROR_MAX_LEN);
    }

    private static String extractHosts(OpenTaskDO task) {
        if (!StringUtils.hasText(task.getTargetsJson())) {
            return null;
        }
        com.alibaba.fastjson.JSONObject json = JSON.parseObject(task.getTargetsJson());
        return json != null ? json.getString("hosts") : null;
    }

    private static String extractPlanId(Map<String, Object> data) {
        if (data == null) {
            return null;
        }
        Object id = data.get("id");
        return id != null ? id.toString() : null;
    }
}
