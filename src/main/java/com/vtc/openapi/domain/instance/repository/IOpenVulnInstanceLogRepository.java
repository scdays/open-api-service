package com.vtc.openapi.domain.instance.repository;

import com.vtc.openapi.domain.instance.model.entity.OpenVulnInstanceLogDO;

import java.util.List;

public interface IOpenVulnInstanceLogRepository {

    void insertBatch(List<OpenVulnInstanceLogDO> rows);

    List<OpenVulnInstanceLogDO> listByVulInfoId(String partnerId, String vulInfoId, int limit);
}
