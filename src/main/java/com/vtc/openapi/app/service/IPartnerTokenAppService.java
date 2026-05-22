package com.vtc.openapi.app.service;

import com.vtc.openapi.ui.dto.auth.PartnerTokenIntrospectRequest;
import com.vtc.openapi.ui.dto.auth.PartnerTokenIntrospectResponse;
import com.vtc.openapi.ui.dto.auth.PartnerTokenIssueRequest;
import com.vtc.openapi.ui.dto.auth.PartnerTokenIssueResponse;
import com.vtc.openapi.web.dto.ApiResponse;

/**
 * Partner Token 签发与 introspect（P0）。
 */
public interface IPartnerTokenAppService {

    ApiResponse<PartnerTokenIssueResponse> issueToken(PartnerTokenIssueRequest request);

    ApiResponse<PartnerTokenIntrospectResponse> introspect(PartnerTokenIntrospectRequest request);
}
