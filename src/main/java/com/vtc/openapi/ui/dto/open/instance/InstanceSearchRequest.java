package com.vtc.openapi.ui.dto.open.instance;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import lombok.Data;

/**
 * 实例搜索请求（§5.2.1）。
 */
@Data
@ApiModel(description = "实例搜索请求")
public class InstanceSearchRequest {

    @ApiModelProperty(value = "平台任务 ID")
    private String taskId;
    @ApiModelProperty(value = "Partner 外部任务 ID，平台解析为 taskId")
    private String extTaskId;
    @ApiModelProperty(value = "实例状态过滤列表，空表示不过滤")
    private List<Integer> vulInfoStatList;
    @ApiModelProperty(value = "危害等级过滤列表")
    private List<Integer> vulLevelList;
    @ApiModelProperty(value = "页码，从 1 开始", required = true)
    private Integer page = 1;
    @ApiModelProperty(value = "每页条数，≤1000", required = true)
    private Integer size = 20;
}
