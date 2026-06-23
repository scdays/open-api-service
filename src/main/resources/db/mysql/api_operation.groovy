package db.mysql

databaseChangeLog(logicalFilePath: 'api_operation.groovy') {

    changeSet(id: 'init-api_operation-table', author: 'open-api') {
        createTable(tableName: 'api_operation', remarks: '开放平台 API 目录（治理平面）') {
            column(name: 'operation_id', type: 'VARCHAR(64)', remarks: 'OpenAPI operationId') {
                constraints(primaryKey: true, primaryKeyName: 'pk_api_operation')
            }
            column(name: 'api_version', type: 'VARCHAR(16)', remarks: 'API 版本') {
                constraints(nullable: false)
            }
            column(name: 'http_method', type: 'VARCHAR(8)', remarks: 'HTTP 方法') {
                constraints(nullable: false)
            }
            column(name: 'path_pattern', type: 'VARCHAR(256)', remarks: '路径模式') {
                constraints(nullable: false)
            }
            column(name: 'required_capability', type: 'VARCHAR(64)', remarks: '所需 capability') {
                constraints(nullable: false)
            }
            column(name: 'domain', type: 'VARCHAR(32)', remarks: '领域 AUTH/TASK/INSTANCE/EXPORT/WEBHOOK') {
                constraints(nullable: false)
            }
            column(name: 'openapi_tag', type: 'VARCHAR(32)', remarks: 'OpenAPI tags：auth/tasks/instances/exports/webhooks') {
                constraints(nullable: true)
            }
            column(name: 'summary', type: 'VARCHAR(128)', remarks: 'OpenAPI summary，管理台展示') {
                constraints(nullable: true)
            }
            column(name: 'status', type: 'VARCHAR(16)', remarks: 'PUBLISHED/DEPRECATED/DISABLED') {
                constraints(nullable: false)
            }
            column(name: 'published_at', type: 'DATETIME', remarks: '上架时间')
        }
        createIndex(tableName: 'api_operation', indexName: 'idx_api_operation_domain', unique: false) {
            column(name: 'domain')
        }
        createIndex(tableName: 'api_operation', indexName: 'idx_api_operation_openapi_tag', unique: false) {
            column(name: 'openapi_tag')
        }
    }

    changeSet(id: 'init-api_operation-seed', author: 'open-api') {
        insert(tableName: 'api_operation') {
            column(name: 'operation_id', value: 'createTask')
            column(name: 'api_version', value: '1.0.0')
            column(name: 'http_method', value: 'POST')
            column(name: 'path_pattern', value: '/api/open/v1/tasks')
            column(name: 'required_capability', value: 'TASK_WRITE')
            column(name: 'domain', value: 'TASK')
            column(name: 'openapi_tag', value: 'tasks')
            column(name: 'summary', value: '创建扫描任务（已废弃，请用 createTaskByJson/createTaskByFile）')
            column(name: 'status', value: 'DEPRECATED')
            column(name: 'published_at', valueDate: '2026-05-22')
        }
        insert(tableName: 'api_operation') {
            column(name: 'operation_id', value: 'createTaskByJson')
            column(name: 'api_version', value: '1.0.4')
            column(name: 'http_method', value: 'POST')
            column(name: 'path_pattern', value: '/api/open/v1/tasks/vul')
            column(name: 'required_capability', value: 'TASK_WRITE')
            column(name: 'domain', value: 'TASK')
            column(name: 'openapi_tag', value: 'tasks')
            column(name: 'summary', value: '创建扫描任务（JSON 参数）')
            column(name: 'status', value: 'PUBLISHED')
            column(name: 'published_at', valueDate: '2026-05-21')
        }
        insert(tableName: 'api_operation') {
            column(name: 'operation_id', value: 'createTaskByFile')
            column(name: 'api_version', value: '1.0.4')
            column(name: 'http_method', value: 'POST')
            column(name: 'path_pattern', value: '/api/open/v1/tasks/file')
            column(name: 'required_capability', value: 'TASK_WRITE')
            column(name: 'domain', value: 'TASK')
            column(name: 'openapi_tag', value: 'tasks')
            column(name: 'summary', value: '创建扫描任务（XML 配置）')
            column(name: 'status', value: 'PUBLISHED')
            column(name: 'published_at', valueDate: '2026-05-21')
        }
        insert(tableName: 'api_operation') {
            column(name: 'operation_id', value: 'createTaskByUpload')
            column(name: 'api_version', value: '1.0.4')
            column(name: 'http_method', value: 'POST')
            column(name: 'path_pattern', value: '/api/open/v1/tasks/upload')
            column(name: 'required_capability', value: 'TASK_WRITE')
            column(name: 'domain', value: 'TASK')
            column(name: 'openapi_tag', value: 'tasks')
            column(name: 'summary', value: '创建扫描任务（上传 XML 文件）')
            column(name: 'status', value: 'PUBLISHED')
            column(name: 'published_at', valueDate: '2026-06-13')
        }
        insert(tableName: 'api_operation') {
            column(name: 'operation_id', value: 'listTasks')
            column(name: 'api_version', value: '1.0.0')
            column(name: 'http_method', value: 'GET')
            column(name: 'path_pattern', value: '/api/open/v1/tasks')
            column(name: 'required_capability', value: 'TASK_READ')
            column(name: 'domain', value: 'TASK')
            column(name: 'openapi_tag', value: 'tasks')
            column(name: 'summary', value: '分页查询任务列表')
            column(name: 'status', value: 'PUBLISHED')
            column(name: 'published_at', valueDate: '2026-05-22')
        }
        insert(tableName: 'api_operation') {
            column(name: 'operation_id', value: 'getTask')
            column(name: 'api_version', value: '1.0.0')
            column(name: 'http_method', value: 'GET')
            column(name: 'path_pattern', value: '/api/open/v1/tasks/{taskId}')
            column(name: 'required_capability', value: 'TASK_READ')
            column(name: 'domain', value: 'TASK')
            column(name: 'openapi_tag', value: 'tasks')
            column(name: 'summary', value: '查询任务进度')
            column(name: 'status', value: 'PUBLISHED')
            column(name: 'published_at', valueDate: '2026-05-22')
        }
        insert(tableName: 'api_operation') {
            column(name: 'operation_id', value: 'issuePartnerToken')
            column(name: 'api_version', value: '1.0.0')
            column(name: 'http_method', value: 'POST')
            column(name: 'path_pattern', value: '/oauth/token')
            column(name: 'required_capability', value: 'NONE')
            column(name: 'domain', value: 'AUTH')
            column(name: 'openapi_tag', value: 'auth')
            column(name: 'summary', value: '获取 Partner 访问令牌')
            column(name: 'status', value: 'PUBLISHED')
            column(name: 'published_at', valueDate: '2026-05-23')
        }
        insert(tableName: 'api_operation') {
            column(name: 'operation_id', value: 'searchInstances')
            column(name: 'api_version', value: '1.0.0')
            column(name: 'http_method', value: 'POST')
            column(name: 'path_pattern', value: '/api/open/v1/instances/search')
            column(name: 'required_capability', value: 'INSTANCE_READ')
            column(name: 'domain', value: 'INSTANCE')
            column(name: 'openapi_tag', value: 'instances')
            column(name: 'summary', value: '分页搜索漏洞实例')
            column(name: 'status', value: 'PUBLISHED')
            column(name: 'published_at', valueDate: '2026-05-23')
        }
        insert(tableName: 'api_operation') {
            column(name: 'operation_id', value: 'getInstance')
            column(name: 'api_version', value: '1.0.0')
            column(name: 'http_method', value: 'GET')
            column(name: 'path_pattern', value: '/api/open/v1/instances/{vulInfoID}')
            column(name: 'required_capability', value: 'INSTANCE_READ')
            column(name: 'domain', value: 'INSTANCE')
            column(name: 'openapi_tag', value: 'instances')
            column(name: 'summary', value: '获取漏洞实例详情')
            column(name: 'status', value: 'PUBLISHED')
            column(name: 'published_at', valueDate: '2026-05-23')
        }
        insert(tableName: 'api_operation') {
            column(name: 'operation_id', value: 'verifyInstance')
            column(name: 'api_version', value: '1.0.0')
            column(name: 'http_method', value: 'POST')
            column(name: 'path_pattern', value: '/api/open/v1/instances/{vulInfoID}/verify')
            column(name: 'required_capability', value: 'INSTANCE_VERIFY')
            column(name: 'domain', value: 'INSTANCE')
            column(name: 'openapi_tag', value: 'instances')
            column(name: 'summary', value: '验证漏洞实例')
            column(name: 'status', value: 'PUBLISHED')
            column(name: 'published_at', valueDate: '2026-05-23')
        }
        insert(tableName: 'api_operation') {
            column(name: 'operation_id', value: 'verifyInstanceBatch')
            column(name: 'api_version', value: '1.0.0')
            column(name: 'http_method', value: 'POST')
            column(name: 'path_pattern', value: '/api/open/v1/instances/verify:batch')
            column(name: 'required_capability', value: 'INSTANCE_VERIFY')
            column(name: 'domain', value: 'INSTANCE')
            column(name: 'openapi_tag', value: 'instances')
            column(name: 'summary', value: '批量验证漏洞实例')
            column(name: 'status', value: 'PUBLISHED')
            column(name: 'published_at', valueDate: '2026-05-23')
        }
        insert(tableName: 'api_operation') {
            column(name: 'operation_id', value: 'remediateInstance')
            column(name: 'api_version', value: '1.0.0')
            column(name: 'http_method', value: 'POST')
            column(name: 'path_pattern', value: '/api/open/v1/instances/{vulInfoID}/remediate')
            column(name: 'required_capability', value: 'INSTANCE_REMEDIATE')
            column(name: 'domain', value: 'INSTANCE')
            column(name: 'openapi_tag', value: 'instances')
            column(name: 'summary', value: '处置·修复（可修复）')
            column(name: 'status', value: 'PUBLISHED')
            column(name: 'published_at', valueDate: '2026-05-23')
        }
        insert(tableName: 'api_operation') {
            column(name: 'operation_id', value: 'remediateInstanceBatch')
            column(name: 'api_version', value: '1.0.0')
            column(name: 'http_method', value: 'POST')
            column(name: 'path_pattern', value: '/api/open/v1/instances/remediate:batch')
            column(name: 'required_capability', value: 'INSTANCE_REMEDIATE')
            column(name: 'domain', value: 'INSTANCE')
            column(name: 'openapi_tag', value: 'instances')
            column(name: 'summary', value: '批量处置·修复（可修复）')
            column(name: 'status', value: 'PUBLISHED')
            column(name: 'published_at', valueDate: '2026-05-23')
        }
        insert(tableName: 'api_operation') {
            column(name: 'operation_id', value: 'archiveInstance')
            column(name: 'api_version', value: '1.0.0')
            column(name: 'http_method', value: 'POST')
            column(name: 'path_pattern', value: '/api/open/v1/instances/{vulInfoID}/archive')
            column(name: 'required_capability', value: 'INSTANCE_ARCHIVE')
            column(name: 'domain', value: 'INSTANCE')
            column(name: 'openapi_tag', value: 'instances')
            column(name: 'summary', value: '处置·备案（不可修复）')
            column(name: 'status', value: 'PUBLISHED')
            column(name: 'published_at', valueDate: '2026-05-23')
        }
        insert(tableName: 'api_operation') {
            column(name: 'operation_id', value: 'verifyFixInstance')
            column(name: 'api_version', value: '1.0.0')
            column(name: 'http_method', value: 'POST')
            column(name: 'path_pattern', value: '/api/open/v1/instances/{vulInfoID}/verify-fix')
            column(name: 'required_capability', value: 'INSTANCE_VERIFY_FIX')
            column(name: 'domain', value: 'INSTANCE')
            column(name: 'openapi_tag', value: 'instances')
            column(name: 'summary', value: '修复核验（单条）')
            column(name: 'status', value: 'PUBLISHED')
            column(name: 'published_at', valueDate: '2026-05-23')
        }
        insert(tableName: 'api_operation') {
            column(name: 'operation_id', value: 'verifyFixInstanceBatch')
            column(name: 'api_version', value: '1.0.0')
            column(name: 'http_method', value: 'POST')
            column(name: 'path_pattern', value: '/api/open/v1/instances/verify-fix:batch')
            column(name: 'required_capability', value: 'INSTANCE_VERIFY_FIX')
            column(name: 'domain', value: 'INSTANCE')
            column(name: 'openapi_tag', value: 'instances')
            column(name: 'summary', value: '批量修复核验')
            column(name: 'status', value: 'PUBLISHED')
            column(name: 'published_at', valueDate: '2026-05-23')
        }
        insert(tableName: 'api_operation') {
            column(name: 'operation_id', value: 'archiveInstanceLegacy')
            column(name: 'api_version', value: '1.0.0')
            column(name: 'http_method', value: 'POST')
            column(name: 'path_pattern', value: '/api/open/v1/instances/{vulInfoID}/unfixable-records')
            column(name: 'required_capability', value: 'INSTANCE_ARCHIVE')
            column(name: 'domain', value: 'INSTANCE')
            column(name: 'openapi_tag', value: 'instances')
            column(name: 'summary', value: '处置·备案（兼容别名）')
            column(name: 'status', value: 'DEPRECATED')
            column(name: 'published_at', valueDate: '2026-05-23')
        }
        insert(tableName: 'api_operation') {
            column(name: 'operation_id', value: 'getExport')
            column(name: 'api_version', value: '1.0.0')
            column(name: 'http_method', value: 'GET')
            column(name: 'path_pattern', value: '/api/open/v1/exports/{exportId}')
            column(name: 'required_capability', value: 'EXPORT_READ')
            column(name: 'domain', value: 'EXPORT')
            column(name: 'openapi_tag', value: 'exports')
            column(name: 'summary', value: '查询外发包元数据')
            column(name: 'status', value: 'PUBLISHED')
            column(name: 'published_at', valueDate: '2026-05-23')
        }
        insert(tableName: 'api_operation') {
            column(name: 'operation_id', value: 'downloadExport')
            column(name: 'api_version', value: '1.0.0')
            column(name: 'http_method', value: 'GET')
            column(name: 'path_pattern', value: '/api/open/v1/exports/{exportId}/download')
            column(name: 'required_capability', value: 'EXPORT_READ')
            column(name: 'domain', value: 'EXPORT')
            column(name: 'openapi_tag', value: 'exports')
            column(name: 'summary', value: '下载外发文件')
            column(name: 'status', value: 'PUBLISHED')
            column(name: 'published_at', valueDate: '2026-05-23')
        }
        insert(tableName: 'api_operation') {
            column(name: 'operation_id', value: 'listTaskExports')
            column(name: 'api_version', value: '1.0.0')
            column(name: 'http_method', value: 'GET')
            column(name: 'path_pattern', value: '/api/open/v1/tasks/{taskId}/exports')
            column(name: 'required_capability', value: 'EXPORT_READ')
            column(name: 'domain', value: 'EXPORT')
            column(name: 'openapi_tag', value: 'exports')
            column(name: 'summary', value: '查询任务下的外发记录')
            column(name: 'status', value: 'PUBLISHED')
            column(name: 'published_at', valueDate: '2026-05-23')
        }
        insert(tableName: 'api_operation') {
            column(name: 'operation_id', value: 'getArtifact')
            column(name: 'api_version', value: '1.0.5')
            column(name: 'http_method', value: 'GET')
            column(name: 'path_pattern', value: '/api/open/v1/artifacts/{artifactId}')
            column(name: 'required_capability', value: 'ARTIFACT_READ')
            column(name: 'domain', value: 'ARTIFACT')
            column(name: 'openapi_tag', value: 'artifacts')
            column(name: 'summary', value: '查询扫描报告产物元数据')
            column(name: 'status', value: 'PUBLISHED')
            column(name: 'published_at', valueDate: '2026-06-23')
        }
        insert(tableName: 'api_operation') {
            column(name: 'operation_id', value: 'downloadArtifact')
            column(name: 'api_version', value: '1.0.5')
            column(name: 'http_method', value: 'GET')
            column(name: 'path_pattern', value: '/api/open/v1/artifacts/{artifactId}/download')
            column(name: 'required_capability', value: 'ARTIFACT_READ')
            column(name: 'domain', value: 'ARTIFACT')
            column(name: 'openapi_tag', value: 'artifacts')
            column(name: 'summary', value: '下载扫描报告产物文件')
            column(name: 'status', value: 'PUBLISHED')
            column(name: 'published_at', valueDate: '2026-06-23')
        }
        insert(tableName: 'api_operation') {
            column(name: 'operation_id', value: 'listTaskArtifacts')
            column(name: 'api_version', value: '1.0.5')
            column(name: 'http_method', value: 'GET')
            column(name: 'path_pattern', value: '/api/open/v1/tasks/{taskId}/artifacts')
            column(name: 'required_capability', value: 'ARTIFACT_READ')
            column(name: 'domain', value: 'ARTIFACT')
            column(name: 'openapi_tag', value: 'artifacts')
            column(name: 'summary', value: '查询任务下的扫描报告产物')
            column(name: 'status', value: 'PUBLISHED')
            column(name: 'published_at', valueDate: '2026-06-23')
        }
        insert(tableName: 'api_operation') {
            column(name: 'operation_id', value: 'listExportArtifacts')
            column(name: 'api_version', value: '1.0.5')
            column(name: 'http_method', value: 'GET')
            column(name: 'path_pattern', value: '/api/open/v1/exports/{exportId}/artifacts')
            column(name: 'required_capability', value: 'ARTIFACT_READ')
            column(name: 'domain', value: 'ARTIFACT')
            column(name: 'openapi_tag', value: 'artifacts')
            column(name: 'summary', value: '查询外发关联扫描报告产物')
            column(name: 'status', value: 'PUBLISHED')
            column(name: 'published_at', valueDate: '2026-06-23')
        }
        insert(tableName: 'api_operation') {
            column(name: 'operation_id', value: 'receivePlatformWebhook')
            column(name: 'api_version', value: '1.0.0')
            column(name: 'http_method', value: 'POST')
            column(name: 'path_pattern', value: '{partnerCallbackUrl}')
            column(name: 'required_capability', value: 'EVENT_SUBSCRIBE')
            column(name: 'domain', value: 'WEBHOOK')
            column(name: 'openapi_tag', value: 'webhooks')
            column(name: 'summary', value: '平台事件回调（Partner 实现）')
            column(name: 'status', value: 'PUBLISHED')
            column(name: 'published_at', valueDate: '2026-05-23')
        }
    }

    changeSet(id: '2026-06-23-deprecate-unimplemented-archive-operation', author: 'open-api') {
        update(tableName: 'api_operation') {
            column(name: 'status', value: 'DEPRECATED')
            where "operation_id = 'archiveInstance'"
        }
    }
}
