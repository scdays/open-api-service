# open-api-service

开放平台业务服务（Nacos 注册名：`open-api-service`），Partner 身份 + 业务一体，与门户 **morningglory / clover 零耦合**。

## 架构位置

```text
Partner → partner-gateway → open-api-service → SVMP (vul-pass)
                ↑ Token 校验 Redis          ↑ SvmpEngineAdapter
                └ open-api-service 签发 Token / 写 Redis
```

## 模块结构（执行平面 + 治理平面）

```text
open-api-service/
├── pom.xml
├── src/main/java/com/vtc/openapi/
│   ├── ApplicationStart.java
│   ├── ui/open/OpenTaskUI.java        # 薄层，仅调 InvocationPipeline
│   ├── ui/auth/PartnerTokenUI.java    # Partner/Token（Agent D，勿破坏）
│   ├── ui/admin/PartnerAdminUI.java
│   ├── governance/                    # 治理平面 P0
│   │   ├── ApiCatalogService.java     # api_operation 校验
│   │   ├── InvocationService.java     # api_invocation 写入
│   │   ├── InvocationContext.java
│   │   └── OpenApiOperations.java
│   ├── pipeline/InvocationPipeline.java
│   ├── handler/TaskHandler.java       # 任务执行平面
│   ├── adapter/SvmpEngineAdapter*.java
│   ├── infra/dao/                     # open_task、api_invocation 等
│   └── web/                           # RequestIdFilter、PartnerContextFilter
├── src/main/resources/db/mysql/
│   ├── api_operation.groovy           # P0 种子 createTask/listTasks/getTask
│   └── api_invocation.groovy
└── scripts/curl-verify.sh
```

详见 [开放平台API治理与调用生命周期](../../../svmp/docs/internal/开放平台API治理与调用生命周期.md)。

## P0 接口状态（Partner 身份与 Token）

| 方法 | 路径 | 状态 |
|------|------|------|
| POST | `/oauth/token` | ✅ `client_credentials` + JWT + Redis `partner:token:{sha256}` |
| POST | `/api/open/v1/oauth/token` | ✅ 同上（别名） |
| POST | `/internal/token/introspect` | ✅ partner-gateway 降级 |
| POST | `/internal/admin/partners` | ✅ Liquibase + MyBatis |
| PUT | `/internal/admin/partners/{partnerId}` | ✅ |
| POST | `/internal/admin/partners/{partnerId}/credentials` | ✅ BCrypt 哈希，明文仅一次 |
| GET | `/internal/admin/partners/{partnerId}/credentials` | ✅ 不含 secret |
| POST | `/api/open/v1/tasks` | ✅ Pipeline + TaskHandler + `api_invocation` |
| GET | `/api/open/v1/tasks/{taskId}` | ✅ |
| GET | `/api/open/v1/tasks` | ✅ |

内网管理 API 鉴权：请求头 `X-Internal-Admin-Key`（`open-api.admin.api-key`）。

Partner 上下文**仅**来自请求头 `X-Partner-Id`（由 **partner-gateway** 注入）。

## Swagger / Knife4j

对齐 **clover**（`io.swagger.annotations` + spore `knife4j-micro-spring-boot-starter`）。

| 项 | 值 |
|----|-----|
| 文档 UI | `http://{host}:{port}/doc.html`（默认 `http://127.0.0.1:35780/doc.html`） |
| OpenAPI JSON | `/v2/api-docs` |
| 配置类 | `com.botany.spore.core.config.Swagger2Config`（`esmp-starter-core` 自动装配） |
| 开关 | `knife4j.enable=true`（`application.yml`） |

Controller 使用 `@Api` / `@ApiOperation`；请求/响应 DTO 使用 `@ApiModel` / `@ApiModelProperty`（参考 clover `PartnerTokenUI`、`ApiResponse`）。

## 编译

```bash
mvn compile -DskipTests
```

## 验证

### 经 partner-gateway（推荐）

1. `POST https://{open域名}/oauth/token` 换 Token
2. `GET https://{open域名}/api/open/v1/tasks`，Header: `Authorization: Bearer {token}`

### Partner 身份与 Token（直连本服务）

```bash
export BASE=http://127.0.0.1:35780
export ADMIN_KEY=dev-internal-admin-key-change-in-prod
bash scripts/curl-partner-auth.sh
```

### 任务 API 隔离（直连本服务）

```bash
export BASE=http://127.0.0.1:35780
export PARTNER_A=partner-demo-01
bash scripts/curl-verify.sh
```

## P1 Backlog（Partner 身份）

| 项 | 说明 |
|----|------|
| 凭证轮换 | `POST .../credentials/{credentialId}/rotate`，双密钥过渡期 |
| 凭证吊销 | `DELETE .../credentials/{credentialId}` |
| 会话吊销 | `DELETE .../partners/{partnerId}/sessions`，清理 Redis `partner:token:*` |
| Token 吊销 | `POST /internal/token/revoke` |
| Partner 逻辑删除 | `DELETE /internal/admin/partners/{partnerId}` |

## SVMP 引擎配置

```yaml
open-api:
  svmp:
    engine-service-name: vul-pass
    engine-path-prefix: ""
    dispatch:
      order-id: "<vul-pass 已存在指令 ID>"
```

相关文档：[联调手册-P0](../../../svmp/docs/internal/联调手册-P0.md) · [partner-gateway与open-api-service-模块与接口清单](../../../svmp/docs/internal/partner-gateway与open-api-service-模块与接口清单.md)
