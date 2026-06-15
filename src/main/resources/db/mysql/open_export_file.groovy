package db.mysql

databaseChangeLog(logicalFilePath: 'open_export_file.groovy') {
    changeSet(id: '2026-06-13-create-table-open_export_file', author: 'open-api') {
        createTable(tableName: 'open_export_file', remarks: '开放平台外发文件（对齐 vul_scan_task_file）') {
            column(name: 'id', type: 'BIGINT', autoIncrement: true) {
                constraints(primaryKey: true, primaryKeyName: 'pk_open_export_file')
            }
            column(name: 'export_id', type: 'VARCHAR(64)') {
                constraints(nullable: false)
            }
            column(name: 'real_task_id', type: 'VARCHAR(64)', remarks: 'open_task.task_id')
            column(name: 'partner_id', type: 'VARCHAR(64)') {
                constraints(nullable: false)
            }
            column(name: 'file_position', type: 'VARCHAR(255)', remarks: 'bucket')
            column(name: 'file_field', type: 'VARCHAR(255)', remarks: 'fileKey')
            column(name: 'file_metadata', type: 'VARCHAR(255)')
            column(name: 'file_type', type: 'INT', remarks: '11=外发XML 12=外发JSON')
            column(name: 'create_time', type: 'DATETIME', defaultValueComputed: 'CURRENT_TIMESTAMP')
            column(name: 'update_time', type: 'DATETIME')
        }
        addUniqueConstraint(tableName: 'open_export_file', columnNames: 'export_id',
                constraintName: 'uk_open_export_file_export_id')
        createIndex(tableName: 'open_export_file', indexName: 'idx_open_export_file_task', unique: false) {
            column(name: 'real_task_id')
            column(name: 'partner_id')
        }
    }
}
