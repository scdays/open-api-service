package com.vtc.openapi.ui.params.admin;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.List;

@Data
@ApiModel("从离线导入任务创建内部修复核验任务")
public class CreateInternalVerifyFixJobParams {

    @NotBlank(message = "partnerId 不能为空")
    @ApiModelProperty(value = "接入方 ID", required = true)
    private String partnerId;

    @NotBlank(message = "taskId 不能为空")
    @ApiModelProperty(value = "平台 taskId（离线导入所属任务）", required = true)
    private String taskId;

    @ApiModelProperty("仅纳入指定 vulInfoID；为空则纳入任务下全部 stat=5 实例")
    private List<String> vulInfoIds;

    @ApiModelProperty("可选批次号，默认 soc-internal-{taskId}")
    private String batchId;
}
