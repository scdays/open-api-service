package db.mysql

databaseChangeLog(logicalFilePath: 'partner_credential.groovy') {
    changeSet(id: '2026-05-22-create-table-partner_credential', author: 'open-api') {
        createTable(tableName: 'partner_credential', remarks: 'Partner 机机凭证') {
            column(name: 'id', type: 'BIGINT', autoIncrement: true) {
                constraints(primaryKey: true, primaryKeyName: 'pk_partner_credential')
            }
            column(name: 'partner_id', type: 'VARCHAR(64)') {
                constraints(nullable: false)
            }
            column(name: 'client_id', type: 'VARCHAR(64)') {
                constraints(nullable: false)
            }
            column(name: 'client_secret_hash', type: 'VARCHAR(128)', remarks: 'secret 哈希') {
                constraints(nullable: false)
            }
            column(name: 'status', type: 'VARCHAR(16)', defaultValue: 'ACTIVE') {
                constraints(nullable: false)
            }
            column(name: 'expires_at', type: 'DATETIME')
            column(name: 'created_at', type: 'DATETIME', defaultValueComputed: 'CURRENT_TIMESTAMP')
            column(name: 'updated_at', type: 'DATETIME')
        }
        addUniqueConstraint(tableName: 'partner_credential', columnNames: 'client_id', constraintName: 'uk_partner_credential_client_id')
        createIndex(tableName: 'partner_credential', indexName: 'idx_partner_credential_partner', unique: false) {
            column(name: 'partner_id')
        }
    }
}
