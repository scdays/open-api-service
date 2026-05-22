# open-api-service

开放平台业务服务（Nacos 注册名：`open-api-service`），Partner 身份 + 业务一体，与门户 **morningglory / clover 零耦合**。

## 架构位置

```text
Partner → partner-gateway → open-api-service → SVMP (vul-pass)
                ↑ Token 校验 Redis          ↑ SvmpEngineAdapter
                └ open-api-service 签发 Token / 写 Redis
```

## 模块结构（DDD）

```text
open-api-service/
├── pom.xml
├── src/main/java/com/vtc/openapi/
│   ├── ApplicationStart.java
│   ├── ui/                    # REST 控制器（对标 vul-pass ui）
│   │   ├── open/OpenTaskUI           # /api/open/v1/tasks
│   │   ├── auth/PartnerTokenUI       # /oauth/token, /internal/token/introspect
│   │   └── admin/PartnerAdminUI      # /internal/admin/partners
│   ├── app/service/           # 应用服务
│   ├── domain/                # 领域实体
│   ├── adapter/               # SvmpEngineAdapter → Feign 调 SVMP
│   ├── infra/                 # DAO、Feign、配置
│   ├── common/                # PartnerContext、错误码
│   └── web/                   # 全局异常、Filter、通用 DTO
├── src/main/resources/
│   ├── bootstrap.yml
│   ├── application.yml
│   └── db/mysql/              # Liquibase
└── scripts/curl-verify.sh
```

## P0 接口状态

| 方法 | 路径 | 状态 |
|------|------|------|
| POST | `/oauth/token` | 🔧 骨架（PartnerTokenUI） |
| POST | `/internal/token/introspect` | 🔧 骨架 |
| POST | `/api/open/v1/tasks` | ✅ OpenTaskUI |
| GET | `/api/open/v1/tasks/{taskId}` | ✅ |
| GET | `/api/open/v1/tasks` | ✅ |
| * | `/internal/admin/partners/*` | 🔧 骨架 |

Partner 上下文**仅**来自请求头 `X-Partner-Id`（由 **partner-gateway** 注入）。

## Swagger / Knife4j

对齐 **clover**（`io.swagger.annotations` + spore `knife4j-micro-spring-boot-starter`）。

| 项 | 值 |
|----|-----|
| 文档 UI | `http://{host}:{port}/doc.html`（默认 `http://127.0.0.1:35780/doc.html`） |
| OpenAPI JSON | `/v2/api-docs` |
| 配置类 | `infra/config/OpenApiSwaggerConfig.java` |
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

### 直连本服务（联调）

```bash
export BASE=http://127.0.0.1:35780
export PARTNER_A=partner-demo-01
bash scripts/curl-verify.sh
```

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
