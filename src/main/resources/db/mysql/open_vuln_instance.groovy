package db.mysql

databaseChangeLog(logicalFilePath: 'open_vuln_instance.groovy') {
    changeSet(id: '2026-05-22-create-table-open_vuln_instance', author: 'open-api') {
        createTable(tableName: 'open_vuln_instance', remarks: '开放平台漏洞实例映射') {
            column(name: 'id', type: 'BIGINT', autoIncrement: true) {
                constraints(primaryKey: true, primaryKeyName: 'pk_open_vuln_instance')
            }
            column(name: 'partner_id', type: 'VARCHAR(64)') {
                constraints(nullable: false)
            }
            column(name: 'vul_info_id', type: 'VARCHAR(64)', remarks: '对外 vulInfoID') {
                constraints(nullable: false)
            }
            column(name: 'vuln_disposal_id', type: 'VARCHAR(64)', remarks: 'SVMP 处置 ID')
            column(name: 'engine_task_id', type: 'VARCHAR(64)')
            column(name: 'vul_info_stat', type: 'INT')
            column(name: 'snapshot_json', type: 'TEXT')
            column(name: 'created_at', type: 'DATETIME', defaultValueComputed: 'CURRENT_TIMESTAMP')
            column(name: 'updated_at', type: 'DATETIME')
        }
        addUniqueConstraint(tableName: 'open_vuln_instance', columnNames: 'vul_info_id',
                constraintName: 'uk_open_vuln_instance_vul_info_id')
        createIndex(tableName: 'open_vuln_instance', indexName: 'idx_open_vuln_instance_partner', unique: false) {
            column(name: 'partner_id')
        }
    }
}
