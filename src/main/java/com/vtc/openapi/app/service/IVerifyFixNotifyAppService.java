package com.vtc.openapi.app.service;

import com.vtc.openapi.ui.dto.internal.VerifyFixCompletedNotifyRequest;

import java.util.Map;

public interface IVerifyFixNotifyAppService {

    Map<String, Object> notifyCompleted(String verifyFixJobId, VerifyFixCompletedNotifyRequest request);
}
