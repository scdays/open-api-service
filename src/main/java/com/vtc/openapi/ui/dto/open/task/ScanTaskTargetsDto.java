package com.vtc.openapi.ui.dto.open.task;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.List;

@Data
public class ScanTaskTargetsDto {

    @NotBlank(message = "targets.hosts 不能为空")
    private String hosts;

    private List<ScanTargetAuthEntryDto> auth;
}
