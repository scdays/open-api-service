package com.vtc.openapi.domain.instance.repository;

import com.botany.spore.ddd.domain.repository.IDatabaseRepository;
import com.vtc.openapi.domain.instance.model.entity.OpenVulnInstanceDO;
import com.vtc.openapi.domain.instance.model.command.RemediateInstanceCommand;
import com.vtc.openapi.domain.instance.model.command.SearchInstanceCommand;
import com.vtc.openapi.domain.instance.model.result.InstanceItemResult;
import com.vtc.openapi.domain.instance.model.result.InstancePageResult;

import java.util.List;

public interface IOpenVulnInstanceRepository extends IDatabaseRepository<OpenVulnInstanceDO> {

    boolean existsByPartnerAndTaskId(String partnerId, String taskId);

    long countByPartnerAndTaskId(String partnerId, String taskId);

    List<OpenVulnInstanceDO> listByPartnerAndTask(String partnerId, String taskId, String extTaskId);

    OpenVulnInstanceDO findByPartnerAndVulInfoId(String partnerId, String vulInfoId);

    OpenVulnInstanceDO findByIdAndPartner(Long id, String partnerId);

    InstancePageResult searchFromDb(String partnerId, SearchInstanceCommand command);

    void batchInsert(List<OpenVulnInstanceDO> instances);

    int deleteByPartnerAndTaskId(String partnerId, String taskId);

    void updateState(Long id, String partnerId, int vulInfoStat, String method, String remedDesc);

    void updateRemediateState(Long id, String partnerId, int vulInfoStat, RemediateInstanceCommand command);
}
