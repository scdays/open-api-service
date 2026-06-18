package com.vtc.openapi.ui.dto.admin;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("OpenTaskAdminDto")
public class OpenTaskAdminDto {

    @ApiModelProperty("平台任务 ID")
    private String taskId;

    @ApiModelProperty("接入方任务 ID")
    private String extTaskId;

    @ApiModelProperty("接入方 ID")
    private String partnerId;

    @ApiModelProperty("任务名称")
    private String taskName;

    @ApiModelProperty("任务类型 1/2/3")
    private Integer vulnType;

    @ApiModelProperty("扫描模板 1001/1002/1003")
    private Integer scanTemplateId;

    @ApiModelProperty("是否 autoVerify")
    private Boolean autoVerify;

    @ApiModelProperty("是否交叉扫描")
    private Boolean crossScan;

    @ApiModelProperty("合并策略 UNION/INTERSECT")
    private String verifyMergeStrategy;

    @ApiModelProperty("编排阶段 1=排查 2=验证")
    private Integer taskPhase;

    @ApiModelProperty("任务状态")
    private String status;

    @ApiModelProperty("进度 0-100")
    private Integer progress;

    @ApiModelProperty("引擎适配模式 mock/task-center/vul-pass")
    private String adapterMode;

    @ApiModelProperty("实例数量")
    private Long instanceCount;

    @ApiModelProperty("子任务数量")
    private Integer subTaskCount;

    @ApiModelProperty("创建时间")
    private String createdAt;

    @ApiModelProperty("开始时间")
    private String startedAt;

    @ApiModelProperty("完成时间")
    private String finishedAt;
}
