package com.vtc.openapi.infra.adapter.taskcenter;

import com.vtc.openapi.infra.feign.IVulnTaskCenterScanClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

@Component
public class TaskCenterSurveyResolver {

    private static final Logger log = LoggerFactory.getLogger(TaskCenterSurveyResolver.class);

    private final IVulnTaskCenterScanClient scanClient;

    public TaskCenterSurveyResolver(IVulnTaskCenterScanClient scanClient) {
        this.scanClient = scanClient;
    }

    /**
     * 解析 centerPlanId 对应最新 surveyId；survey state=2 视为完成。
     */
    public SurveyPollResult pollSurvey(String centerPlanId) {
        SurveyPollResult result = new SurveyPollResult();
        if (!StringUtils.hasText(centerPlanId)) {
            return result;
        }
        String surveyId = resolveLatestSurveyId(centerPlanId);
        if (!StringUtils.hasText(surveyId)) {
            return result;
        }
        result.setSurveyId(surveyId);
        Map<String, Object> survey = scanClient.getSurveyById(surveyId);
        if (survey == null || survey.isEmpty()) {
            return result;
        }
        Object state = survey.get("state");
        Object endTime = survey.get("endTime");
        Object process = survey.get("process");
        if (process instanceof Number) {
            result.setProgress(((Number) process).intValue());
        } else if (process != null) {
            try {
                result.setProgress(Integer.parseInt(process.toString()));
            } catch (NumberFormatException ignored) {
                result.setProgress(0);
            }
        }
        boolean finished = TaskCenterSubSupport.SURVEY_STATE_FINISHED.equals(
                state != null ? state.toString() : null) || endTime != null;
        result.setFinished(finished);
        if (finished && result.getProgress() < 100) {
            result.setProgress(100);
        }
        return result;
    }

    private String resolveLatestSurveyId(String centerPlanId) {
        try {
            Map<String, Object> page = scanClient.surveyList(centerPlanId, 1, 1);
            String fromList = extractSurveyIdFromPage(page);
            if (StringUtils.hasText(fromList)) {
                return fromList;
            }
        } catch (Exception ex) {
            log.debug("surveyList failed planId={}: {}", centerPlanId, ex.getMessage());
        }
        Map<String, Object> plan = scanClient.getTaskById(centerPlanId);
        if (plan != null && plan.get("lastSurveyId") != null) {
            return plan.get("lastSurveyId").toString();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static String extractSurveyIdFromPage(Map<String, Object> page) {
        if (page == null) {
            return null;
        }
        Object records = page.get("records");
        if (!(records instanceof List) || CollectionUtils.isEmpty((List<?>) records)) {
            records = page.get("list");
        }
        if (!(records instanceof List) || CollectionUtils.isEmpty((List<?>) records)) {
            return null;
        }
        Object first = ((List<?>) records).get(0);
        if (first instanceof Map) {
            Object id = ((Map<?, ?>) first).get("id");
            return id != null ? id.toString() : null;
        }
        return null;
    }

    public static class SurveyPollResult {
        private String surveyId;
        private boolean finished;
        private int progress;

        public String getSurveyId() {
            return surveyId;
        }

        public void setSurveyId(String surveyId) {
            this.surveyId = surveyId;
        }

        public boolean isFinished() {
            return finished;
        }

        public void setFinished(boolean finished) {
            this.finished = finished;
        }

        public int getProgress() {
            return progress;
        }

        public void setProgress(int progress) {
            this.progress = progress;
        }
    }
}
