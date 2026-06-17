package com.vtc.openapi.infra.feign.dto;

import lombok.Data;

import java.util.List;

@Data
public class VulPassCreateOpenTaskResponse {

    private Long passTaskId;
    private String status;
    private List<VulPassSubTaskCreated> subTasks;
    private String message;
}
