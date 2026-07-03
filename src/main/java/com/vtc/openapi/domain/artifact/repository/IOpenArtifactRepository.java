package com.vtc.openapi.domain.artifact.repository;

import com.botany.spore.core.page.PageInfo;
import com.vtc.openapi.domain.artifact.model.entity.OpenArtifactDO;

import java.util.Collection;
import java.util.List;

public interface IOpenArtifactRepository {

    OpenArtifactDO findByArtifactId(String artifactId);

    OpenArtifactDO findByPartnerAndArtifactId(String partnerId, String artifactId);

    OpenArtifactDO findBySubTaskAndSource(String partnerId, String subTaskId, String artifactSource);

    void saveArtifact(OpenArtifactDO artifact);

    void updateArtifact(OpenArtifactDO artifact);

    PageInfo<OpenArtifactDO> pageByTask(String partnerId, String taskId, String exportStage,
                                        String artifactSource, int page, int size);

    PageInfo<OpenArtifactDO> pageByTaskAndStage(String partnerId, String taskId, String exportStage,
                                                int page, int size);

    /**
     * 按 webhook_event_id 批量查询产物记录（推送记录业务详情用）。
     */
    List<OpenArtifactDO> listByWebhookEventIds(Collection<String> eventIds);

    List<OpenArtifactDO> listPendingWebhookDelivery(String partnerId, String taskId, String exportStage,
                                                    String verifyFixJobId, int limit);

    List<OpenArtifactDO> listAllPendingWebhookDelivery(int limit);
}
