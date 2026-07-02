package com.vtc.openapi.ui.dto.admin;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@ApiModel("修复核验工作台")
public class VerifyFixWorkspaceDto {

    @ApiModelProperty("修复核验 job")
    private MockVerifyFixJobDto job;

    @ApiModelProperty("复扫子任务 open_task_sub phase=3")
    private List<OpenTaskSubDto> rescanSubs = new ArrayList<>();

    @ApiModelProperty("条目状态统计 itemStatus -> count")
    private Map<String, Long> itemStatCounts;

    @ApiModelProperty("结果状态统计 vulInfoStat -> count")
    private Map<String, Long> itemResultCounts;

    @ApiModelProperty("关联 open_task 摘要")
    private List<OpenTaskAdminDto> relatedTasks = new ArrayList<>();

    @ApiModelProperty("全链路时序")
    private List<OpenTaskTimelineEventDto> timeline = new ArrayList<>();

    @ApiModelProperty("约束说明（展示）")
    private List<String> constraints = new ArrayList<>();
}
