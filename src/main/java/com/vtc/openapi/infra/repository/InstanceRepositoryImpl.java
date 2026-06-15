package com.vtc.openapi.infra.repository;

import com.vtc.openapi.domain.instance.model.command.SearchInstanceCommand;
import com.vtc.openapi.domain.instance.model.result.InstanceItemResult;
import com.vtc.openapi.domain.instance.model.result.InstancePageResult;
import com.vtc.openapi.domain.instance.repository.IInstanceRepository;
import com.vtc.openapi.infra.adapter.IVulnInstanceGateway;
import org.springframework.stereotype.Repository;

/**
 * 实例仓储实现：委托 IVulnInstanceGateway 与引擎交互。
 */
@Repository
public class InstanceRepositoryImpl implements IInstanceRepository {

    private final IVulnInstanceGateway vulnInstanceGateway;

    public InstanceRepositoryImpl(IVulnInstanceGateway vulnInstanceGateway) {
        this.vulnInstanceGateway = vulnInstanceGateway;
    }

    @Override
    public InstancePageResult searchInstances(String partnerId, SearchInstanceCommand command) {
        return vulnInstanceGateway.searchInstances(command);
    }

    @Override
    public InstanceItemResult findByVulInfoId(String partnerId, String vulInfoId) {
        return vulnInstanceGateway.findByVulInfoId(vulInfoId);
    }

    @Override
    public void updateInstanceState(Long id, Integer targetStat, String srcMethod, String remedDesc) {
        int stat = targetStat != null ? targetStat : 0;
        vulnInstanceGateway.updateInstance(id, stat, srcMethod, remedDesc);
    }
}
