package com.vtc.openapi.ui.dto.admin;

import lombok.Data;

import java.util.List;

@Data
public class UpdatePartnerRequest {

    private String partnerName;

    private String status;

    private List<String> capabilities;

    private String defaultCallbackUrl;

    private Integer rateLimitQps;
}
