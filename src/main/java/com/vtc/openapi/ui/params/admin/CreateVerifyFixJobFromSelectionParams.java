package com.vtc.openapi.ui.params.admin;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
@ApiModel("从 Partner 修复核验调用所选实例创建/归入内部任务")
public class CreateVerifyFixJobFromSelectionParams {

    @NotBlank(message = "partnerId 不能为空")
    @ApiModelProperty(value = "接入方 ID", required = true)
    private String partnerId;

    @NotEmpty(message = "vulInfoIds 不能为空")
    @ApiModelProperty(value = "所选 vulInfoID 列表", required = true)
    private List<String> vulInfoIds;

    @ApiModelProperty("可选批次号")
    private String batchId;
}
