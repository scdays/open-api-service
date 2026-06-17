package com.vtc.openapi.infra.adapter;

import com.vtc.openapi.domain.instance.model.command.RemediateInstanceCommand;
import com.vtc.openapi.domain.instance.model.command.SearchInstanceCommand;
import com.vtc.openapi.domain.instance.model.result.InstanceItemResult;
import com.vtc.openapi.domain.instance.model.result.InstancePageResult;

public interface IVulnInstanceGateway {

    InstancePageResult searchInstances(SearchInstanceCommand command);

    InstanceItemResult findByVulInfoId(String vulInfoId);

    void updateInstance(Long id, int vulInfoStat, String method, String remedDesc);

    void updateRemediateInstance(Long id, int vulInfoStat, RemediateInstanceCommand command);
}
