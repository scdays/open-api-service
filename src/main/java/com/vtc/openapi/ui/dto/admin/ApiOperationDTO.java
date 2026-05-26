package com.vtc.openapi.ui.dto.admin;

import com.botany.spore.ddd.ui.dto.BaseDTO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("ApiOperationDTO")
public class ApiOperationDTO extends BaseDTO {

    @ApiModelProperty("operationId")
    private String operationId;

    @ApiModelProperty("HTTP 方法")
    private String httpMethod;

    @ApiModelProperty("路径模板")
    private String pathPattern;

    @ApiModelProperty("所需能力码")
    private String requiredCapability;

    @ApiModelProperty("业务域")
    private String domain;

    @ApiModelProperty("状态")
    private String status;

    @ApiModelProperty("OpenAPI Tag")
    private String openapiTag;

    @ApiModelProperty("接口摘要")
    private String summary;

    @ApiModelProperty("发布时间")
    private Date publishedAt;
}
