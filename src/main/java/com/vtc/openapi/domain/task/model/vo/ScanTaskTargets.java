package com.vtc.openapi.domain.task.model.vo;

import lombok.Data;

import java.util.List;

@Data
public class ScanTaskTargets {

    private String hosts;

    private List<ScanTargetAuthEntry> auth;
}
