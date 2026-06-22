package com.vtc.openapi.infra.adapter.taskcenter;

/** VTC 扫描结果落库结果（供 Kafka 回收与轮询重试决策）。 */
enum SurveyPersistOutcome {
    /** 已落库至少一条扫描结果 */
    PERSISTED,
    /** 空结果且仍在等待 VTC 入库，应延迟重试、勿推进任务 FINISHED */
    DEFERRED_VTC_LAG,
    /** 空结果但已放弃重试或判定为真实空扫 */
    EMPTY_ACCEPTED
}
