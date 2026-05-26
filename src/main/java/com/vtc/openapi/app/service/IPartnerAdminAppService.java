package com.vtc.openapi.app.service;

import com.botany.spore.core.page.PageInfo;
import com.botany.spore.ddd.app.service.IAppService;
import com.vtc.openapi.ui.dto.admin.PartnerCredentialDTO;
import com.vtc.openapi.ui.dto.admin.PartnerDTO;
import com.vtc.openapi.ui.dto.admin.PartnerPageDto;
import com.vtc.openapi.ui.dto.ApiResponse;
import com.vtc.openapi.ui.params.admin.CreatePartnerParams;
import com.vtc.openapi.ui.params.admin.UpdatePartnerParams;

import java.util.List;

/**
 * Partner 内部管理应用服务（运营后台 · P0）。
 */
public interface IPartnerAdminAppService extends IAppService<PartnerDTO> {

    ApiResponse<PartnerDTO> createPartner(CreatePartnerParams params);

    ApiResponse<PartnerPageDto> listPartners(PageInfo<PartnerDTO> pageInfo);

    ApiResponse<PartnerDTO> getPartner(String partnerId);

    ApiResponse<PartnerDTO> updatePartner(String partnerId, UpdatePartnerParams params);

    ApiResponse<PartnerCredentialDTO> createCredential(String partnerId);

    ApiResponse<List<PartnerCredentialDTO>> listCredentials(String partnerId);
}
