package com.vtc.openapi.ui.dto.admin;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("运营案件历史回填结果")
public class OperationCaseBackfillResultDto {

    @ApiModelProperty("是否 dry-run")
    private boolean dryRun;

    @ApiModelProperty("处理上限")
    private int limit;

    @ApiModelProperty("新建案件数")
    private int casesCreated;

    @ApiModelProperty("回填 invocation.case_id 数")
    private int invocationsLinked;

    @ApiModelProperty("回填 open_task.case_id 数")
    private int tasksLinked;

    @ApiModelProperty("回填 open_verify_fix_job.case_id 数")
    private int jobsLinked;
}
