package com.vtc.openapi.domain.instance.model.result;

import lombok.Data;

@Data
public class VerifyFixSubmitResult {

    private String vulInfoId;
    private Integer previousStat;
    private Integer currentStat;
    private String verifyFixJobId;
    private String verifyFixStatus;
}
