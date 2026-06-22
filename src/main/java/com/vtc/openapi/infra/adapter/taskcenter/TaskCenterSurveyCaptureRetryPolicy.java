package com.vtc.openapi.infra.adapter.taskcenter;

import com.vtc.openapi.domain.task.model.entity.OpenTaskSubDO;
import com.vtc.openapi.infra.config.OpenApiProperties;
import com.vtc.openapi.infra.feign.dto.taskcenter.TaskCenterSurveyBundle;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "open-api.engine.adapter-mode", havingValue = "task-center")
public class TaskCenterSurveyCaptureRetryPolicy {

    private final OpenApiProperties properties;

    public TaskCenterSurveyCaptureRetryPolicy(OpenApiProperties properties) {
        this.properties = properties;
    }

    public boolean isEnabled() {
        return properties.getTaskCenter().getSurveyCapture().isRetryEnabled();
    }

    public int getMaxAttempts() {
        return Math.max(1, properties.getTaskCenter().getSurveyCapture().getMaxAttempts());
    }

    public long getRetryIntervalMs() {
        return Math.max(500L, properties.getTaskCenter().getSurveyCapture().getRetryIntervalMs());
    }

    public long getMaxWaitMs() {
        return Math.max(5_000L, properties.getTaskCenter().getSurveyCapture().getMaxWaitMs());
    }

    public boolean exceededMaxWait(OpenTaskSubDO sub) {
        if (sub == null || sub.getUpdatedAt() == null) {
            return false;
        }
        long elapsed = System.currentTimeMillis() - sub.getUpdatedAt().getTime();
        return elapsed >= getMaxWaitMs();
    }

    public boolean shouldDeferEmpty(OpenTaskSubDO sub, TaskCenterSurveyBundle bundle) {
        if (!isEnabled()) {
            return false;
        }
        if (!TaskCenterSurveyBundleSupport.isLikelyVtcLag(bundle)) {
            return false;
        }
        return !exceededMaxWait(sub);
    }

    public void sleepBeforeRetry(int attemptIndex) {
        long interval = getRetryIntervalMs();
        try {
            Thread.sleep(interval);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
