package db.mysql

databaseChangeLog(logicalFilePath: 'api_invocation.groovy') {
    changeSet(id: '2026-05-22-create-table-api_invocation', author: 'open-api') {
        createTable(tableName: 'api_invocation', remarks: '开放平台 API 调用记录（治理平面）') {
            column(name: 'invocation_id', type: 'VARCHAR(64)', remarks: '调用记录 ID') {
                constraints(primaryKey: true, primaryKeyName: 'pk_api_invocation')
            }
            column(name: 'request_id', type: 'VARCHAR(64)', remarks: '与响应 requestId、X-Request-Id 一致') {
                constraints(nullable: false)
            }
            column(name: 'partner_id', type: 'VARCHAR(64)', remarks: 'Partner ID') {
                constraints(nullable: false)
            }
            column(name: 'operation_id', type: 'VARCHAR(64)', remarks: '关联 api_operation') {
                constraints(nullable: false)
            }
            column(name: 'http_method', type: 'VARCHAR(8)', remarks: 'HTTP 方法')
            column(name: 'request_path', type: 'VARCHAR(512)', remarks: '实际请求路径')
            column(name: 'response_code', type: 'INT', remarks: '业务 code，0=成功')
            column(name: 'http_status', type: 'INT', defaultValueNumeric: 200, remarks: 'HTTP 状态码')
            column(name: 'latency_ms', type: 'INT', remarks: '端到端耗时 ms')
            column(name: 'client_ip', type: 'VARCHAR(64)', remarks: '客户端 IP')
            column(name: 'error_message', type: 'VARCHAR(512)', remarks: '失败摘要')
            column(name: 'resource_type', type: 'VARCHAR(32)', remarks: 'TASK/INSTANCE')
            column(name: 'resource_id', type: 'VARCHAR(128)', remarks: 'taskId / vulInfoID')
            column(name: 'started_at', type: 'DATETIME', remarks: '开始时间') {
                constraints(nullable: false)
            }
            column(name: 'finished_at', type: 'DATETIME', remarks: '结束时间')
        }
        createIndex(tableName: 'api_invocation', indexName: 'idx_api_inv_partner', unique: false) {
            column(name: 'partner_id')
        }
        createIndex(tableName: 'api_invocation', indexName: 'idx_api_inv_request_id', unique: false) {
            column(name: 'request_id')
        }
        createIndex(tableName: 'api_invocation', indexName: 'idx_api_inv_operation', unique: false) {
            column(name: 'operation_id')
        }
        createIndex(tableName: 'api_invocation', indexName: 'idx_api_inv_started', unique: false) {
            column(name: 'started_at')
        }
    }

    changeSet(id: '2026-05-26-api-invocation-response-body-json', author: 'open-api') {
        addColumn(tableName: 'api_invocation') {
            column(name: 'response_body_json', type: 'MEDIUMTEXT', remarks: 'Partner 实际收到的 ApiResponse JSON')
        }
    }

    changeSet(id: '2026-06-13-api-invocation-request-body-json', author: 'open-api') {
        addColumn(tableName: 'api_invocation') {
            column(name: 'request_body_json', type: 'MEDIUMTEXT', remarks: 'Partner 实际提交的请求体 JSON')
        }
    }
}
