package com.vtc.openapi.domain.instance.repository;

import com.vtc.openapi.domain.instance.model.entity.OpenVerifyFixJobDO;
import com.vtc.openapi.domain.instance.model.entity.OpenVerifyFixJobItemDO;

import java.util.List;

public interface IOpenVerifyFixJobRepository {

    void saveJob(OpenVerifyFixJobDO job);

    void updateJob(OpenVerifyFixJobDO job);

    OpenVerifyFixJobDO findByJobId(String jobId);

    List<OpenVerifyFixJobDO> listByPartner(String partnerId, String status, int limit);

    List<OpenVerifyFixJobDO> listRecent(String status, int limit);

    void saveItems(List<OpenVerifyFixJobItemDO> items);

    void updateItem(OpenVerifyFixJobItemDO item);

    List<OpenVerifyFixJobItemDO> listItemsByJobId(String jobId);

    OpenVerifyFixJobItemDO findLatestPendingItemByPartnerAndVulInfoId(String partnerId, String vulInfoId);

    List<OpenVerifyFixJobDO> listActiveVtcJobs(int limit);

    List<OpenVerifyFixJobDO> listDispatchFailedJobs(int limit);

    OpenVerifyFixJobDO findByCenterSubId(String centerSubId);
}
