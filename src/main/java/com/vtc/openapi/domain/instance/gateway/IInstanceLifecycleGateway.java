package com.vtc.openapi.domain.instance.gateway;

import com.vtc.openapi.domain.instance.model.command.VerifyFixInstanceCommand;
import com.vtc.openapi.domain.instance.model.result.VerifyFixSubmitResult;

public interface IInstanceLifecycleGateway {

    boolean isAsyncVerifyFixEnabled();

    VerifyFixSubmitResult submitVerifyFix(String partnerId, VerifyFixInstanceCommand command);
}
