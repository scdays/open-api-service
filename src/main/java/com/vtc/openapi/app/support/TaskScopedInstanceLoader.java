package com.vtc.openapi.app.support;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.vtc.openapi.domain.instance.model.entity.OpenVulnInstanceDO;
import com.vtc.openapi.domain.instance.model.entity.OpenVulnInstanceLogDO;
import com.vtc.openapi.domain.instance.model.entity.OpenVerifyFixJobDO;
import com.vtc.openapi.domain.instance.model.entity.OpenVerifyFixJobItemDO;
import com.vtc.openapi.domain.instance.model.result.InstanceItemResult;
import com.vtc.openapi.domain.instance.repository.IOpenVulnInstanceLogRepository;
import com.vtc.openapi.domain.instance.repository.IOpenVulnInstanceRepository;
import com.vtc.openapi.domain.instance.service.business.IVerifyFixJobDomainService;
import com.vtc.openapi.domain.task.model.entity.OpenTaskDO;
import com.vtc.openapi.domain.task.model.entity.OpenTaskSubDO;
import com.vtc.openapi.domain.task.repository.IOpenTaskSubRepository;
import com.vtc.openapi.infra.adapter.taskcenter.TaskCenterSubSupport;
import com.vtc.openapi.infra.export.ExportInstanceDeduper;
import com.vtc.openapi.infra.converter.InstanceItemConverter;
import com.vtc.openapi.ui.dto.admin.OpenTaskInstanceBriefDto;
import com.vtc.openapi.ui.dto.admin.OpenTaskInstanceScopeDto;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 按 taskId + subId 组装漏洞实例：open_vuln_instance 快照 + open_vuln_instance_log 任务内跃迁。
 */
@Component
public class TaskScopedInstanceLoader {

    private final IOpenVulnInstanceRepository vulnInstanceRepository;
    private final IOpenVulnInstanceLogRepository vulnInstanceLogRepository;
    private final IOpenTaskSubRepository openTaskSubRepository;
    private final ExportInstanceDeduper exportInstanceDeduper;

    public TaskScopedInstanceLoader(IOpenVulnInstanceRepository vulnInstanceRepository,
                                    IOpenVulnInstanceLogRepository vulnInstanceLogRepository,
                                    IOpenTaskSubRepository openTaskSubRepository,
                                    ExportInstanceDeduper exportInstanceDeduper) {
        this.vulnInstanceRepository = vulnInstanceRepository;
        this.vulnInstanceLogRepository = vulnInstanceLogRepository;
        this.openTaskSubRepository = openTaskSubRepository;
        this.exportInstanceDeduper = exportInstanceDeduper;
    }

    /**
     * 外发组装：按阶段下各 sub 的跃迁 log 快照取数，再合并为 task 级实例列表（vulInfoId 去重）。
     */
    public List<OpenVulnInstanceDO> loadMergedInstancesForExport(OpenTaskDO task, int scanPhase) {
        if (task == null || !StringUtils.hasText(task.getPartnerId()) || !StringUtils.hasText(task.getTaskId())) {
            return Collections.emptyList();
        }
        Map<String, OpenVulnInstanceDO> merged = new LinkedHashMap<>();
        List<OpenTaskSubDO> subs = openTaskSubRepository.listByTaskIdAndPhase(task.getTaskId(), scanPhase);
        if (!CollectionUtils.isEmpty(subs)) {
            for (OpenTaskSubDO sub : subs) {
                if (sub == null || !StringUtils.hasText(sub.getSubId())) {
                    continue;
                }
                mergeSubExportInstances(task, sub.getSubId(), scanPhase, merged);
            }
        }
        if (merged.isEmpty()) {
            return dedupeForExport(fallbackTaskInstances(task));
        }
        return dedupeForExport(new ArrayList<>(merged.values()));
    }

