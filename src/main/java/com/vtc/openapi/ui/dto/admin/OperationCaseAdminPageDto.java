package com.vtc.openapi.ui.dto.admin;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel("OperationCaseAdminPageDto")
public class OperationCaseAdminPageDto {

    @ApiModelProperty("记录列表")
    private List<OperationCaseAdminDto> items;

    @ApiModelProperty("当前页（从 1 开始）")
    private int page;

    @ApiModelProperty("每页条数")
    private int size;

    @ApiModelProperty("总记录数")
    private long total;
}
