package com.vtc.openapi.app.open;

import com.vtc.openapi.domain.open.model.InvocationContext;

/**
 * 执行平面业务处理器（由 InvocationPipeline 环绕审计）。
 */
@FunctionalInterface
public interface OpenOperationHandler<T> {

    T execute(InvocationContext ctx);
}