    /**
     * 修复核验外发：按 job 下各复扫 sub 关联实例，合并为 task 级列表。
     */
    public List<OpenVulnInstanceDO> loadMergedForVerifyFixExport(OpenTaskDO task,
                                                                 OpenVerifyFixJobDO job,
                                                                 List<OpenVerifyFixJobItemDO> items) {
        if (task == null || job == null || CollectionUtils.isEmpty(items)) {
            return task != null ? fallbackTaskInstances(task) : Collections.emptyList();
        }
        Map<String, OpenVulnInstanceDO> merged = new LinkedHashMap<>();
        Set<String> rescanSubIds = new LinkedHashSet<>();
        for (OpenVerifyFixJobItemDO item : items) {
            if (item == null || !StringUtils.hasText(item.getVulInfoId())) {
                continue;
            }
            if (!task.getTaskId().equals(item.getTaskId())) {
                continue;
            }
            if (StringUtils.hasText(item.getRescanSubId())) {
                rescanSubIds.add(item.getRescanSubId().trim());
            }
        }
        if (!rescanSubIds.isEmpty()) {
            for (String rescanSubId : rescanSubIds) {
                OpenTaskInstanceScopeDto scope = loadVerifyFixScope(
                        job, task.getTaskId(), rescanSubId, items, 10000);
                appendBriefsToExport(task, scope.getInstances(), merged);
            }
        } else {
            for (OpenVerifyFixJobItemDO item : items) {
                if (item == null || !task.getTaskId().equals(item.getTaskId())) {
                    continue;
                }
                OpenVulnInstanceDO instance = vulnInstanceRepository.findByPartnerAndVulInfoId(
                        task.getPartnerId(), item.getVulInfoId());
                if (instance == null) {
                    continue;
                }
                OpenVulnInstanceLogDO verifyLog = findVerifyFixLog(
                        task.getPartnerId(), task.getTaskId(), item.getVulInfoId());
                Integer stat = verifyLog != null ? verifyLog.getVulInfoStat() : instance.getVulInfoStat();
                merged.put(instance.getVulInfoId(), copyForExport(instance, stat));
            }
        }
        if (merged.isEmpty()) {
            return dedupeForExport(fallbackTaskInstances(task));
        }
        return dedupeForExport(new ArrayList<>(merged.values()));
    }

    private List<OpenVulnInstanceDO> dedupeForExport(List<OpenVulnInstanceDO> instances) {
        if (exportInstanceDeduper == null || CollectionUtils.isEmpty(instances)) {
            return instances;
        }
        return exportInstanceDeduper.dedupeSystemVulnerabilities(instances);
    }

    private void mergeSubExportInstances(OpenTaskDO task, String subId, int scanPhase,
                                         Map<String, OpenVulnInstanceDO> merged) {
        Map<String, OpenVulnInstanceLogDO> latestByVulInfoId = loadLatestLogsPerVulInfoId(
                task.getPartnerId(), task.getTaskId(), subId, scanPhase);
        for (OpenVulnInstanceLogDO logRow : latestByVulInfoId.values()) {
            if (logRow == null || !StringUtils.hasText(logRow.getVulInfoId())) {
                continue;
            }
            OpenVulnInstanceDO instance = vulnInstanceRepository.findByPartnerAndVulInfoId(
                    task.getPartnerId(), logRow.getVulInfoId());
            if (instance == null) {
                continue;
            }
            merged.put(logRow.getVulInfoId().trim(),
                    copyForExport(instance, logRow.getVulInfoStat()));
        }
        appendOpenSubIdFallback(task, subId, scanPhase, merged);
    }

    private void appendOpenSubIdFallback(OpenTaskDO task, String subId, int scanPhase,
                                       Map<String, OpenVulnInstanceDO> merged) {
        List<OpenVulnInstanceDO> rows = vulnInstanceRepository.listByPartnerAndTask(
                task.getPartnerId(), task.getTaskId(), task.getExtTaskId());
        if (CollectionUtils.isEmpty(rows)) {
            return;
        }
        for (OpenVulnInstanceDO row : rows) {
            if (row == null || !StringUtils.hasText(row.getVulInfoId())) {
                continue;
            }
            if (merged.containsKey(row.getVulInfoId().trim())) {
                continue;
            }
            if (!subId.equals(extractOpenSubId(row))) {
                continue;
            }
            merged.put(row.getVulInfoId().trim(), copyForExport(row, row.getVulInfoStat()));
        }
    }

    private void appendBriefsToExport(OpenTaskDO task,
                                    List<OpenTaskInstanceBriefDto> briefs,
                                    Map<String, OpenVulnInstanceDO> merged) {
        if (CollectionUtils.isEmpty(briefs)) {
            return;
        }
        for (OpenTaskInstanceBriefDto brief : briefs) {
            if (brief == null || !StringUtils.hasText(brief.getVulInfoId())) {
                continue;
            }
            OpenVulnInstanceDO instance = vulnInstanceRepository.findByPartnerAndVulInfoId(
                    task.getPartnerId(), brief.getVulInfoId());
            if (instance == null) {
                continue;
            }
            merged.put(brief.getVulInfoId().trim(), copyForExport(instance, brief.getVulInfoStat()));
        }
    }

