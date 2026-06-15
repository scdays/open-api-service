package com.vtc.openapi.domain.task.model.result;

import com.vtc.openapi.domain.task.model.vo.ScanTaskTargets;
import lombok.Data;

/** {@code POST /tasks/file} 解析 XML 后的任务创建参数。 */
@Data
public class ParsedScanTaskFileResult {

    private String taskName;
    private ScanTaskTargets targets;
    private String priority;
    private String callbackUrl;
    private Integer scanTemplateId;
    private Integer reportTemplateId;
    /** 原始 XML，供引擎适配器/审计使用 */
    private String fileXml;
}
