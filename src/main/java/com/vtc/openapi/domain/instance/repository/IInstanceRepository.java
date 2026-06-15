package com.vtc.openapi.domain.instance.repository;

import com.vtc.openapi.domain.instance.model.command.SearchInstanceCommand;
import com.vtc.openapi.domain.instance.model.result.InstanceItemResult;
import com.vtc.openapi.domain.instance.model.result.InstancePageResult;

/**
 * 实例仓储接口。
 */
public interface IInstanceRepository {

    /** 分页搜索实例 */
    InstancePageResult searchInstances(String partnerId, SearchInstanceCommand command);

    /** 按 vulInfoId 查找实例（含 Partner 隔离） */
    InstanceItemResult findByVulInfoId(String partnerId, String vulInfoId);

    /** 更新实例状态 */
    void updateInstanceState(Long id, Integer targetStat, String srcMethod, String remedDesc);
}