    private Map<String, OpenVulnInstanceLogDO> loadLatestLogsPerVulInfoId(String partnerId,
                                                                          String taskId,
                                                                          String subId,
                                                                          int scanPhase) {
        List<OpenVulnInstanceLogDO> logs = vulnInstanceLogRepository.listByPartnerTaskAndSubId(
                partnerId, taskId, subId, 10000);
        if (CollectionUtils.isEmpty(logs)) {
            return Collections.emptyMap();
        }
        Map<String, OpenVulnInstanceLogDO> latestByVulInfoId = new LinkedHashMap<>();
        for (OpenVulnInstanceLogDO row : logs) {
            if (row == null || !StringUtils.hasText(row.getVulInfoId())) {
                continue;
            }
            if (row.getScanPhase() != null && row.getScanPhase() != scanPhase) {
                continue;
            }
            latestByVulInfoId.put(row.getVulInfoId().trim(), row);
        }
        return latestByVulInfoId;
    }

    private List<OpenVulnInstanceDO> fallbackTaskInstances(OpenTaskDO task) {
        List<OpenVulnInstanceDO> rows = vulnInstanceRepository.listByPartnerAndTask(
                task.getPartnerId(), task.getTaskId(), task.getExtTaskId());
        return rows != null ? rows : Collections.emptyList();
    }

    private static OpenVulnInstanceDO copyForExport(OpenVulnInstanceDO source, Integer vulInfoStat) {
        OpenVulnInstanceDO row = new OpenVulnInstanceDO();
        row.setId(source.getId());
        row.setPartnerId(source.getPartnerId());
        row.setVulInfoId(source.getVulInfoId());
        row.setVulnDisposalId(source.getVulnDisposalId());
        row.setEngineTaskId(source.getEngineTaskId());
        row.setTaskId(source.getTaskId());
        row.setExtTaskId(source.getExtTaskId());
        row.setScanTemplateId(source.getScanTemplateId());
        row.setReportTemplateId(source.getReportTemplateId());
        row.setBundleId(source.getBundleId());
        row.setIngestStatus(source.getIngestStatus());
        row.setIngestAt(source.getIngestAt());
        row.setVulInfoStat(vulInfoStat != null ? vulInfoStat : source.getVulInfoStat());
        row.setSnapshotJson(source.getSnapshotJson());
        row.setCreatedAt(source.getCreatedAt());
        row.setUpdatedAt(source.getUpdatedAt());
        return row;
    }

    public OpenTaskInstanceScopeDto loadSurveyScope(OpenTaskDO task, OpenTaskSubDO sub, int scanPhase, int limit) {
        OpenTaskInstanceScopeDto scope = new OpenTaskInstanceScopeDto();
        if (task == null || sub == null) {
            scope.setHint("子任务不存在");
            return scope;
        }
        scope.setTaskId(task.getTaskId());
        scope.setSubId(sub.getSubId());
        scope.setScanPhase(scanPhase);
        List<OpenTaskInstanceBriefDto> instances = loadFromTaskSubLogs(
                task.getPartnerId(), task.getTaskId(), sub.getSubId(), scanPhase, limit);
        if (CollectionUtils.isEmpty(instances)) {
            instances = loadFromInstancesByOpenSubId(task, sub.getSubId(), scanPhase, limit);
            if (CollectionUtils.isEmpty(instances)) {
                scope.setHint("该子任务暂无漏洞实例快照，请等待扫描入库或选择其他 sub");
            }
        }
        scope.setInstances(instances);
        scope.setInstanceStatCounts(buildStatCounts(instances));
        return scope;
    }

