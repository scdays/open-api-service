package com.vtc.openapi.infra.adapter.taskcenter;

import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 验证阶段 vulInfoStat 解析：UNION（SOC）与 INTERSECT（部侧）。
 */
@Component
public class TaskCenterVerifyStatusResolver {

    public static final String STRATEGY_UNION = "UNION";
    public static final String STRATEGY_INTERSECT = "INTERSECT";

    private final TaskCenterVerifyMergeService mergeService;

    public TaskCenterVerifyStatusResolver(TaskCenterVerifyMergeService mergeService) {
        this.mergeService = mergeService;
    }

    /**
     * 按合并策略与扫描器命中数确定验证后状态。
     *
     * <ul>
     *   <li>UNION：双扫均命中→2，单侧命中→1，均未命中→3</li>
     *   <li>INTERSECT：双扫均命中→2，否则→3</li>
     * </ul>
     */
    public int resolveVerifyStat(int scannerHits, int totalScanners, String strategy) {
        if (totalScanners <= 1) {
            return scannerHits > 0 ? 1 : 3;
        }
        if (STRATEGY_UNION.equalsIgnoreCase(strategy)) {
            if (scannerHits >= totalScanners) {
                return 2;
            }
            if (scannerHits >= 1) {
                return 1;
            }
            return 3;
        }
        if (scannerHits >= totalScanners) {
            return 2;
        }
        return 3;
    }

    public List<JSONObject> resolveUnionVerifyStats(List<JSONObject> mergedResults,
                                                    Map<String, Integer> scannerHitCount,
                                                    int totalScanners) {
        List<JSONObject> list = new ArrayList<>();
        for (JSONObject raw : mergedResults) {
            JSONObject inst = JSONObject.parseObject(raw.toJSONString());
            String key = mergeService.dedupKey(raw);
            int hits = scannerHitCount.getOrDefault(key, 1);
            inst.put("vulInfoStat", resolveVerifyStat(hits, totalScanners, STRATEGY_UNION));
            list.add(inst);
        }
        return list;
    }

    /** @deprecated 使用 {@link #resolveUnionVerifyStats} */
    @Deprecated
    public List<JSONObject> resolveVerifyStats(List<JSONObject> mergedResults,
                                               Map<String, Integer> scannerHitCount,
                                               int totalScanners) {
        return resolveUnionVerifyStats(mergedResults, scannerHitCount, totalScanners);
    }
}
