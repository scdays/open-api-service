package db.mysql

databaseChangeLog(logicalFilePath: 'partner_capability.groovy') {
    changeSet(id: '2026-05-22-create-table-partner_capability', author: 'open-api') {
        createTable(tableName: 'partner_capability', remarks: 'Partner 能力集') {
            column(name: 'id', type: 'BIGINT', autoIncrement: true) {
                constraints(primaryKey: true, primaryKeyName: 'pk_partner_capability')
            }
            column(name: 'partner_id', type: 'VARCHAR(64)') {
                constraints(nullable: false)
            }
            column(name: 'capability', type: 'VARCHAR(64)') {
                constraints(nullable: false)
            }
            column(name: 'created_at', type: 'DATETIME', defaultValueComputed: 'CURRENT_TIMESTAMP')
        }
        addUniqueConstraint(tableName: 'partner_capability', columnNames: 'partner_id, capability',
                constraintName: 'uk_partner_capability')
    }
}
