package com.vtc.openapi.infra.adapter.taskcenter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * scanTemplateId → scannerType 下发策略。
 * <ul>
 *   <li>1001：交叉扫描 scannerType 1（绿盟）+ 7（Nessus）</li>
 *   <li>1002/1003：单扫描器 scannerType=1</li>
 * </ul>
 */
public final class TaskCenterScannerPlanner {

    public static final String SCANNER_LM = "1";
    public static final String SCANNER_NESSUS = "7";

    private TaskCenterScannerPlanner() {
    }

    public static boolean isCrossScan(Integer scanTemplateId) {
        return scanTemplateId != null && scanTemplateId == 1001;
    }

    public static List<String> resolveScannerTypes(Integer scanTemplateId) {
        if (isCrossScan(scanTemplateId)) {
            return Arrays.asList(SCANNER_LM, SCANNER_NESSUS);
        }
        return Collections.singletonList(SCANNER_LM);
    }

    public static String resolveVerifyMergeStrategy(Integer scanTemplateId) {
        return isCrossScan(scanTemplateId) ? "UNION" : "INTERSECT";
    }
}
