package db.mysql

databaseChangeLog(logicalFilePath: 'open_artifact.groovy') {
    changeSet(id: '2026-06-23-create-table-open_artifact', author: 'open-api') {
        createTable(tableName: 'open_artifact', remarks: '扫描报告产物记录') {
            column(name: 'id', type: 'BIGINT', autoIncrement: true) {
                constraints(primaryKey: true, primaryKeyName: 'pk_open_artifact')
            }
            column(name: 'artifact_id', type: 'VARCHAR(64)') {
                constraints(nullable: false)
            }
            column(name: 'partner_id', type: 'VARCHAR(64)') {
                constraints(nullable: false)
            }
            column(name: 'task_id', type: 'VARCHAR(64)') {
                constraints(nullable: false)
            }
            column(name: 'ext_task_id', type: 'VARCHAR(128)')
            column(name: 'export_id', type: 'VARCHAR(64)', remarks: '关联规范化外发 ID，可为空')
            column(name: 'export_stage', type: 'VARCHAR(32)') {
                constraints(nullable: false)
            }
            column(name: 'artifact_source', type: 'VARCHAR(32)') {
                constraints(nullable: false)
            }
            column(name: 'report_type_code', type: 'INT')
            column(name: 'report_type_name', type: 'VARCHAR(128)')
            column(name: 'scanner_vendor', type: 'VARCHAR(64)')
            column(name: 'scanner_product', type: 'VARCHAR(128)')
            column(name: 'sub_task_id', type: 'VARCHAR(64)', remarks: 'open_task_sub.sub_id')
            column(name: 'file_name', type: 'VARCHAR(255)') {
                constraints(nullable: false)
            }
            column(name: 'file_format', type: 'VARCHAR(16)') {
                constraints(nullable: false)
            }
            column(name: 'content_type', type: 'VARCHAR(128)') {
                constraints(nullable: false)
            }
            column(name: 'byte_size', type: 'BIGINT')
            column(name: 'checksum', type: 'VARCHAR(128)', remarks: 'SHA-256')
            column(name: 'status', type: 'VARCHAR(32)') {
                constraints(nullable: false)
            }
            column(name: 'generated_at', type: 'DATETIME')
            column(name: 'expires_at', type: 'DATETIME')
            column(name: 'download_url', type: 'VARCHAR(1024)')
            column(name: 'error_message', type: 'VARCHAR(1024)')
            column(name: 'file_position', type: 'VARCHAR(255)', remarks: 'bucket')
            column(name: 'file_field', type: 'VARCHAR(255)', remarks: 'fileKey')
            column(name: 'created_at', type: 'DATETIME', defaultValueComputed: 'CURRENT_TIMESTAMP')
            column(name: 'updated_at', type: 'DATETIME')
        }
        addUniqueConstraint(tableName: 'open_artifact', columnNames: 'artifact_id',
                constraintName: 'uk_open_artifact_artifact_id')
        addUniqueConstraint(tableName: 'open_artifact', columnNames: 'partner_id, sub_task_id, artifact_source',
                constraintName: 'uk_open_artifact_sub_source')
        createIndex(tableName: 'open_artifact', indexName: 'idx_open_artifact_partner_task', unique: false) {
            column(name: 'partner_id')
            column(name: 'task_id')
            column(name: 'export_stage')
        }
        createIndex(tableName: 'open_artifact', indexName: 'idx_open_artifact_export', unique: false) {
            column(name: 'partner_id')
            column(name: 'export_id')
        }
    }

    changeSet(id: '2026-07-03-open_artifact-webhook-delivery', author: 'open-api') {
        addColumn(tableName: 'open_artifact') {
            column(name: 'webhook_delivery_status', type: 'VARCHAR(16)',
                    remarks: 'ARTIFACT_READY 投递：PENDING/SENT')
            column(name: 'verify_fix_job_id', type: 'VARCHAR(64)',
                    remarks: 'VERIFY_FIX_SCAN 关联 verifyFixJobId')
        }
        createIndex(tableName: 'open_artifact', indexName: 'idx_open_artifact_webhook_pending', unique: false) {
            column(name: 'webhook_delivery_status')
            column(name: 'task_id')
            column(name: 'export_stage')
        }
    }
}
