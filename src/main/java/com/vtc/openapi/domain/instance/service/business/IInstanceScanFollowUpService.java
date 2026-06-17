package com.vtc.openapi.domain.instance.service.business;

/**
 * Mock 验证阶段复扫跟进（verify-fix 已迁至 {@link IVerifyFixJobDomainService}）。
 */
public interface IInstanceScanFollowUpService {

    void scheduleVerifyScan(String partnerId, String vulInfoId);
}
