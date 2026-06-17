package com.vtc.openapi.ui.dto.admin;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@ApiModel("离线任务修复核验上下文")
public class OfflineTaskVerifyFixContextDto {

    private String taskId;
    private String extTaskId;
    private String partnerId;
    private String taskStatus;
    private Boolean instancesIngested;
    private Integer persistedInstanceCount;
    private Boolean hasSourceXml;

    @ApiModelProperty("各 vulInfoStat 实例数")
    private Map<String, Integer> statCounts = new LinkedHashMap<>();

    @ApiModelProperty("stat=5 可纳入修复核验的 vulInfoID")
    private List<String> eligibleVulInfoIds = new ArrayList<>();
}
