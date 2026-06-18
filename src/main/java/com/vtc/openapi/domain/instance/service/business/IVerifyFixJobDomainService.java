package com.vtc.openapi.domain.instance.service.business;

import com.vtc.openapi.domain.instance.model.command.VerifyFixInstanceCommand;
import com.vtc.openapi.domain.instance.model.entity.OpenVerifyFixJobDO;
import com.vtc.openapi.domain.instance.model.entity.OpenVerifyFixJobItemDO;
import com.vtc.openapi.domain.instance.model.result.InstanceStateResult;
import com.vtc.openapi.domain.instance.model.support.VerifyFixCompleteMode;

import java.util.List;

public interface IVerifyFixJobDomainService {

    String STATUS_PENDING = "PENDING";
    String STATUS_RUNNING = "RUNNING";
    String STATUS_FINISHED = "FINISHED";
    String STATUS_FAILED = "FAILED";
    /** VTC 下发失败，可重试 */
    String STATUS_DISPATCH_FAILED = "DISPATCH_FAILED";

    String ITEM_PENDING = "PENDING";
    String ITEM_DONE = "DONE";
    String ITEM_FAILED = "FAILED";

    /**
     * 受理单条修复核验（异步，实例保持 stat=5）。
     */
    InstanceStateResult accept(String partnerId, VerifyFixInstanceCommand command, String batchId);

    /**
     * 批量受理：共享一个 verifyFixJobId。
     */
    List<InstanceStateResult> acceptBatch(String partnerId, String batchId,
                                          List<VerifyFixInstanceCommand> commands);

    OpenVerifyFixJobDO requireJob(String jobId);

    List<OpenVerifyFixJobItemDO> listJobItems(String jobId);

    /**
     * 完成修复核验：更新实例状态并仅推送 Webhook（不外发）。
     */
    void completeJob(String jobId, VerifyFixCompleteMode mode);

    /**
     * 导入复扫 XML 后按报告自动比对并完成。
     */
    void importRescanXmlAndComplete(String jobId, byte[] xmlBytes);

    /**
     * task-center 复扫结果指纹比对完成（VTC 回收后调用，全 job 一次比对）。
     */
    void completeFromRescanCompare(String jobId, java.util.Set<String> rescanFingerprintKeys);

    /**
     * 单条 open_task_sub(phase=3) 回收后，仅比对归属该 sub 的实例。
     */
    void completeFromRescanCompareForSub(String jobId, String rescanSubId,
                                         java.util.Set<String> rescanFingerprintKeys);

    /**
     * vul-pass 内部回调：更新状态并仅推送 Webhook。
     */
    void completeFromInternalNotify(String jobId, String vulInfoId, Integer resultStat,
                                    String batchId, boolean jobFailed);

    /**
     * SOC / 运营：基于离线导入任务创建平台内部修复核验任务（不经 Partner API 建扫任务）。
     */
    String createInternalFromOfflineTask(String partnerId, String taskId,
                                         List<String> vulInfoIds, String batchId);

    /**
     * 运营：基于 Partner 调用记录所选 vulInfoID 归入已有 PENDING 作业或新建合并作业。
     */
    String createJobFromSelection(String partnerId, List<String> vulInfoIds, String batchId);

    List<OpenVerifyFixJobDO> listRecentJobs(String partnerId, String status, int limit);
}
