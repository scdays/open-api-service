package com.vtc.openapi.domain.task.model.query;

import lombok.Data;

import java.util.Date;

@Data
public class OpenTaskListQuery {

    private String extTaskId;

    private String status;

    private Date createdFrom;

    private Date createdTo;

    private int page;

    private int size;
}
