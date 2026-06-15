# Mock 引擎 Fixture

联调环境：`spring.profiles.active=mock`

## 模板 bundle（scanTemplateId / reportTemplateId）

| bundleId | scanTemplateId | vulnTypes | 数据来源 |
|----------|----------------|-----------|----------|
| scan-1001-vul | 1001 | 1,2 | 漏洞扫描结果1166.xml（500条） |
| scan-1001-pwd | 1001 | 3 | 弱口令扫描结果1053.xml |
| scan-1002-live | 1002 | - | report_by_vul.xml（存活探测） |
| scan-1003-port | 1003 | - | report_by_vul.xml（端口 appendix） |

`reportTemplateIds` 默认 `[2001, 2002]`，创建任务时两种报告模板均可匹配。

创建任务示例：`scanTemplateId=1001` + `reportTemplateId=2001` + `type=1` → 返回漏洞实例；`type=3` → 弱口令实例。

## 重新生成

```bash
python svmp/docs/internal/scripts/import-nsfocus-xml-to-mock-bundle.py \
  --xml .../漏洞扫描结果1166.xml --bundle-id scan-1001-vul \
  --out .../bundles/scan-1001-vul --limit 500 --profile vul \
  --scan-template-id 1001 --vuln-types 1,2
```

详见 `svmp/docs/standards/mock-data/` 与 `引擎对接与Mock模式方案.md`。

## 半人工联调（ingest-mode=manual）

Profile：`spring.profiles.active=mock-manual`（自动 include `mock`）。

| 步骤 | 说明 |
|------|------|
| 1 | Partner `POST /api/open/v1/tasks/*` 建任务，Mock 引擎保持 **RUNNING** |
| 2 | 运营 `GET /internal/admin/mock-tasks/{taskId}/dispatch-packet` 查看目标/模板/任务目录 |
| 3 | 人工在 vuln-task-center / 扫描器执行扫描，回收 Aurora XML |
| 4 | `POST /internal/admin/mock-tasks/{taskId}/import-report` 上传 XML（multipart `file`，可选 `force=true` 重导） |
| 5 | 服务写 `tasks/{taskId}/instances.json` → 任务 **FINISHED** → 实例入库 → Webhook + 外发 |

**M2 Admin API**

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/internal/admin/mock-tasks/{taskId}/bundle-status` | bundle / 入库状态 |
| POST | `/internal/admin/mock-tasks/{taskId}/preview-report` | 解析预览（不落库） |
| POST | `/internal/admin/mock-tasks/{taskId}/import-report?force=true` | 强制重导 |

运营 UI：`asset-openplatform-manage` → `/mock-manual`（Mock 半人工导入）。

**M3 Java 解析（默认）**

```yaml
open-api.engine.mock:
  xml-import-mode: java    # java | python
  xml-import-profile: auto # auto | vul | pwd | live | port
```

`java` 模式无需本机 Python；`python` 为兼容回退。导入时会按 `scanTemplateId` 校验 profile（1001→vul/pwd，1002→live，1003→port）。

配置要点（`application-mock-manual.yml`）：

```yaml
open-api.engine.mock:
  ingest-mode: manual
  data-dir: file:/data/open-api-mock   # classpath 时写入 java.io.tmpdir/open-api-mock
  import-script-path: svmp/docs/internal/scripts/import-nsfocus-xml-to-mock-bundle.py
```

E2E 脚本：`scripts/e2e-mock-manual-flow.ps1`（需本机 Python）。
