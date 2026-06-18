package com.vtc.openapi.domain.task.model.command;

import com.vtc.openapi.domain.task.model.vo.ScanTaskTargets;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class CreateOpenTaskCommand {

    private String extTaskId;

    private String taskName;

    /** 任务类型 1/2/3，见附录 F */
    private Integer type;

    private ScanTaskTargets targets;

    private String callbackUrl;

    private Integer scanTemplateId;

    private Integer reportTemplateId;

    private String priority;

    private Integer srcMethod;

    private List<String> vulIDs;

    private List<String> secResourceHashes;

    /** file 模式原始 XML */
    private String fileXml;

    private Map<String, Object> options;

    /** 默认 true：排查完成后自动触发验证阶段交叉扫描 */
    private Boolean autoVerify;
}
