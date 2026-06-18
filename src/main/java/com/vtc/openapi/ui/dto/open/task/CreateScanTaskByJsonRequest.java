package com.vtc.openapi.ui.dto.open.task;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 对齐 OpenAPI {@code CreateScanTaskByJsonRequest}（§5.1.2 POST /tasks/vul）。
 */
@Data
public class CreateScanTaskByJsonRequest {

    @NotBlank(message = "extTaskId 不能为空")
    @Size(max = 128, message = "extTaskId 长度不能超过 128")
    private String extTaskId;

    @NotBlank(message = "taskName 不能为空")
    @Size(max = 256, message = "taskName 长度不能超过 256")
    private String taskName;

    @NotNull(message = "type 不能为空")
    private Integer type;

    @NotNull(message = "targets 不能为空")
    @Valid
    private ScanTaskTargetsDto targets;

    private Integer scanTemplateId;

    private Integer reportTemplateId;

    private Integer srcMethod;

    private List<String> vulIDs;

    private List<String> secResourceHashes;

    private String callbackUrl;

    private String priority;

    /** 默认 true，见 API §1.3 autoVerify */
    private Boolean autoVerify;
}
