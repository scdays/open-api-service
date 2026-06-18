package com.vtc.openapi.domain.task.model.entity;

import com.botany.spore.ddd.domain.model.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
public class OpenTaskSubDO extends BaseDO {

    private Long id;
    private String subId;
    private String taskId;
    private String partnerId;
    /** 1=排查 2=验证 */
    private Integer scanPhase;
    private String scannerType;
    /** vuln / port / alive */
    private String centerTaskType;
    private String centerPlanId;
    private String surveyId;
    private String status;
    private Integer progress;
    private String errorMessage;
    /** VTC 报告 FTP 下载路径（download_report_finish_topic） */
    private String reportDownloadPath;
    private Date createdAt;
    private Date updatedAt;
}