    public OpenTaskInstanceScopeDto loadVerifyFixScope(OpenVerifyFixJobDO job,
                                                       String taskId,
                                                       String rescanSubId,
                                                       List<OpenVerifyFixJobItemDO> allItems,
                                                       int limit) {
        OpenTaskInstanceScopeDto scope = new OpenTaskInstanceScopeDto();
        if (job == null || !StringUtils.hasText(taskId) || !StringUtils.hasText(rescanSubId)) {
            scope.setHint("taskId/subId 不能为空");
            return scope;
        }
        scope.setTaskId(taskId.trim());
        scope.setSubId(rescanSubId.trim());
        scope.setScanPhase(TaskCenterSubSupport.PHASE_VERIFY_FIX);
        boolean jobFinished = IVerifyFixJobDomainService.STATUS_FINISHED.equals(job.getStatus());
        scope.setVerified(jobFinished);

        List<OpenVerifyFixJobItemDO> scoped = new ArrayList<>();
        if (!CollectionUtils.isEmpty(allItems)) {
            for (OpenVerifyFixJobItemDO item : allItems) {
                if (item == null || !StringUtils.hasText(item.getVulInfoId())) {
                    continue;
                }
                if (!taskId.trim().equals(item.getTaskId())) {
                    continue;
                }
                if (!rescanSubId.trim().equals(item.getRescanSubId())) {
                    continue;
                }
                scoped.add(item);
            }
        }
        if (scoped.isEmpty()) {
            scope.setHint("该 taskId+复扫 sub 下暂无待核验实例");
            return scope;
        }

        List<OpenTaskInstanceBriefDto> instances = new ArrayList<>();
        int max = Math.max(1, limit);
        for (OpenVerifyFixJobItemDO item : scoped) {
            if (instances.size() >= max) {
                break;
            }
            OpenVulnInstanceDO instance = vulnInstanceRepository.findByPartnerAndVulInfoId(
                    job.getPartnerId(), item.getVulInfoId());
            OpenVulnInstanceLogDO verifyLog = findVerifyFixLog(
                    job.getPartnerId(), taskId.trim(), item.getVulInfoId());
            instances.add(toVerifyFixBrief(item, instance, verifyLog, rescanSubId.trim()));
        }
        scope.setInstances(instances);
        scope.setInstanceStatCounts(buildStatCounts(instances));
        if (!jobFinished && !instances.isEmpty()) {
            scope.setHint("核验前：展示 open_vuln_instance 快照；核验完成后将关联 VERIFY_FIX_COMPLETE 跃迁 log");
        }
        return scope;
    }

    private List<OpenTaskInstanceBriefDto> loadFromTaskSubLogs(String partnerId,
                                                               String taskId,
                                                               String subId,
                                                               int scanPhase,
                                                               int limit) {
        List<OpenVulnInstanceLogDO> logs = vulnInstanceLogRepository.listByPartnerTaskAndSubId(
                partnerId, taskId, subId, 10000);
        if (CollectionUtils.isEmpty(logs)) {
            return Collections.emptyList();
        }
        Map<String, OpenVulnInstanceLogDO> latestByVulInfoId = new LinkedHashMap<>();
        for (OpenVulnInstanceLogDO row : logs) {
            if (row == null || !StringUtils.hasText(row.getVulInfoId())) {
                continue;
            }
            if (row.getScanPhase() != null && row.getScanPhase() != scanPhase) {
                continue;
            }
            if (OpenVulnInstanceLogDO.REASON_SURVEY_INGEST.equals(row.getChangeReason())
                    || !StringUtils.hasText(row.getChangeReason())) {
                latestByVulInfoId.put(row.getVulInfoId().trim(), row);
            }
        }
        if (latestByVulInfoId.isEmpty()) {
            for (OpenVulnInstanceLogDO row : logs) {
                if (row == null || !StringUtils.hasText(row.getVulInfoId())) {
                    continue;
                }
                if (row.getScanPhase() != null && row.getScanPhase() != scanPhase) {
                    continue;
                }
                latestByVulInfoId.put(row.getVulInfoId().trim(), row);
            }
        }
        List<OpenTaskInstanceBriefDto> result = new ArrayList<>();
        int max = Math.max(1, limit);
        for (OpenVulnInstanceLogDO logRow : latestByVulInfoId.values()) {
            if (result.size() >= max) {
                break;
            }
            OpenVulnInstanceDO instance = vulnInstanceRepository.findByPartnerAndVulInfoId(
                    partnerId, logRow.getVulInfoId());
            result.add(toSurveyBrief(logRow, instance));
        }
        return result;
    }

    private List<OpenTaskInstanceBriefDto> loadFromInstancesByOpenSubId(OpenTaskDO task,
                                                                      String subId,
                                                                      int scanPhase,
                                                                      int limit) {
        List<OpenVulnInstanceDO> rows = vulnInstanceRepository.listByPartnerAndTask(
                task.getPartnerId(), task.getTaskId(), task.getExtTaskId());
        if (CollectionUtils.isEmpty(rows)) {
            return Collections.emptyList();
        }
        List<OpenTaskInstanceBriefDto> result = new ArrayList<>();
        int max = Math.max(1, limit);
        for (OpenVulnInstanceDO row : rows) {
            if (result.size() >= max) {
                break;
            }
            String openSubId = extractOpenSubId(row);
            if (!subId.equals(openSubId)) {
                continue;
            }
            OpenTaskInstanceBriefDto brief = new OpenTaskInstanceBriefDto();
            brief.setVulInfoId(row.getVulInfoId());
            brief.setVulInfoStat(row.getVulInfoStat());
            brief.setSubId(subId);
            brief.setScanPhase(scanPhase);
            fillFromSnapshot(brief, row);
            result.add(brief);
        }
        return result;
    }

