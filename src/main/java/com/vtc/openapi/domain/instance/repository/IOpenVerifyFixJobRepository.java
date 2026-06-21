package com.vtc.openapi.domain.instance.repository;

import com.vtc.openapi.domain.instance.model.entity.OpenVerifyFixJobDO;
import com.vtc.openapi.domain.instance.model.entity.OpenVerifyFixJobItemDO;

import java.util.List;

public interface IOpenVerifyFixJobRepository {

    /** 自动重试下发次数上限：达到后不再由定时任务自动重试，保留 DISPATCH_FAILED 转人工（手动重试不受限）。 */
    int MAX_AUTO_DISPATCH_RETRY = 5;

    void saveJob(OpenVerifyFixJobDO job);

    void updateJob(OpenVerifyFixJobDO job);

    OpenVerifyFixJobDO findByJobId(String jobId);

    List<OpenVerifyFixJobDO> listByPartner(String partnerId, String status, int limit);

    List<OpenVerifyFixJobDO> listRecent(String status, int limit);

    List<OpenVerifyFixJobDO> listForAdmin(String partnerId, String status, String jobId, int limit);

    void saveItems(List<OpenVerifyFixJobItemDO> items);

    void updateItem(OpenVerifyFixJobItemDO item);

    List<OpenVerifyFixJobItemDO> listItemsByJobId(String jobId);

    OpenVerifyFixJobItemDO findLatestPendingItemByPartnerAndVulInfoId(String partnerId, String vulInfoId);

    List<OpenVerifyFixJobDO> listActiveVtcJobs(int limit);

    List<OpenVerifyFixJobDO> listDispatchFailedJobs(int limit);

    OpenVerifyFixJobDO findByCenterSubId(String centerSubId);

    void updateCaseId(String jobId, String caseId);

    List<OpenVerifyFixJobDO> listWithoutCaseId(int limit);
}
