package com.vtc.openapi.ui.auth;

import com.vtc.openapi.app.service.IPartnerTokenAppService;
import com.vtc.openapi.ui.dto.auth.PartnerTokenIntrospectRequest;
import com.vtc.openapi.ui.dto.auth.PartnerTokenIntrospectResponse;
import com.vtc.openapi.ui.dto.auth.PartnerTokenIssueRequest;
import com.vtc.openapi.ui.dto.auth.PartnerTokenIssueResponse;
import com.vtc.openapi.ui.dto.ApiResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * Partner Token 签发与 introspect。
 * <ul>
 *   <li>{@code POST /oauth/token} — 公网（经 partner-gateway 白名单）</li>
 *   <li>{@code POST /internal/token/introspect} — partner-gateway 降级 introspect</li>
 * </ul>
 */
@RestController
@Validated
@Api(tags = "Partner Token")
public class PartnerTokenUI {

    private final IPartnerTokenAppService partnerTokenAppService;

    public PartnerTokenUI(IPartnerTokenAppService partnerTokenAppService) {
        this.partnerTokenAppService = partnerTokenAppService;
    }

    @ApiOperation(value = "OAuth2 client_credentials 换 Token", notes = "路径：/oauth/token 或 /api/open/v1/oauth/token")
    @PostMapping({"/oauth/token", "/api/open/v1/oauth/token"})
    public ApiResponse<PartnerTokenIssueResponse> issueToken(@Valid @RequestBody PartnerTokenIssueRequest request) {
        return partnerTokenAppService.issueToken(request);
    }

    @ApiOperation(value = "Token introspect", notes = "供 partner-gateway Feign 降级；入参 token")
    @PostMapping("/internal/token/introspect")
    public ApiResponse<PartnerTokenIntrospectResponse> introspect(@Valid @RequestBody PartnerTokenIntrospectRequest request) {
        return partnerTokenAppService.introspect(request);
    }
}
