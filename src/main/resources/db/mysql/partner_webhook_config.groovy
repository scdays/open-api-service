package db.mysql

databaseChangeLog(logicalFilePath: 'partner_webhook_config.groovy') {
    changeSet(id: '2026-05-22-create-table-partner_webhook_config', author: 'open-api') {
        createTable(tableName: 'partner_webhook_config', remarks: 'Partner Webhook 配置') {
            column(name: 'id', type: 'BIGINT', autoIncrement: true) {
                constraints(primaryKey: true, primaryKeyName: 'pk_partner_webhook_config')
            }
            column(name: 'partner_id', type: 'VARCHAR(64)') {
                constraints(nullable: false)
            }
            column(name: 'callback_url', type: 'VARCHAR(512)')
            column(name: 'webhook_secret_hash', type: 'VARCHAR(128)')
            column(name: 'updated_at', type: 'DATETIME')
        }
        addUniqueConstraint(tableName: 'partner_webhook_config', columnNames: 'partner_id',
                constraintName: 'uk_partner_webhook_config_partner')
    }
}
