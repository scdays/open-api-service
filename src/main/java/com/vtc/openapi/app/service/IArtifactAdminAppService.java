package com.vtc.openapi.app.service;

import org.springframework.http.ResponseEntity;

public interface IArtifactAdminAppService {

    ResponseEntity<byte[]> downloadArtifact(String partnerId, String artifactId);

    ResponseEntity<byte[]> downloadArtifactByEventId(String partnerId, String eventId);
}
