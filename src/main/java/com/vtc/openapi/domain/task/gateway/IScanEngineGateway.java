package com.vtc.openapi.domain.task.gateway;

import com.vtc.openapi.domain.task.model.vo.ScanEngineCreateCommand;
import com.vtc.openapi.domain.task.model.vo.ScanEngineCreateResult;
import com.vtc.openapi.domain.task.model.vo.ScanEngineProgressResult;

/**
 * 扫描引擎防腐层（Domain 定义，Infra 实现）。
 */
public interface IScanEngineGateway {

    ScanEngineCreateResult createTask(ScanEngineCreateCommand command);

    ScanEngineProgressResult getTaskProgress(String engineTaskId);
}
