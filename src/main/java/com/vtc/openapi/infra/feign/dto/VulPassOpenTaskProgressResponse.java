package com.vtc.openapi.infra.feign.dto;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class VulPassOpenTaskProgressResponse {

    private Long passTaskId;
    private String status;
    private Integer progress;
    private List<VulPassSubTaskProgress> subTasks;
    private Date finishedAt;
}
