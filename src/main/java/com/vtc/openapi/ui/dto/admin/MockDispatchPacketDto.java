package com.vtc.openapi.ui.dto.admin;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Dispatch context for manual mock workflow (copy to vuln-task-center / scanner).
 */
@Data
public class MockDispatchPacketDto {

    private String taskId;

    private String extTaskId;

    private String engineTaskId;

    private String partnerId;

    private String taskName;

    private String status;

    private Integer scanTemplateId;

    private Integer reportTemplateId;

    private Integer vulnType;

    private List<String> targets = new ArrayList<>();

    private String targetsJson;

    private String optionsJson;

    private String ingestMode;

    /** Writable tasks/{taskId} directory hint for operators */
    private String taskBundleDir;
}
