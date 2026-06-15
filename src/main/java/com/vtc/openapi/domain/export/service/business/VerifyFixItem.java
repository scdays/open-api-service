package com.vtc.openapi.domain.export.service.business;

import lombok.Data;

@Data
public class VerifyFixItem {
    private String vulInfoId;
    private Integer vulInfoStat;
    private Integer previousVulInfoStat;
}
