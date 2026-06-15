package com.vtc.openapi.infra.adapter;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.vtc.openapi.domain.task.model.entity.OpenTaskDO;
import com.vtc.openapi.domain.task.repository.IOpenTaskRepository;
import com.vtc.openapi.infra.adapter.dto.SvmpTaskCreateRequest;
import com.vtc.openapi.infra.adapter.dto.SvmpTaskCreateResult;
import com.vtc.openapi.infra.adapter.dto.SvmpTaskProgressResult;
import com.vtc.openapi.infra.adapter.mock.MockEngineBundle;
import com.vtc.openapi.infra.adapter.mock.MockEngineFixtureLoader;
import com.vtc.openapi.infra.adapter.mock.MockFixtureResolver;
import com.vtc.openapi.infra.config.OpenApiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * vul-pass 扫描引擎 Mock 实现：内存任务状态 + fixture 实例数据。
 * 重启后任务进度从 open_task 恢复。
 */
@Component
@ConditionalOnProperty(name = "open-api.engine.adapter-mode", havingValue = "mock")
public class SvmpEngineAdapterMockImpl implements SvmpEngineAdapter {

    private static final Logger log = LoggerFactory.getLogger(SvmpEngineAdapterMockImpl.class);

    private final OpenApiProperties properties;
    private final MockEngineFixtureLoader fixtureLoader;
    private final MockFixtureResolver fixtureResolver;
    private final IOpenTaskRepository openTaskRepository;
    private final AtomicLong idSeq = new AtomicLong(1);
    private final Map<String, MockTaskState> taskStates = new ConcurrentHashMap<>();

    public SvmpEngineAdapterMockImpl(OpenApiProperties properties,
                                       MockEngineFixtureLoader fixtureLoader,
                                       MockFixtureResolver fixtureResolver,
                                       IOpenTaskRepository openTaskRepository) {
        this.properties = properties;
        this.fixtureLoader = fixtureLoader;
        this.fixtureResolver = fixtureResolver;
        this.openTaskRepository = openTaskRepository;
        log.warn("Engine adapter mode: MOCK (data-dir={})",
                properties.getEngine().getMock().getDataDir());
    }

    @Override
    public SvmpTaskCreateResult createTask(SvmpTaskCreateRequest request) {
        String engineTaskId = "MOCK-ENG-" + idSeq.getAndIncrement();
        MockTaskState state = new MockTaskState();
        state.createdAtMs = System.currentTimeMillis();
        state.extTaskId = request.getOptions() != null
                ? String.valueOf(request.getOptions().get("extTaskId")) : null;
        state.taskName = request.getTaskName();
        state.scanTemplateId = request.getScanTemplateId();
        state.vulnType = request.getVulnType();
        if (request.getOptions() != null && request.getOptions().get("reportTemplateId") != null) {
            Object raw = request.getOptions().get("reportTemplateId");
            if (raw instanceof Number) {
                state.reportTemplateId = ((Number) raw).intValue();
            }
        }
        taskStates.put(engineTaskId, state);
        log.info("Mock createTask engineTaskId={} taskName={}", engineTaskId, request.getTaskName());
        SvmpTaskCreateResult result = new SvmpTaskCreateResult();
        result.setEngineTaskId(engineTaskId);
        return result;
    }

    @Override
    public SvmpTaskProgressResult getTaskProgress(String engineTaskId) {
        MockTaskState state = taskStates.get(engineTaskId);
        if (state == null) {
            return progressFromPersistedTask(engineTaskId);
        }
        return progressFromElapsed(state.createdAtMs);
    }

    private SvmpTaskProgressResult progressFromPersistedTask(String engineTaskId) {
        OpenTaskDO task = openTaskRepository.findByEngineTaskId(engineTaskId);
        SvmpTaskProgressResult progress = new SvmpTaskProgressResult();
        if (task == null) {
            progress.setStatus("FAILED");
            progress.setProgress(0);
            progress.setErrorMessage("mock engine task not found: " + engineTaskId);
            return progress;
        }
        if ("FINISHED".equals(task.getStatus())) {
            progress.setStatus("FINISHED");
            progress.setProgress(task.getProgress() != null ? task.getProgress() : 100);
            return progress;
        }
        if ("FAILED".equals(task.getStatus())) {
            progress.setStatus("FAILED");
            progress.setProgress(task.getProgress() != null ? task.getProgress() : 0);
            progress.setErrorMessage(task.getErrorMessage());
            return progress;
        }
        long createdMs = task.getCreatedAt() != null ? task.getCreatedAt().getTime() : System.currentTimeMillis();
        return progressFromElapsed(createdMs);
    }