    private OpenVulnInstanceLogDO findVerifyFixLog(String partnerId, String taskId, String vulInfoId) {
        List<OpenVulnInstanceLogDO> logs = vulnInstanceLogRepository.listByVulInfoId(partnerId, vulInfoId, 100);
        if (CollectionUtils.isEmpty(logs)) {
            return null;
        }
        for (int i = logs.size() - 1; i >= 0; i--) {
            OpenVulnInstanceLogDO row = logs.get(i);
            if (row == null) {
                continue;
            }
            if (!OpenVulnInstanceLogDO.REASON_VERIFY_FIX_COMPLETE.equals(row.getChangeReason())) {
                continue;
            }
            if (StringUtils.hasText(row.getTaskId()) && !taskId.equals(row.getTaskId().trim())) {
                continue;
            }
            return row;
        }
        return null;
    }

    private static OpenTaskInstanceBriefDto toSurveyBrief(OpenVulnInstanceLogDO logRow,
                                                          OpenVulnInstanceDO instance) {
        OpenTaskInstanceBriefDto brief = new OpenTaskInstanceBriefDto();
        brief.setVulInfoId(logRow.getVulInfoId());
        brief.setVulInfoStat(logRow.getVulInfoStat());
        brief.setSubId(logRow.getSubId());
        brief.setScanPhase(logRow.getScanPhase());
        if (instance != null) {
            fillFromSnapshot(brief, instance);
            if (!StringUtils.hasText(brief.getSubId())) {
                brief.setSubId(extractOpenSubId(instance));
            }
        }
        return brief;
    }

    private static OpenTaskInstanceBriefDto toVerifyFixBrief(OpenVerifyFixJobItemDO item,
                                                             OpenVulnInstanceDO instance,
                                                             OpenVulnInstanceLogDO verifyLog,
                                                             String rescanSubId) {
        OpenTaskInstanceBriefDto brief = new OpenTaskInstanceBriefDto();
        brief.setVulInfoId(item.getVulInfoId());
        brief.setSubId(rescanSubId);
        brief.setScanPhase(TaskCenterSubSupport.PHASE_VERIFY_FIX);
        brief.setPreviousStat(item.getPreviousStat());
        if (verifyLog != null) {
            brief.setResultStat(verifyLog.getVulInfoStat());
            brief.setVulInfoStat(verifyLog.getVulInfoStat());
            if (verifyLog.getPrevStat() != null) {
                brief.setPreviousStat(verifyLog.getPrevStat());
            }
        } else if (instance != null) {
            brief.setVulInfoStat(instance.getVulInfoStat());
        }
        if (instance != null) {
            fillFromSnapshot(brief, instance);
        }
        return brief;
    }

    private static void fillFromSnapshot(OpenTaskInstanceBriefDto brief, OpenVulnInstanceDO instance) {
        InstanceItemResult item = InstanceItemConverter.fromSnapshot(instance);
        if (item == null) {
            return;
        }
        brief.setAddress(item.getVulNetAddr());
        brief.setPort(item.getVulPort() != null ? String.valueOf(item.getVulPort()) : null);
        brief.setVulnName(item.getVulName());
        brief.setLevel(item.getVulLevel() != null ? String.valueOf(item.getVulLevel()) : null);
        brief.setOrgVulId(item.getOrgVulId());
    }

    private static String extractOpenSubId(OpenVulnInstanceDO instance) {
        if (instance == null || !StringUtils.hasText(instance.getSnapshotJson())) {
            return null;
        }
        JSONObject snap = JSON.parseObject(instance.getSnapshotJson());
        return snap != null ? snap.getString("openSubId") : null;
    }

    private static Map<String, Long> buildStatCounts(List<OpenTaskInstanceBriefDto> instances) {
        if (CollectionUtils.isEmpty(instances)) {
            return Collections.emptyMap();
        }
        Map<String, Long> counts = new LinkedHashMap<>();
        for (OpenTaskInstanceBriefDto row : instances) {
            String key = row.getVulInfoStat() != null ? String.valueOf(row.getVulInfoStat()) : "unknown";
            counts.merge(key, 1L, Long::sum);
        }
        return counts;
    }
}
