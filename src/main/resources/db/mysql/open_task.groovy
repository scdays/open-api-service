package db.mysql

databaseChangeLog(logicalFilePath: 'open_task.groovy') {
    changeSet(id: '2026-05-21-create-table-open_task', author: 'open-api') {
        createTable(tableName: 'open_task', remarks: '开放平台任务') {
            column(name: 'id', type: 'BIGINT', autoIncrement: true, remarks: '主键') {
                constraints(primaryKey: true, primaryKeyName: 'pk_open_task')
            }
            column(name: 'task_id', type: 'VARCHAR(64)', remarks: '平台对外任务 ID') {
                constraints(nullable: false)
            }
            column(name: 'partner_id', type: 'VARCHAR(64)', remarks: 'Partner ID') {
                constraints(nullable: false)
            }
            column(name: 'ext_task_id', type: 'VARCHAR(128)', remarks: 'Partner 幂等键') {
                constraints(nullable: false)
            }
            column(name: 'engine_task_id', type: 'VARCHAR(64)', remarks: 'SVMP 引擎 taskId')
            column(name: 'task_name', type: 'VARCHAR(256)', remarks: '任务名称') {
                constraints(nullable: false)
            }
            column(name: 'target_type', type: 'VARCHAR(16)', remarks: 'IPV4/IPV6/URL')
            column(name: 'vuln_type', type: 'INT', remarks: '1=系统 2=Web')
            column(name: 'targets_json', type: 'TEXT', remarks: '扫描目标 JSON')
            column(name: 'status', type: 'VARCHAR(32)', remarks: 'ACCEPTED/PENDING/RUNNING/FINISHED/FAILED') {
                constraints(nullable: false)
            }
            column(name: 'progress', type: 'INT', defaultValueNumeric: 0, remarks: '进度 0-100')
            column(name: 'scan_template_id', type: 'INT', remarks: '扫描模板 ID')
            column(name: 'callback_url', type: 'VARCHAR(512)', remarks: '回调地址')
            column(name: 'options_json', type: 'TEXT', remarks: '扫描选项 JSON')
            column(name: 'error_message', type: 'VARCHAR(1024)', remarks: '失败原因')
            column(name: 'started_at', type: 'DATETIME', remarks: '开始时间')
            column(name: 'finished_at', type: 'DATETIME', remarks: '结束时间')
            column(name: 'created_at', type: 'DATETIME', defaultValueComputed: 'CURRENT_TIMESTAMP', remarks: '创建时间') {
                constraints(nullable: false)
            }
            column(name: 'updated_at', type: 'DATETIME', remarks: '更新时间')
        }
        addUniqueConstraint(tableName: 'open_task', columnNames: 'task_id', constraintName: 'uk_open_task_task_id')
        addUniqueConstraint(tableName: 'open_task', columnNames: 'partner_id, ext_task_id', constraintName: 'uk_open_task_partner_ext')
        createIndex(tableName: 'open_task', indexName: 'idx_open_task_partner', unique: false) {
            column(name: 'partner_id')
        }
        createIndex(tableName: 'open_task', indexName: 'idx_open_task_partner_status', unique: false) {
            column(name: 'partner_id')
            column(name: 'status')
        }
    }

    changeSet(id: '2026-06-13-extend-open_task-ingest', author: 'open-api') {
        addColumn(tableName: 'open_task') {
            column(name: 'instances_ingested', type: 'BOOLEAN', defaultValueBoolean: false, remarks: 'Mock 实例是否已 ingest')
            column(name: 'ingest_error', type: 'VARCHAR(512)', remarks: 'Mock ingest 失败原因')
        }
        createIndex(tableName: 'open_task', indexName: 'idx_open_task_engine_task_id', unique: false) {
            column(name: 'engine_task_id')
        }
    }
}
