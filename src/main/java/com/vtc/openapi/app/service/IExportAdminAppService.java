package com.vtc.openapi.app.service;

import org.springframework.http.ResponseEntity;

public interface IExportAdminAppService {

    ResponseEntity<byte[]> downloadExport(String partnerId, String exportId);
}
