package db.mysql

databaseChangeLog(logicalFilePath: 'open_export.groovy') {
    changeSet(id: '2026-05-22-create-table-open_export', author: 'open-api') {
        createTable(tableName: 'open_export', remarks: '扫描结果外发记录') {
            column(name: 'id', type: 'BIGINT', autoIncrement: true) {
                constraints(primaryKey: true, primaryKeyName: 'pk_open_export')
            }
            column(name: 'export_id', type: 'VARCHAR(64)') {
                constraints(nullable: false)
            }
            column(name: 'partner_id', type: 'VARCHAR(64)') {
                constraints(nullable: false)
            }
            column(name: 'task_id', type: 'VARCHAR(64)')
            column(name: 'format', type: 'VARCHAR(16)', remarks: 'XML/JSON')
            column(name: 'status', type: 'VARCHAR(32)')
            column(name: 'record_count', type: 'INT')
            column(name: 'expires_at', type: 'DATETIME')
            column(name: 'storage_path', type: 'VARCHAR(512)')
            column(name: 'created_at', type: 'DATETIME', defaultValueComputed: 'CURRENT_TIMESTAMP')
        }
        addUniqueConstraint(tableName: 'open_export', columnNames: 'export_id', constraintName: 'uk_open_export_export_id')
    }
}
