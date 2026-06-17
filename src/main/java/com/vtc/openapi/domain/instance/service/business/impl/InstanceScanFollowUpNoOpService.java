package com.vtc.openapi.domain.instance.service.business.impl;

import com.vtc.openapi.domain.instance.service.business.IInstanceScanFollowUpService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnMissingBean(MockInstanceScanFollowUpService.class)
public class InstanceScanFollowUpNoOpService implements IInstanceScanFollowUpService {

    @Override
    public void scheduleVerifyScan(String partnerId, String vulInfoId) {
    }
}
