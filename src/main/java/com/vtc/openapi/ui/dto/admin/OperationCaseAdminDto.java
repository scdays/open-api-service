package com.vtc.openapi.ui.dto.admin;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

@Data
@ApiModel("OperationCaseAdminDto")
public class OperationCaseAdminDto {

    @ApiModelProperty("案件 ID")
    private String caseId;

    @ApiModelProperty("Partner ID")
    private String partnerId;

    @ApiModelProperty("案件类型")
    private String caseType;

    @ApiModelProperty("状态")
    private String status;

    @ApiModelProperty("列表标题")
    private String title;

    @ApiModelProperty("主资源类型")
    private String primaryResourceType;

    @ApiModelProperty("主资源 ID")
    private String primaryResourceId;

    @ApiModelProperty("批量批次 ID")
    private String batchId;

    @ApiModelProperty("受理调用 ID")
    private String invocationId;

    @ApiModelProperty("失败摘要")
    private String errorMessage;

    @ApiModelProperty("开始时间")
    private Date startedAt;

    @ApiModelProperty("结束时间")
    private Date finishedAt;
}
