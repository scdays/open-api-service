package db.mysql

databaseChangeLog(logicalFilePath: 'webhook_delivery_log.groovy') {
    changeSet(id: '2026-05-22-create-table-webhook_delivery_log', author: 'open-api') {
        createTable(tableName: 'webhook_delivery_log', remarks: 'Webhook 投递日志') {
            column(name: 'id', type: 'BIGINT', autoIncrement: true) {
                constraints(primaryKey: true, primaryKeyName: 'pk_webhook_delivery_log')
            }
            column(name: 'partner_id', type: 'VARCHAR(64)') {
                constraints(nullable: false)
            }
            column(name: 'event_type', type: 'VARCHAR(64)')
            column(name: 'payload_json', type: 'TEXT')
            column(name: 'callback_url', type: 'VARCHAR(512)')
            column(name: 'http_status', type: 'INT')
            column(name: 'retry_count', type: 'INT', defaultValueNumeric: 0)
            column(name: 'status', type: 'VARCHAR(32)', remarks: 'PENDING/SUCCESS/FAILED')
            column(name: 'created_at', type: 'DATETIME', defaultValueComputed: 'CURRENT_TIMESTAMP')
            column(name: 'next_retry_at', type: 'DATETIME')
        }
        createIndex(tableName: 'webhook_delivery_log', indexName: 'idx_webhook_delivery_partner', unique: false) {
            column(name: 'partner_id')
            column(name: 'status')
        }
    }

    changeSet(id: '2026-05-26-webhook-delivery-log-governance-fields', author: 'open-api') {
        addColumn(tableName: 'webhook_delivery_log') {
            column(name: 'event_id', type: 'VARCHAR(64)', remarks: 'Webhook 信封 eventId（幂等键）')
        }
        addColumn(tableName: 'webhook_delivery_log') {
            column(name: 'resource_type', type: 'VARCHAR(32)', remarks: 'TASK / INSTANCE / EXPORT')
        }
        addColumn(tableName: 'webhook_delivery_log') {
            column(name: 'resource_id', type: 'VARCHAR(128)', remarks: '主资源 ID：taskId / vulInfoID / exportId')
        }
        addColumn(tableName: 'webhook_delivery_log') {
            column(name: 'resource_ids_json', type: 'TEXT', remarks: '批量实例 vulInfoID 列表 JSON')
        }
        addColumn(tableName: 'webhook_delivery_log') {
            column(name: 'trigger_source', type: 'VARCHAR(32)', remarks: 'FIRST_ATTEMPT / AUTO_RETRY / MANUAL_RETRY')
        }
        createIndex(tableName: 'webhook_delivery_log', indexName: 'idx_webhook_delivery_event', unique: false) {
            column(name: 'partner_id')
            column(name: 'event_id')
        }
        createIndex(tableName: 'webhook_delivery_log', indexName: 'idx_webhook_delivery_resource', unique: false) {
            column(name: 'partner_id')
            column(name: 'resource_type')
            column(name: 'resource_id')
        }
    }
}
