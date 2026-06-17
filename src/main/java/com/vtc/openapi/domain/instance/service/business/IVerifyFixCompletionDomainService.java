package com.vtc.openapi.domain.instance.service.business;

import com.vtc.openapi.ui.dto.internal.VerifyFixCompletedNotifyRequest;

/**
 * verify-fix 异步完成后的 Partner 侧同步：实例状态、外发与 Webhook。
 */
public interface IVerifyFixCompletionDomainService {

    void onVerifyFixCompleted(String verifyFixJobId, VerifyFixCompletedNotifyRequest request);
}
