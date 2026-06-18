package com.vtc.openapi.domain.task.repository;

import com.botany.spore.ddd.domain.repository.IDatabaseRepository;
import com.vtc.openapi.domain.task.model.entity.OpenTaskSubDO;

import java.util.List;

public interface IOpenTaskSubRepository extends IDatabaseRepository<OpenTaskSubDO> {

    List<OpenTaskSubDO> listByTaskId(String taskId);

    List<OpenTaskSubDO> listByTaskIdAndPhase(String taskId, int scanPhase);

    List<OpenTaskSubDO> listRunning();

    OpenTaskSubDO findBySubId(String subId);

    List<OpenTaskSubDO> listByVerifyFixJobId(String verifyFixJobId);

    List<OpenTaskSubDO> listRunningVerifyFixSubs(int limit);

    /** FINISHED 且漏洞类子任务尚未归档原始报告（等待 SFTP 路径或重试上传） */
    List<OpenTaskSubDO> listFinishedAwaitingReportArchive(int limit);

    void saveSub(OpenTaskSubDO row);

    void updateSub(OpenTaskSubDO row);
}
