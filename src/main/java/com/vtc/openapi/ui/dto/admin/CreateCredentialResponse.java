package com.vtc.openapi.ui.dto.admin;

import lombok.Data;

@Data
public class CreateCredentialResponse {

    private String partnerId;
    private String clientId;
    /** 明文仅本次响应返回，禁止落日志 */
    private String clientSecret;
    private String status;
}
