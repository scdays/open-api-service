package db.mysql

databaseChangeLog(logicalFilePath: 'partner.groovy') {
    changeSet(id: '2026-05-22-create-table-partner', author: 'open-api') {
        createTable(tableName: 'partner', remarks: 'Partner 主数据') {
            column(name: 'id', type: 'BIGINT', autoIncrement: true, remarks: '主键') {
                constraints(primaryKey: true, primaryKeyName: 'pk_partner')
            }
            column(name: 'partner_id', type: 'VARCHAR(64)', remarks: 'Partner 唯一标识') {
                constraints(nullable: false)
            }
            column(name: 'partner_name', type: 'VARCHAR(128)', remarks: '名称') {
                constraints(nullable: false)
            }
            column(name: 'partner_type', type: 'VARCHAR(32)', remarks: '类型 SIEM/ITSM 等')
            column(name: 'status', type: 'VARCHAR(16)', defaultValue: 'ACTIVE', remarks: 'ACTIVE/DISABLED') {
                constraints(nullable: false)
            }
            column(name: 'rate_limit_qps', type: 'INT', defaultValueNumeric: 100, remarks: '网关限流 QPS')
            column(name: 'created_at', type: 'DATETIME', defaultValueComputed: 'CURRENT_TIMESTAMP') {
                constraints(nullable: false)
            }
            column(name: 'updated_at', type: 'DATETIME')
        }
        addUniqueConstraint(tableName: 'partner', columnNames: 'partner_id', constraintName: 'uk_partner_partner_id')
    }
}
