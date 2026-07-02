package com.vtc.openapi.ui.admin;

import com.vtc.openapi.domain.artifact.model.entity.OpenArtifactDO;
import com.vtc.openapi.domain.artifact.repository.IOpenArtifactRepository;
import com.vtc.openapi.domain.artifact.model.entity.OpenArtifactDO;
import com.vtc.openapi.domain.artifact.repository.IOpenArtifactRepository;
import com.vtc.openapi.domain.export.model.entity.OpenExportDO;
import com.vtc.openapi.domain.export.repository.IOpenExportRepository;
import com.vtc.openapi.domain.export.service.business.IExportDownloadPolicy;
import com.vtc.openapi.ui.dto.admin.WebhookEventDetailDto;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Webhook 事件业务详情查询接口。
 * <p>
 * 通过 event_id 关联业务侧（open_export / open_artifact）的业务详情，
 * 供前端推送记录 tab 获取下载 URL 和 downloadable 标志。
 * <p>
 * 与 platform-admin /push-records（投递元数据）配合使用，前端按 event_id 合并。
 *
 * @author asset-security
 */
@RestController
@RequestMapping("/internal/admin")
@Api(tags = "Webhook 事件业务详情")
public class WebhookEventDetailUi {

    private static final String STATUS_READY = "READY";

    private final IOpenExportRepository exportRepository;
    private final IOpenArtifactRepository artifactRepository;
    private final IExportDownloadPolicy exportDownloadPolicy;

    public WebhookEventDetailUi(IOpenExportRepository exportRepository,
                                IOpenArtifactRepository artifactRepository,
                                IExportDownloadPolicy exportDownloadPolicy) {
        this.exportRepository = exportRepository;
        this.artifactRepository = artifactRepository;
        this.exportDownloadPolicy = exportDownloadPolicy;
    }

    @ApiOperation("按事件ID批量查询 Webhook 事件业务详情")
    @GetMapping("/webhook-event-details")
    public List<WebhookEventDetailDto> getEventDetails(@RequestParam List<String> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 1. 批量查询 export 和 artifact
        List<OpenExportDO> exports = exportRepository.listByWebhookEventIds(eventIds);
        List<OpenArtifactDO> artifacts = artifactRepository.listByWebhookEventIds(eventIds);

        // 2. 按 eventId 建立映射
        Map<String, OpenExportDO> exportByEventId = new LinkedHashMap<>();
        for (OpenExportDO e : exports) {
            if (e.getWebhookEventId() != null) {
                exportByEventId.put(e.getWebhookEventId(), e);
            }
        }
        Map<String, OpenArtifactDO> artifactByEventId = new LinkedHashMap<>();
        for (OpenArtifactDO a : artifacts) {
            if (a.getWebhookEventId() != null) {
                artifactByEventId.put(a.getWebhookEventId(), a);
            }
        }

        // 3. 按请求的 eventIds 顺序构建响应
        List<WebhookEventDetailDto> result = new ArrayList<>();
        for (String eventId : eventIds) {
            OpenExportDO export = exportByEventId.get(eventId);
            OpenArtifactDO artifact = artifactByEventId.get(eventId);
            if (export == null && artifact == null) {
                continue; // 非 EXPORT_READY/ARTIFACT_READY 事件，不返回业务详情
            }

            WebhookEventDetailDto dto = new WebhookEventDetailDto();
            dto.setEventId(eventId);

            if (export != null) {
                dto.setEventType("EXPORT_READY");
                dto.setPartnerId(export.getPartnerId());
                dto.setExportId(export.getExportId());
                dto.setExportFormat(export.getFormat());
                dto.setExportStage(export.getExportStage());
                dto.setDownloadUrl(export.getDownloadUrl());
                dto.setDownloadable(exportDownloadPolicy.isStageDownloadable(
                        export.getPartnerId(), export.getExportStage()));
                dto.setSummary(buildExportSummary(export));
            } else if (artifact != null) {
                dto.setEventType("ARTIFACT_READY");
                dto.setPartnerId(artifact.getPartnerId());
                dto.setArtifactId(artifact.getArtifactId());
                dto.setArtifactFormat(artifact.getFileFormat());
                dto.setExportStage(artifact.getExportStage());
                dto.setDownloadUrl(artifact.getDownloadUrl());
                dto.setDownloadable(exportDownloadPolicy.isStageDownloadable(
                        artifact.getPartnerId(), artifact.getExportStage()));
                dto.setSummary(buildArtifactSummary(artifact));
            }

            result.add(dto);
        }

        return result;
    }

    private String buildExportSummary(OpenExportDO export) {
        return String.format("外发 %s (%s)", export.getExportStage(), export.getFormat());
    }

    private String buildArtifactSummary(OpenArtifactDO artifact) {
        return String.format("产物 %s (%s)", artifact.getFileName(), artifact.getFileFormat());
    }
}