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

    /** FINISHED 且尚无落库扫描结果、任务仍在 RUNNING 的子任务（等待 VTC 入库重试） */
    List<OpenTaskSubDO> listFinishedAwaitingSurveyCapture(int limit);

    void saveSub(OpenTaskSubDO row);

    void updateSub(OpenTaskSubDO row);
}
