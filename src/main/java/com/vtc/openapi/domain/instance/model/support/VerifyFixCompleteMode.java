package com.vtc.openapi.domain.instance.model.support;

/**
 * Mock / 内部运营完成修复核验的方式。
 */
public enum VerifyFixCompleteMode {

    /** 按已导入或任务 bundle 复扫报告指纹比对 */
    COMPARE_RESCAN,
    /** 一键：全部核验修复（6） */
    ALL_FIXED,
    /** 一键：全部核验未修复（7） */
    ALL_UNFIXED
}
