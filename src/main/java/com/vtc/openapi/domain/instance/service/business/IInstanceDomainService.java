package com.vtc.openapi.domain.instance.service.business;

import com.vtc.openapi.domain.instance.model.command.RemediateInstanceCommand;
import com.vtc.openapi.domain.instance.model.command.SearchInstanceCommand;
import com.vtc.openapi.domain.instance.model.command.VerifyFixInstanceCommand;
import com.vtc.openapi.domain.instance.model.command.VerifyInstanceCommand;
import com.vtc.openapi.domain.instance.model.result.InstanceItemResult;
import com.vtc.openapi.domain.instance.model.result.InstancePageResult;
import com.vtc.openapi.domain.instance.model.result.InstanceStateResult;

/**
 * 实例领域服务接口：搜索、详情、验证、修复、核验修复。
 */
public interface IInstanceDomainService {

    /** 搜索实例 */
    InstancePageResult search(String partnerId, SearchInstanceCommand command);

    /** 按 vulInfoId 查询实例 */
    InstanceItemResult getByVulInfoId(String partnerId, String vulInfoId);

    /** 验证实例 */
    InstanceStateResult verify(String partnerId, VerifyInstanceCommand command);

    /** 修复实例 */
    InstanceStateResult remediate(String partnerId, RemediateInstanceCommand command);

    /** 核验修复 */
    InstanceStateResult verifyFix(String partnerId, VerifyFixInstanceCommand command);
}