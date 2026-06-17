package com.vtc.openapi.infra.adapter;

import com.vtc.openapi.domain.instance.gateway.IInstanceLifecycleGateway;
import com.vtc.openapi.domain.instance.model.command.VerifyFixInstanceCommand;
import com.vtc.openapi.domain.instance.model.result.VerifyFixSubmitResult;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "open-api.svmp.orchestration.enabled", havingValue = "false", matchIfMissing = true)
public class InstanceLifecycleGatewayNoOpImpl implements IInstanceLifecycleGateway {

    @Override
    public boolean isAsyncVerifyFixEnabled() {
        return false;
    }

    @Override
    public VerifyFixSubmitResult submitVerifyFix(String partnerId, VerifyFixInstanceCommand command) {
        throw new UnsupportedOperationException("async verify-fix not enabled");
    }
}
