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
