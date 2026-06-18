package db.mysql

databaseChangeLog(logicalFilePath: 'open_operation_case.groovy') {
    changeSet(id: '2026-06-18-create-open_operation_case', author: 'open-api') {
        createTable(tableName: 'open_operation_case', remarks: '开放平台运营案件') {
            column(name: 'case_id', type: 'VARCHAR(32)', remarks: '案件 ID，CASE-{uuid12}') {
                constraints(primaryKey: true, primaryKeyName: 'pk_open_operation_case')
            }
            column(name: 'partner_id', type: 'VARCHAR(64)') {
                constraints(nullable: false)
            }
            column(name: 'case_type', type: 'VARCHAR(32)') {
                constraints(nullable: false)
            }
            column(name: 'status', type: 'VARCHAR(16)') {
                constraints(nullable: false)
            }
            column(name: 'title', type: 'VARCHAR(256)')
            column(name: 'primary_resource_type', type: 'VARCHAR(32)')
            column(name: 'primary_resource_id', type: 'VARCHAR(128)')
            column(name: 'batch_id', type: 'VARCHAR(128)')
            column(name: 'invocation_id', type: 'VARCHAR(64)')
            column(name: 'idempotency_key', type: 'VARCHAR(128)')
            column(name: 'request_summary_json', type: 'TEXT')
            column(name: 'result_summary_json', type: 'TEXT')
            column(name: 'error_message', type: 'VARCHAR(512)')
            column(name: 'started_at', type: 'DATETIME') {
                constraints(nullable: false)
            }
            column(name: 'finished_at', type: 'DATETIME')
            column(name: 'created_at', type: 'DATETIME', defaultValueComputed: 'CURRENT_TIMESTAMP')
            column(name: 'updated_at', type: 'DATETIME', defaultValueComputed: 'CURRENT_TIMESTAMP')
        }
        createIndex(tableName: 'open_operation_case', indexName: 'idx_open_op_case_partner', unique: false) {
            column(name: 'partner_id')
            column(name: 'case_type')
            column(name: 'status')
        }
        createIndex(tableName: 'open_operation_case', indexName: 'idx_open_op_case_primary', unique: false) {
            column(name: 'primary_resource_id')
        }
        createIndex(tableName: 'open_operation_case', indexName: 'idx_open_op_case_batch', unique: false) {
            column(name: 'batch_id')
        }
        createIndex(tableName: 'open_operation_case', indexName: 'idx_open_op_case_invocation', unique: false) {
            column(name: 'invocation_id')
        }
    }

    changeSet(id: '2026-06-18-create-open_operation_case_event', author: 'open-api') {
        createTable(tableName: 'open_operation_case_event', remarks: '运营案件时间线') {
            column(name: 'id', type: 'BIGINT', autoIncrement: true) {
                constraints(primaryKey: true, primaryKeyName: 'pk_open_operation_case_event')
            }
            column(name: 'case_id', type: 'VARCHAR(32)') {
                constraints(nullable: false)
            }
            column(name: 'event_type', type: 'VARCHAR(32)') {
                constraints(nullable: false)
            }
            column(name: 'event_payload_json', type: 'TEXT')
            column(name: 'created_at', type: 'DATETIME', defaultValueComputed: 'CURRENT_TIMESTAMP')
        }
        createIndex(tableName: 'open_operation_case_event', indexName: 'idx_open_op_case_event_case', unique: false) {
            column(name: 'case_id')
        }
    }

    changeSet(id: '2026-06-18-api-invocation-case-id', author: 'open-api') {
        addColumn(tableName: 'api_invocation') {
            column(name: 'case_id', type: 'VARCHAR(32)', remarks: '关联运营案件')
        }
        createIndex(tableName: 'api_invocation', indexName: 'idx_api_inv_case_id', unique: false) {
            column(name: 'case_id')
        }
    }

    changeSet(id: '2026-06-18-reserve-case-id-related-tables', author: 'open-api') {
        addColumn(tableName: 'open_task') {
            column(name: 'case_id', type: 'VARCHAR(32)', remarks: 'TASK_SCAN 案件互指')
        }
        addColumn(tableName: 'open_verify_fix_job') {
            column(name: 'case_id', type: 'VARCHAR(32)', remarks: 'VERIFY_FIX 案件互指')
        }
        addColumn(tableName: 'open_vuln_instance_log') {
            column(name: 'case_id', type: 'VARCHAR(32)', remarks: '跃迁与案件绑定')
        }
    }

    changeSet(id: '2026-06-18-create-open_operation_case_target', author: 'open-api') {
        createTable(tableName: 'open_operation_case_target', remarks: '运营案件多目标（批量操作）') {
            column(name: 'id', type: 'BIGINT', autoIncrement: true) {
                constraints(primaryKey: true, primaryKeyName: 'pk_open_operation_case_target')
            }
            column(name: 'case_id', type: 'VARCHAR(32)') {
                constraints(nullable: false)
            }
            column(name: 'target_key', type: 'VARCHAR(128)') {
                constraints(nullable: false)
            }
            column(name: 'target_status', type: 'VARCHAR(16)') {
                constraints(nullable: false)
            }
            column(name: 'prev_stat', type: 'INT')
            column(name: 'result_stat', type: 'INT')
            column(name: 'payload_json', type: 'TEXT')
            column(name: 'created_at', type: 'DATETIME', defaultValueComputed: 'CURRENT_TIMESTAMP')
        }
        createIndex(tableName: 'open_operation_case_target', indexName: 'idx_open_op_case_target_case', unique: false) {
            column(name: 'case_id')
        }
    }
}
