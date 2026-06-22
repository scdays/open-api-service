package com.vtc.openapi.infra.adapter.taskcenter;

import com.vtc.openapi.infra.feign.dto.taskcenter.TaskCenterSurveyBundle;
import org.springframework.util.CollectionUtils;

/**
 * 判断 VTC Feign 返回是否「接口成功但数据仍为空」（常见于 task_finish 早于 VTC 入库）。
 */
final class TaskCenterSurveyBundleSupport {

    private TaskCenterSurveyBundleSupport() {
    }

    static boolean isEmptyBundle(TaskCenterSurveyBundle bundle) {
        if (bundle == null) {
            return true;
        }
        return CollectionUtils.isEmpty(bundle.getVulnScanResultList())
                && CollectionUtils.isEmpty(bundle.getVulnDatabaseList())
                && CollectionUtils.isEmpty(bundle.getPortScanRows())
                && CollectionUtils.isEmpty(bundle.getSuccessIps())
                && CollectionUtils.isEmpty(bundle.getFailIps());
    }

    /**
     * Feign 调用均未抛错、但结果集全空，高概率为 VTC 侧入库滞后。
     */
    static boolean isLikelyVtcLag(TaskCenterSurveyBundle bundle) {
        if (bundle == null) {
            return true;
        }
        if (!isEmptyBundle(bundle)) {
            return false;
        }
        return bundle.isSuccessIpsQueryOk()
                && bundle.isFailIpsQueryOk()
                && bundle.isPortScanQueryOk();
    }
}
