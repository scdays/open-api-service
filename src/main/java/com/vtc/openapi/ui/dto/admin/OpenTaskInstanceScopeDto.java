package com.vtc.openapi.ui.dto.admin;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@ApiModel("按 taskId+subId 查询的漏洞实例范围")
public class OpenTaskInstanceScopeDto {

    @ApiModelProperty("任务 ID")
    private String taskId;

    @ApiModelProperty("子任务 subId")
    private String subId;

    @ApiModelProperty("扫描阶段 1=排查 2=验证 3=修复核验复扫")
    private Integer scanPhase;

    @ApiModelProperty("说明（无数据或待入库时）")
    private String hint;

    @ApiModelProperty("是否已完成核验（修复核验场景）")
    private Boolean verified;

    @ApiModelProperty("实例状态分布 vulInfoStat -> count")
    private Map<String, Long> instanceStatCounts = new LinkedHashMap<>();

    @ApiModelProperty("漏洞实例列表")
    private List<OpenTaskInstanceBriefDto> instances = new ArrayList<>();
}
