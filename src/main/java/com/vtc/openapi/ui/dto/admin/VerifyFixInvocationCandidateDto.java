package com.vtc.openapi.ui.dto.admin;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("修复核验调用候选实例（来自 Partner 调用记录）")
public class VerifyFixInvocationCandidateDto {

    @ApiModelProperty("系统漏洞实例 ID")
    private String vulInfoId;

    @ApiModelProperty("所属平台 taskId")
    private String taskId;

    @ApiModelProperty("当前 vulInfoStat")
    private Integer vulInfoStat;

    @ApiModelProperty("受理返回的 verifyFixJobId（可能为空）")
    private String verifyFixJobId;

    @ApiModelProperty("关联作业状态")
    private String jobStatus;

    @ApiModelProperty("最近调用 invocationId")
    private String invocationId;

    @ApiModelProperty("operationId")
    private String operationId;

    @ApiModelProperty("调用时间 ISO-UTC")
    private String invokedAt;
}
