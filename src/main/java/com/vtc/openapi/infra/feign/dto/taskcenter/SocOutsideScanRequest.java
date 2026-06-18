package com.vtc.openapi.infra.feign.dto.taskcenter;

import lombok.Data;

/**
 * POST /event/scan/task/outside/soc/scan 请求体。
 */
@Data
public class SocOutsideScanRequest {

    private String taskId;
    private String taskName;
    private String inputIp;
    /** vuln | port | alive */
    private String taskType;
    /** 1绿盟 7Nessus 等 */
    private String scannerType;
}
