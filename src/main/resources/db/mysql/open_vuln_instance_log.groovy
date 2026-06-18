package db.mysql

databaseChangeLog(logicalFilePath: 'open_vuln_instance_log.groovy') {
    changeSet(id: '2026-06-18-create-open_vuln_instance_log', author: 'open-api') {
        createTable(tableName: 'open_vuln_instance_log', remarks: '开放平台漏洞实例状态跃迁审计') {
            column(name: 'id', type: 'BIGINT', autoIncrement: true) {
                constraints(primaryKey: true, primaryKeyName: 'pk_open_vuln_instance_log')
            }
            column(name: 'partner_id', type: 'VARCHAR(64)') {
                constraints(nullable: false)
            }
            column(name: 'vul_info_id', type: 'VARCHAR(64)') {
                constraints(nullable: false)
            }
            column(name: 'task_id', type: 'VARCHAR(64)') {
                constraints(nullable: false)
            }
            column(name: 'sub_id', type: 'VARCHAR(64)')
            column(name: 'scan_phase', type: 'INT')
            column(name: 'prev_stat', type: 'INT')
            column(name: 'vul_info_stat', type: 'INT') {
                constraints(nullable: false)
            }
            column(name: 'change_reason', type: 'VARCHAR(32)') {
                constraints(nullable: false)
            }
            column(name: 'verify_merge_strategy', type: 'VARCHAR(16)')
            column(name: 'scanner_hit_count', type: 'INT')
            column(name: 'transfer_time', type: 'VARCHAR(20)')
            column(name: 'created_at', type: 'DATETIME', defaultValueComputed: 'CURRENT_TIMESTAMP')
        }
        createIndex(tableName: 'open_vuln_instance_log', indexName: 'idx_open_vuln_instance_log_vul', unique: false) {
            column(name: 'vul_info_id')
        }
        createIndex(tableName: 'open_vuln_instance_log', indexName: 'idx_open_vuln_instance_log_task', unique: false) {
            column(name: 'task_id')
        }
    }
}
