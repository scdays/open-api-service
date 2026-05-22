package com.vtc.openapi.ui.dto.auth;

import lombok.Data;

import java.util.List;

@Data
public class PartnerTokenIntrospectResponse {

    private String subjectType;

    private String partnerId;

    private List<String> capabilities;

    private String clientId;

    private Long issuedAt;

    private Long expiresAt;
}
