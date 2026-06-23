package com.vtc.openapi.domain.instance.repository;

import com.vtc.openapi.domain.instance.model.entity.OpenVulnInstanceLogDO;

import java.util.List;

public interface IOpenVulnInstanceLogRepository {

    void insertBatch(List<OpenVulnInstanceLogDO> rows);

    List<OpenVulnInstanceLogDO> listByVulInfoId(String partnerId, String vulInfoId, int limit);

    List<OpenVulnInstanceLogDO> listByCaseId(String caseId, int limit);

    /**
     * 按任务查询跃迁日志（按 id 升序，便于取同一 vulInfoId 在任务内的最终状态）。
     */
    List<OpenVulnInstanceLogDO> listByPartnerAndTaskId(String partnerId, String taskId, int limit);

    /**
     * 按任务与子任务查询跃迁日志（按 id 升序）。
     */
    List<OpenVulnInstanceLogDO> listByPartnerTaskAndSubId(String partnerId, String taskId, String subId, int limit);

    int deleteByTaskIdAndScanPhase(String taskId, int scanPhase);

    List<String> listVulInfoIdsByTaskSubAndPhase(String taskId, String subId, int scanPhase);

    int deleteByTaskIdAndSubIdAndScanPhase(String taskId, String subId, int scanPhase);
}
