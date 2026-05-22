package com.vtc.openapi.ui.dto.admin;

import lombok.Data;

import java.util.List;

@Data
public class PartnerDetailDto {

    private String partnerId;

    private String partnerName;

    private String partnerType;

    private String status;

    private List<String> capabilities;

    private String defaultCallbackUrl;

    private Integer rateLimitQps;
}
