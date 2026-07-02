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

    changeSet(id: '2026-06-13-extend-open_export-metadata', author: 'open-api') {
        addColumn(tableName: 'open_export') {
            column(name: 'ext_task_id', type: 'VARCHAR(128)')
            column(name: 'report_template_id', type: 'INT')
            column(name: 'export_stage', type: 'VARCHAR(32)')
            column(name: 'data_type', type: 'VARCHAR(32)')
            column(name: 'generated_at', type: 'DATETIME')
            column(name: 'download_url', type: 'VARCHAR(1024)', remarks: 'file-sharing 完整下载 URL')
            column(name: 'error_message', type: 'VARCHAR(1024)')
            column(name: 'verify_fix_job_id', type: 'VARCHAR(64)')
            column(name: 'updated_at', type: 'DATETIME')
        }
        addUniqueConstraint(tableName: 'open_export',
                columnNames: 'partner_id, task_id, export_stage, format',
                constraintName: 'uk_open_export_task_stage_format')
        createIndex(tableName: 'open_export', indexName: 'idx_open_export_partner_task', unique: false) {
            column(name: 'partner_id')
            column(name: 'task_id')
        }
    }

    changeSet(id: '2026-06-22-open_export-sub-id', author: 'open-api') {
        addColumn(tableName: 'open_export') {
            column(name: 'sub_id', type: 'VARCHAR(32)', remarks: '原始报告归档关联的 open_task_sub.sub_id；外发记录为 NULL')
        }
        dropUniqueConstraint(tableName: 'open_export', constraintName: 'uk_open_export_task_stage_format')
        addUniqueConstraint(tableName: 'open_export',
                columnNames: 'partner_id, task_id, export_stage, format, sub_id',
                constraintName: 'uk_open_export_task_stage_format')
    }

    changeSet(id: '2026-06-29-open_export-webhook_event_id', author: 'open-api') {
        addColumn(tableName: 'open_export') {
            column(name: 'webhook_event_id', type: 'VARCHAR(64)', remarks: '关联的 Webhook 事件ID（业务侧生成）')
        }
    }
}
