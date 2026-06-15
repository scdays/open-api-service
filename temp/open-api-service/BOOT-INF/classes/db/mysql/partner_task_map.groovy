package db.mysql

databaseChangeLog(logicalFilePath: 'partner_task_map.groovy') {
    changeSet(id: '2026-05-21-create-table-partner_task_map', author: 'open-api') {
        createTable(tableName: 'partner_task_map', remarks: 'Partner 任务幂等映射') {
            column(name: 'id', type: 'BIGINT', autoIncrement: true, remarks: '主键') {
                constraints(primaryKey: true, primaryKeyName: 'pk_partner_task_map')
            }
            column(name: 'partner_id', type: 'VARCHAR(64)', remarks: 'Partner ID') {
                constraints(nullable: false)
            }
            column(name: 'ext_task_id', type: 'VARCHAR(128)', remarks: 'Partner 幂等键') {
                constraints(nullable: false)
            }
            column(name: 'platform_task_id', type: 'VARCHAR(64)', remarks: '平台 taskId') {
                constraints(nullable: false)
            }
            column(name: 'created_at', type: 'DATETIME', defaultValueComputed: 'CURRENT_TIMESTAMP', remarks: '创建时间') {
                constraints(nullable: false)
            }
        }
        addUniqueConstraint(tableName: 'partner_task_map', columnNames: 'partner_id, ext_task_id',
                constraintName: 'uk_partner_task_map_partner_ext')
        createIndex(tableName: 'partner_task_map', indexName: 'idx_partner_task_map_platform', unique: false) {
            column(name: 'platform_task_id')
        }
    }
}
