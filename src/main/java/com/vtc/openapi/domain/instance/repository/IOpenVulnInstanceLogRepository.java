package com.vtc.openapi.domain.instance.repository;

import com.vtc.openapi.domain.instance.model.entity.OpenVulnInstanceLogDO;

import java.util.List;

public interface IOpenVulnInstanceLogRepository {

    void insertBatch(List<OpenVulnInstanceLogDO> rows);

    List<OpenVulnInstanceLogDO> listByVulInfoId(String partnerId, String vulInfoId, int limit);

    List<OpenVulnInstanceLogDO> listByCaseId(String caseId, int limit);

    int deleteByTaskIdAndScanPhase(String taskId, int scanPhase);

    List<String> listVulInfoIdsByTaskSubAndPhase(String taskId, String subId, int scanPhase);

    int deleteByTaskIdAndSubIdAndScanPhase(String taskId, String subId, int scanPhase);
}
