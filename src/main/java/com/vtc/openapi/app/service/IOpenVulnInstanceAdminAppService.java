package com.vtc.openapi.app.service;

import com.vtc.openapi.ui.dto.ApiResponse;
import com.vtc.openapi.ui.dto.admin.OpenVulnInstanceStateLogDto;

import java.util.List;

public interface IOpenVulnInstanceAdminAppService {

    ApiResponse<List<OpenVulnInstanceStateLogDto>> listStateLogs(String partnerId, String vulInfoId, int limit);
}
