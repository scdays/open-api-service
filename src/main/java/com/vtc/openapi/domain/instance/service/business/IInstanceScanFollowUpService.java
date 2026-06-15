package com.vtc.openapi.domain.instance.service.business;

/**
 * Mock 验证/核验复扫跟进（无操作实现，供非 mock 模式注入）。
 */
public interface IInstanceScanFollowUpService {

    void scheduleVerifyScan(String partnerId, String vulInfoId);

    void scheduleVerifyFixScan(String partnerId, String vulInfoId, int previousStat, int newStat, String batchId);
}
