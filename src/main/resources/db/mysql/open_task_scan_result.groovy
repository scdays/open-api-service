package db.mysql

databaseChangeLog(logicalFilePath: 'open_task_scan_result.groovy') {
    changeSet(id: '2026-06-18-create-open_task_scan_result', author: 'open-api') {
        createTable(tableName: 'open_task_scan_result', remarks: 'VTC 扫描结果（存活/端口，对齐 TaskExport §5.6.5）') {
            column(name: 'id', type: 'BIGINT', autoIncrement: true) {
                constraints(primaryKey: true, primaryKeyName: 'pk_open_task_scan_result')
            }
            column(name: 'task_id', type: 'VARCHAR(64)', remarks: 'open_task.task_id') {
                constraints(nullable: false)
            }
            column(name: 'sub_id', type: 'VARCHAR(32)', remarks: 'open_task_sub.sub_id') {
                constraints(nullable: false)
            }
            column(name: 'partner_id', type: 'VARCHAR(64)', remarks: 'Partner ID') {
                constraints(nullable: false)
            }
            column(name: 'scan_phase', type: 'INT', remarks: '1=排查 2=验证') {
                constraints(nullable: false)
            }
            column(name: 'survey_id', type: 'VARCHAR(64)', remarks: 'VTC surveyId')
            column(name: 'scanner_type', type: 'VARCHAR(8)', remarks: '扫描器类型码')
            column(name: 'result_type', type: 'VARCHAR(16)', remarks: 'LIVE_PROBE / PORT_SCAN') {
                constraints(nullable: false)
            }
            column(name: 'result_key', type: 'VARCHAR(256)', remarks: '去重键 address 或 address|port|protocol') {
                constraints(nullable: false)
            }
            column(name: 'payload_json', type: 'TEXT', remarks: 'TaskExport 行 JSON（liveProbeResults/portScanResults 元素）') {
                constraints(nullable: false)
            }
            column(name: 'created_at', type: 'DATETIME', defaultValueComputed: 'CURRENT_TIMESTAMP') {
                constraints(nullable: false)
            }
            column(name: 'updated_at', type: 'DATETIME')
        }
        addUniqueConstraint(tableName: 'open_task_scan_result',
                columnNames: 'sub_id, result_type, result_key',
                constraintName: 'uk_open_task_scan_result_sub_type_key')
        createIndex(tableName: 'open_task_scan_result', indexName: 'idx_open_task_scan_result_task', unique: false) {
            column(name: 'task_id')
            column(name: 'scan_phase')
            column(name: 'result_type')
        }
    }
}
