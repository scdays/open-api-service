package com.vtc.openapi.ui.dto.open.task;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 对齐 OpenAPI {@code CreateScanTaskByFileRequest}（§5.1.1 POST /tasks/file）。
 */
@Data
public class CreateScanTaskByFileRequest {

    @NotBlank(message = "extTaskId 不能为空")
    @Size(max = 128, message = "extTaskId 长度不能超过 128")
    private String extTaskId;

    @NotNull(message = "type 不能为空")
    private Integer type;

    @NotBlank(message = "file 不能为空")
    private String file;
}
