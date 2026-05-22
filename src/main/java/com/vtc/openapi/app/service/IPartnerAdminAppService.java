package com.vtc.openapi.app.service;

import com.vtc.openapi.ui.dto.admin.CreateCredentialResponse;
import com.vtc.openapi.ui.dto.admin.CreatePartnerRequest;
import com.vtc.openapi.ui.dto.admin.PartnerDetailDto;
import com.vtc.openapi.ui.dto.admin.PartnerSummaryDto;
import com.vtc.openapi.ui.dto.admin.UpdatePartnerRequest;
import com.vtc.openapi.web.dto.ApiResponse;

import java.util.List;

/**
 * Partner 内部管理应用服务（运营后台 · P0 骨架）。
 */
public interface IPartnerAdminAppService {

    ApiResponse<PartnerDetailDto> createPartner(CreatePartnerRequest request);

    ApiResponse<List<PartnerSummaryDto>> listPartners(int page, int size);

    ApiResponse<PartnerDetailDto> getPartner(String partnerId);

    ApiResponse<PartnerDetailDto> updatePartner(String partnerId, UpdatePartnerRequest request);

    ApiResponse<CreateCredentialResponse> createCredential(String partnerId);
}
