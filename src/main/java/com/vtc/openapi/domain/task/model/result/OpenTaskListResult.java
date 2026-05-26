package com.vtc.openapi.domain.task.model.result;

import lombok.Data;

import java.util.List;

@Data
public class OpenTaskListResult {

    private int page;

    private int size;

    private long total;

    private List<OpenTaskSummaryResult> items;
}