    private SvmpTaskProgressResult progressFromElapsed(long createdAtMs) {
        SvmpTaskProgressResult progress = new SvmpTaskProgressResult();
        int delaySec = Math.max(0, properties.getEngine().getMock().getTaskFinishDelaySeconds());
        long elapsed = System.currentTimeMillis() - createdAtMs;
        if (elapsed < delaySec * 1000L) {
            progress.setStatus("RUNNING");
            progress.setProgress(50);
        } else {
            progress.setStatus("FINISHED");
            progress.setProgress(100);
        }
        return progress;
    }

    @Override
    public Object searchInstances(Object request) {
        JSONObject req = toJsonObject(request);
        MockEngineBundle bundle = fixtureResolver.resolve(
                req.getString("extTaskId"), req.getString("taskId"), req.getString("taskName"));
        if (bundle == null) {
            String engineTaskId = req.getString("engineTaskId");
            MockTaskState state = engineTaskId != null ? taskStates.get(engineTaskId) : null;
            if (state != null) {
                bundle = fixtureLoader.resolveBundle(
                        state.extTaskId, state.taskName, state.scanTemplateId, state.reportTemplateId, state.vulnType);
            }
        }
        JSONArray items = new JSONArray();
        if (bundle != null) {
            List<JSONObject> filtered = filterInstances(bundle.getInstances(), req);
            int page = Math.max(1, req.getIntValue("page"));
            int size = Math.min(1000, Math.max(1, req.getIntValue("size")));
            int from = (page - 1) * size;
            for (int i = from; i < Math.min(from + size, filtered.size()); i++) {
                items.add(filtered.get(i));
            }
        }
        JSONObject data = new JSONObject();
        data.put("items", items);
        data.put("total", items.size());
        data.put("page", req.getIntValue("page"));
        data.put("size", req.getIntValue("size"));
        return data;
    }

    @Override
    public Object getInstanceDetail(String vulnDisposalId) {
        if (!StringUtils.hasText(vulnDisposalId)) {
            return null;
        }
        for (MockEngineBundle bundle : fixtureLoader.listBundles()) {
            if (bundle.getInstances() == null) {
                continue;
            }
            for (JSONObject inst : bundle.getInstances()) {
                if (Objects.equals(vulnDisposalId, inst.getString("vulInfoID"))
                        || Objects.equals(vulnDisposalId, inst.getString("vulnDisposalId"))) {
                    return JSON.parseObject(inst.toJSONString());
                }
            }
        }
        return null;
    }

    @Override
    public Object disposeInstance(Object request) {
        return notImplementedYet("disposeInstance");
    }

    @Override
    public Object verifyInstance(Object request) {
        return notImplementedYet("verifyInstance");
    }

  /** 供 P1 Domain 联调：按 extTaskId/taskName 解析 fixture bundle */
    public MockEngineBundle resolveBundleForTask(String extTaskId, String taskName) {
        return fixtureResolver.resolve(extTaskId, null, taskName);
    }

    private static List<JSONObject> filterInstances(List<JSONObject> source, JSONObject req) {
        JSONArray statList = req.getJSONArray("vulInfoStatList");
        JSONArray levelList = req.getJSONArray("vulLevelList");
        List<JSONObject> out = new ArrayList<>();
        if (source == null) {
            return out;
        }
        for (JSONObject inst : source) {
            if (statList != null && !statList.isEmpty()) {
                if (!statList.contains(inst.getInteger("vulInfoStat"))) {
                    continue;
                }
            }
            if (levelList != null && !levelList.isEmpty()) {
                if (!levelList.contains(inst.getInteger("vulLevel"))) {
                    continue;
                }
            }
            out.add(JSON.parseObject(inst.toJSONString()));
        }
        return out;
    }

    private static JSONObject toJsonObject(Object request) {
        if (request instanceof JSONObject) {
            return (JSONObject) request;
        }
        if (request instanceof Map) {
            return new JSONObject((Map<String, Object>) request);
        }
        return JSON.parseObject(JSON.toJSONString(request));
    }

    private static JSONObject notImplementedYet(String op) {
        JSONObject body = new JSONObject();
        body.put("mock", true);
        body.put("message", op + " 尚未实现，P1 实例写操作请走 open_vuln_instance 网关或 VulnInstanceGateway Mock");
        return body;
    }

    private static class MockTaskState {
        long createdAtMs;
        String extTaskId;
        String taskName;
        Integer scanTemplateId;
        Integer reportTemplateId;
        Integer vulnType;
    }
}
