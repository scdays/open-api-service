package com.vtc.openapi.domain.task.service.business;

import com.vtc.openapi.domain.open.model.InvocationContext;
import com.vtc.openapi.domain.task.model.command.CreateOpenTaskCommand;
import com.vtc.openapi.domain.task.model.query.OpenTaskListQuery;
import com.vtc.openapi.domain.task.model.result.OpenTaskCreatedResult;
import com.vtc.openapi.domain.task.model.result.OpenTaskListResult;
import com.vtc.openapi.domain.task.model.result.OpenTaskProgressResult;

public interface IOpenTaskDomainService {

    OpenTaskCreatedResult create(InvocationContext ctx, CreateOpenTaskCommand command);

    OpenTaskProgressResult get(InvocationContext ctx, String taskId);

    OpenTaskListResult list(InvocationContext ctx, OpenTaskListQuery query);
}
