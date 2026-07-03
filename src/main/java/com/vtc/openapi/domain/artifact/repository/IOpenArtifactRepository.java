package com.vtc.openapi.domain.artifact.repository;

import com.botany.spore.core.page.PageInfo;
import com.vtc.openapi.domain.artifact.model.entity.OpenArtifactDO;

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

    List<OpenArtifactDO> listPendingWebhookDelivery(String partnerId, String taskId, String exportStage,
                                                    String verifyFixJobId, int limit);

    List<OpenArtifactDO> listAllPendingWebhookDelivery(int limit);
}
