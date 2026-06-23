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
        return isEmptyBundle(bundle, ScanTemplateSurveyScope.full());
    }

    static boolean isEmptyBundle(TaskCenterSurveyBundle bundle, ScanTemplateSurveyScope scope) {
        if (bundle == null) {
            return true;
        }
        ScanTemplateSurveyScope effective = scope != null ? scope : ScanTemplateSurveyScope.full();
        boolean empty = true;
        if (effective.needsVulnScan()) {
            empty &= CollectionUtils.isEmpty(bundle.getVulnScanResultList())
                    && CollectionUtils.isEmpty(bundle.getVulnDatabaseList());
        }
        if (effective.needsPortScan()) {
            empty &= CollectionUtils.isEmpty(bundle.getPortScanRows());
        }
        if (effective.needsAliveProbe()) {
            empty &= CollectionUtils.isEmpty(bundle.getSuccessIps())
                    && CollectionUtils.isEmpty(bundle.getFailIps());
        }
        return empty;
    }

    /**
     * Feign 调用均未抛错、但结果集全空，高概率为 VTC 侧入库滞后。
     */
    static boolean isLikelyVtcLag(TaskCenterSurveyBundle bundle) {
        return isLikelyVtcLag(bundle, ScanTemplateSurveyScope.full());
    }

    static boolean isLikelyVtcLag(TaskCenterSurveyBundle bundle, ScanTemplateSurveyScope scope) {
        if (bundle == null) {
            return true;
        }
        ScanTemplateSurveyScope effective = scope != null ? scope : ScanTemplateSurveyScope.full();
        if (!isEmptyBundle(bundle, effective)) {
            return false;
        }
        boolean queriesOk = true;
        if (effective.needsAliveProbe()) {
            queriesOk &= bundle.isSuccessIpsQueryOk() && bundle.isFailIpsQueryOk();
        }
        if (effective.needsPortScan()) {
            queriesOk &= bundle.isPortScanQueryOk();
        }
        return queriesOk;
    }
}
