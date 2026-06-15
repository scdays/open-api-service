package com.vtc.openapi.infra.adapter.mock;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.vtc.openapi.domain.partner.context.PartnerContext;
import com.vtc.openapi.domain.task.model.entity.OpenTaskDO;
import com.vtc.openapi.domain.task.model.entity.PartnerTaskMapDO;
import com.vtc.openapi.domain.task.repository.IOpenTaskRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Resolve mock fixture bundle from open task template ids (scanTemplateId / reportTemplateId / type).
 */
@Component
@ConditionalOnProperty(name = "open-api.engine.adapter-mode", havingValue = "mock")
public class MockFixtureResolver {

    private final MockEngineFixtureLoader fixtureLoader;
    private final IOpenTaskRepository openTaskRepository;

    public MockFixtureResolver(MockEngineFixtureLoader fixtureLoader, IOpenTaskRepository openTaskRepository) {
        this.fixtureLoader = fixtureLoader;
        this.openTaskRepository = openTaskRepository;
    }

    public MockEngineBundle resolve(String extTaskId, String taskId, String taskName) {
        OpenTaskDO task = loadTask(extTaskId, taskId);
        if (task != null) {
            return fixtureLoader.resolveBundle(
                    task.getExtTaskId(),
                    task.getTaskName(),
                    task.getScanTemplateId(),
                    parseReportTemplateId(task),
                    task.getVulnType());
        }
        return fixtureLoader.resolveBundle(extTaskId, taskName, null, null, null);
    }

    private OpenTaskDO loadTask(String extTaskId, String taskId) {
        if (StringUtils.hasText(taskId)) {
            OpenTaskDO byTaskId = openTaskRepository.findByTaskId(taskId);
            if (byTaskId != null) {
                return byTaskId;
            }
        }
        String partnerId = PartnerContext.getPartnerId();
        if (StringUtils.hasText(partnerId) && StringUtils.hasText(extTaskId)) {
            PartnerTaskMapDO map = openTaskRepository.findTaskMap(partnerId, extTaskId);
            if (map != null && StringUtils.hasText(map.getPlatformTaskId())) {
                return openTaskRepository.findByTaskId(map.getPlatformTaskId());
            }
        }
        return null;
    }

    private static Integer parseReportTemplateId(OpenTaskDO task) {
        if (task == null || !StringUtils.hasText(task.getOptionsJson())) {
            return null;
        }
        try {
            JSONObject options = JSON.parseObject(task.getOptionsJson());
            return options != null ? options.getInteger("reportTemplateId") : null;
        } catch (Exception ignored) {
            return null;
        }
    }
}
