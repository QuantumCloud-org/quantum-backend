# Quantum Backend Architecture

## 当前形态

Quantum Backend 是 Spring Boot + Maven 多模块的模块化单体。

最近更新: 2026-07-31T02:00:00Z

| 模块 | 职责 |
|---|---|
| `quantum-common-framework` | 通用实体、上下文、异常、工具 |
| `quantum-common-security` | Spring Security、JWT、refresh cookie、CORS、限流、防重复提交 |
| `quantum-common-orm` | 分页、数据权限上下文、MyBatis-Flex 支撑 |
| `quantum-common-cache` | Redis / local cache 抽象 |
| `quantum-common-logging` | 操作日志、登录日志、日志分页 |
| `quantum-common-file` | 文件校验、本地存储、RustFS/S3 兼容存储、Excel |
| `quantum-biz-system` | 用户、角色、菜单、部门、字典、文件记录等系统域 |
| `quantum-mcp` | MCP OAuth 授权、Manifest v1、只读 tool 适配 |
| `quantum-server` | 启动入口与 profile 配置 |

## 依赖治理

单一治理入口: root `pom.xml` 的 `<parent>` = `spring-boot-starter-parent`, 由它继承 Spring Boot BOM 的 dependencyManagement。

- **不再重复 import** `spring-boot-dependencies` / `netty-bom` / `jackson-bom`。Boot BOM 的优先级高于同级 import 的组件 BOM, 重复导入会让 property 值与实际解析版本长期不一致 (2026-07-30 实测: 属性写 Jackson 3.2.0 / Netty 4.2.14, 实际解析 3.1.4 / 4.2.15)。
- **组件版本用 Boot 官方覆盖属性调整**: `jackson-bom.version`、`jackson-2-bom.version`、`netty.version`、`tomcat.version`、`hikaricp.version`、`postgresql.version`。
- **例外**: 不受 Boot BOM 管理的组件 (MyBatis-Flex BOM、Hutool、Redisson、AWS SDK S3、springdoc、ip2region、fastexcel 等) 仍在 root `dependencyManagement` 显式钉版本。
- **验收口径**: 版本升级的完成证据是 effective POM / `dependency:list` 的**实际解析版本**, 不是 property 文本值。只改属性不改解析结果 = 未完成。

## 安全与权限现状

- 浏览器跨域由生产白名单配置控制, credentials 模式不反射任意 Origin。
- 数据权限运行态以登录用户 `LoginUser.dataScope/deptIds` 为入口; 用户登录时优先由有效角色聚合生成。
- 用户导入属于系统域写路径, 复用用户新增/编辑 DTO 校验并执行部门数据域 guard。

## AI 协作基座

- 生成期: `docs/ai/convention-pack/` (模板数据权限默认启用 + validate.md G1-G4 安全门禁) + scaffold-module-gen skill。
- 运行期: `quantum-mcp` 已提供 S3 skeleton (OAuth 2.1 public client + PKCE, 独立 token store, Bearer filter, Manifest v1, 只读 tools)。
- chat/RAG/Provider 等已定案移出本仓库。

## 子系统索引

- [auth-security.md](auth-security.md)
- [ai-collaboration.md](ai-collaboration.md)
- [auth-mcp-oauth.md](auth-mcp-oauth.md)
