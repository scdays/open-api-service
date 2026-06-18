package com.vtc.openapi.ui.dto.admin;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@ApiModel("OpenTaskSurveyResultsDto")
public class OpenTaskSurveyResultsDto {

    @ApiModelProperty("任务 ID")
    private String taskId;

    @ApiModelProperty("扫描阶段 1=排查 2=验证")
    private Integer scanPhase;

    @ApiModelProperty("子任务 ID")
    private String subId;

    @ApiModelProperty("surveyId")
    private String surveyId;

    @ApiModelProperty("扫描器")
    private String scannerLabel;

    @ApiModelProperty("数据来源 task-center/mock/unavailable")
    private String source;

    @ApiModelProperty("存活 IP 成功集合")
    private List<String> successIps;

    @ApiModelProperty("存活 IP 失败集合")
    private List<String> failIps;

    @ApiModelProperty("存活探测明细（§5.6.5 liveProbeResults 行）")
    private List<Map<String, Object>> liveProbeResults;

    @ApiModelProperty("端口扫描结果")
    private List<Map<String, Object>> portScanResults;

    @ApiModelProperty("漏洞扫描结果")
    private List<Map<String, Object>> vulnerabilities;

    @ApiModelProperty("漏洞库详情")
    private List<Map<String, Object>> vulnDatabaseList;

    @ApiModelProperty("提示信息")
    private String hint;
}
