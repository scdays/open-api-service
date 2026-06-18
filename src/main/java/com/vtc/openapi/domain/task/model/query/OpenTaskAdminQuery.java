package com.vtc.openapi.domain.task.model.query;

import lombok.Data;

@Data
public class OpenTaskAdminQuery {

    private String partnerId;

    private String taskId;

    private String extTaskId;

    private String status;

    private Integer scanTemplateId;

    private Integer vulnType;

    private int page = 1;

    private int size = 20;
}
