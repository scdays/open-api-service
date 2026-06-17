package db.mysql

databaseChangeLog(logicalFilePath: 'open_verify_fix_job.groovy') {
    changeSet(id: '2026-06-17-create-open_verify_fix_job', author: 'open-api') {
        createTable(tableName: 'open_verify_fix_job', remarks: '修复核验内部复扫任务（Partner 不可见）') {
            column(name: 'id', type: 'BIGINT', autoIncrement: true, remarks: '主键') {
                constraints(primaryKey: true, primaryKeyName: 'pk_open_verify_fix_job')
            }
            column(name: 'job_id', type: 'VARCHAR(32)', remarks: 'verifyFixJobId') {
                constraints(nullable: false)
            }
            column(name: 'partner_id', type: 'VARCHAR(64)', remarks: 'Partner ID') {
                constraints(nullable: false)
            }
            column(name: 'batch_id', type: 'VARCHAR(128)', remarks: '批量幂等批次 ID')
            column(name: 'status', type: 'VARCHAR(16)', remarks: 'PENDING/RUNNING/FINISHED/FAILED') {
                constraints(nullable: false)
            }
            column(name: 'item_count', type: 'INT', defaultValueNumeric: 0, remarks: '目标实例数')
            column(name: 'error_message', type: 'VARCHAR(512)', remarks: '失败原因')
            column(name: 'rescan_imported', type: 'BOOLEAN', defaultValueBoolean: false, remarks: '是否已导入复扫 XML')
            column(name: 'finished_at', type: 'DATETIME', remarks: '完成时间')
            column(name: 'created_at', type: 'DATETIME', defaultValueComputed: 'CURRENT_TIMESTAMP', remarks: '创建时间') {
                constraints(nullable: false)
            }
            column(name: 'updated_at', type: 'DATETIME', remarks: '更新时间')
        }
        addUniqueConstraint(tableName: 'open_verify_fix_job', columnNames: 'job_id', constraintName: 'uk_open_verify_fix_job_id')
        createIndex(tableName: 'open_verify_fix_job', indexName: 'idx_open_verify_fix_job_partner', unique: false) {
            column(name: 'partner_id')
            column(name: 'status')
        }

        createTable(tableName: 'open_verify_fix_job_item', remarks: '修复核验任务目标实例') {
            column(name: 'id', type: 'BIGINT', autoIncrement: true, remarks: '主键') {
                constraints(primaryKey: true, primaryKeyName: 'pk_open_verify_fix_job_item')
            }
            column(name: 'job_id', type: 'VARCHAR(32)', remarks: 'verifyFixJobId') {
                constraints(nullable: false)
            }
            column(name: 'partner_id', type: 'VARCHAR(64)', remarks: 'Partner ID') {
                constraints(nullable: false)
            }
            column(name: 'vul_info_id', type: 'VARCHAR(64)', remarks: '实例 ID') {
                constraints(nullable: false)
            }
            column(name: 'task_id', type: 'VARCHAR(64)', remarks: '关联排查任务 ID')
            column(name: 'previous_stat', type: 'INT', remarks: '受理前状态')
            column(name: 'result_stat', type: 'INT', remarks: '完成后状态 6/7/10')
            column(name: 'item_status', type: 'VARCHAR(16)', remarks: 'PENDING/DONE/FAILED') {
                constraints(nullable: false)
            }
            column(name: 'created_at', type: 'DATETIME', defaultValueComputed: 'CURRENT_TIMESTAMP', remarks: '创建时间') {
                constraints(nullable: false)
            }
            column(name: 'updated_at', type: 'DATETIME', remarks: '更新时间')
        }
        addUniqueConstraint(tableName: 'open_verify_fix_job_item', columnNames: 'job_id, vul_info_id',
                constraintName: 'uk_open_verify_fix_job_item')
        createIndex(tableName: 'open_verify_fix_job_item', indexName: 'idx_open_verify_fix_job_item_vul', unique: false) {
            column(name: 'partner_id')
            column(name: 'vul_info_id')
        }
    }
}
