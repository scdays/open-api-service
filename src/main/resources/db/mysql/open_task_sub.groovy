package db.mysql

databaseChangeLog(logicalFilePath: 'open_task_sub.groovy') {
    changeSet(id: '2026-06-18-create-open_task_sub', author: 'open-api') {
        createTable(tableName: 'open_task_sub', remarks: '开放平台任务子扫描（vuln-task-center survey 1:1）') {
            column(name: 'id', type: 'BIGINT', autoIncrement: true, remarks: '主键') {
                constraints(primaryKey: true, primaryKeyName: 'pk_open_task_sub')
            }
            column(name: 'sub_id', type: 'VARCHAR(32)', remarks: '子任务业务 ID') {
                constraints(nullable: false)
            }
            column(name: 'task_id', type: 'VARCHAR(64)', remarks: 'open_task.task_id') {
                constraints(nullable: false)
            }
            column(name: 'partner_id', type: 'VARCHAR(64)', remarks: 'Partner ID') {
                constraints(nullable: false)
            }
            column(name: 'scan_phase', type: 'INT', remarks: '1=排查 2=验证(autoVerify)') {
                constraints(nullable: false)
            }
            column(name: 'scanner_type', type: 'VARCHAR(8)', remarks: 'task-center scannerType') {
                constraints(nullable: false)
            }
            column(name: 'center_task_type', type: 'VARCHAR(16)', remarks: 'vuln/port/alive') {
                constraints(nullable: false)
            }
            column(name: 'center_plan_id', type: 'VARCHAR(64)', remarks: 'vuln-task-center 计划 ID')
            column(name: 'survey_id', type: 'VARCHAR(64)', remarks: '计划实例 surveyId')
            column(name: 'status', type: 'VARCHAR(16)', remarks: 'PENDING/RUNNING/FINISHED/FAILED') {
                constraints(nullable: false)
            }
            column(name: 'progress', type: 'INT', defaultValueNumeric: 0, remarks: '进度 0-100')
            column(name: 'error_message', type: 'VARCHAR(512)', remarks: '失败原因')
            column(name: 'created_at', type: 'DATETIME', defaultValueComputed: 'CURRENT_TIMESTAMP') {
                constraints(nullable: false)
            }
            column(name: 'updated_at', type: 'DATETIME')
        }
        addUniqueConstraint(tableName: 'open_task_sub', columnNames: 'sub_id', constraintName: 'uk_open_task_sub_id')
        createIndex(tableName: 'open_task_sub', indexName: 'idx_open_task_sub_task', unique: false) {
            column(name: 'task_id')
            column(name: 'scan_phase')
        }
        createIndex(tableName: 'open_task_sub', indexName: 'idx_open_task_sub_status', unique: false) {
            column(name: 'status')
        }
    }

    changeSet(id: '2026-06-18-extend-open_task-task-center', author: 'open-api') {
        addColumn(tableName: 'open_task') {
            column(name: 'task_phase', type: 'INT', defaultValueNumeric: 1, remarks: '编排阶段 1=排查 2=验证')
            column(name: 'auto_verify', type: 'BOOLEAN', defaultValueBoolean: true, remarks: '是否 autoVerify')
            column(name: 'verify_merge_strategy', type: 'VARCHAR(16)', defaultValue: 'UNION', remarks: '交叉扫描合并策略')
            column(name: 'cross_scan', type: 'BOOLEAN', defaultValueBoolean: false, remarks: '是否双扫描器交叉')
        }
    }

    changeSet(id: '2026-06-18-open_task_sub-report-download-path', author: 'open-api') {
        addColumn(tableName: 'open_task_sub') {
            column(name: 'report_download_path', type: 'VARCHAR(512)', remarks: 'VTC 报告 FTP 下载路径')
        }
    }
}
