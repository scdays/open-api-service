package com.vtc.openapi.ui.dto.admin;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel("PartnerPageDto")
public class PartnerPageDto {

    @ApiModelProperty("当前页数据")
    private List<PartnerDTO> items;

    @ApiModelProperty("总条数")
    private long total;

    @ApiModelProperty("当前页")
    private int page;

    @ApiModelProperty("页大小")
    private int size;
}
