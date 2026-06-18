package com.vtc.openapi.ui.dto.admin;

import com.botany.spore.ddd.ui.dto.BaseDTO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("InvocationDTO")
public class InvocationDTO extends BaseDTO {

    @ApiModelProperty("调用记录 ID")
    private String invocationId;

    @ApiModelProperty("请求追踪 ID")
    private String requestId;

    @ApiModelProperty("Partner ID")
    private String partnerId;

    @ApiModelProperty("操作 ID")
    private String operationId;

    @ApiModelProperty("HTTP 方法")
    private String httpMethod;

    @ApiModelProperty("业务域 TASK/INSTANCE/EXPORT/AUTH")
    private String domain;

    @ApiModelProperty("业务响应码")
    private Integer responseCode;

    @ApiModelProperty("HTTP 状态码")
    private Integer httpStatus;

    @ApiModelProperty("耗时（毫秒）")
    private Integer latencyMs;

    @ApiModelProperty("请求路径")
    private String requestPath;

    @ApiModelProperty("资源类型")
    private String resourceType;

    @ApiModelProperty("资源 ID")
    private String resourceId;

    @ApiModelProperty("运营案件 ID")
    private String caseId;

    @ApiModelProperty("错误摘要")
    private String errorMessage;

    @ApiModelProperty("开始时间")
    private Date startedAt;

    @ApiModelProperty("结束时间")
    private Date finishedAt;
}
