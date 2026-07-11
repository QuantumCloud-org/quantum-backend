# quantum-backend · 技术栈 / 阻碍 / 完善设计

> 2026-07-11 由全景对账产出。配套阅读: [ai-sprint-design.md](ai-sprint-design.md) (§8 阶段表) ·
> [docs/ai/convention-pack/runtime-env.md](ai/convention-pack/runtime-env.md) · `.ai_state/proposals.md`。
> 总路线 (跨项目): **① 完善 quantum (本仓优先) → ② Rlues 9.9.1 双端收尾 → ③ AI infra (S5 chat 产品, 独立项目)**。

## 技术栈

| 层 | 选型 |
|---|---|
| 语言/运行时 | Java 25 LTS |
| 框架 | Spring Boot (servlet 栈) + spring-boot-starter-actuator (health/prometheus 已暴露) |
| ORM | MyBatis-Flex (+ processor 生成 TableDef) |
| 数据库 | PostgreSQL 16.x (`deploy/init.sql` 全量初始化, 库 `baseweb`) |
| 缓存 | L1 Caffeine + L2 Redis (双级, dev 需本机 6379) |
| 构建 | Maven 多模块 (`${revision}` 版本): quantum-server / quantum-common-{framework,orm,security,cache,logging,file} / quantum-biz-system / quantum-mcp |
| 认证/授权 | 自研 JWT (TokenService) + RBAC (`@RequiresPermission`) + 数据权限 (`@DataScope` + DataScopeAspect/Interceptor, **fail-closed**: 无用户上下文 DENY_ALL, `SystemDataScope` 逃生门) |
| AI 契约 | 契约① Convention Pack (`docs/ai/convention-pack/`, dev-time codegen) + 契约② `quantum-mcp` (OAuth 2.1 + PKCE + MCP JSON-RPC 只读能力, 默认 `ai.mcp.enabled=false`) |
| 测试 | JUnit 5 + Mockito (root pom 继承); 全量 `mvn -pl quantum-server -am test` = 11 模块 Reactor |

## 当前阻碍 (按硬度排序)

1. **[硬·环境] 本机 PostgreSQL(5432) + Redis(6379) 未运行** — 一切动态验证的总闸:
   boot 200 证据 (runtime-env.md 标 BLOCKED)、OAuth 全链冒烟 (mcp-test-access.md §4)、F7 真·动态 E2E 全部因此搁置。
   `deploy/` 目前只有 init.md + init.sql, **无 docker-compose 一键环境**。
2. **[待立项] F7/S6 真·动态 E2E** — FE(mock off) + BE 起服 + agent OAuth→consent→token→`/mcp` tools/call 全链 +
   playwright 证据。依赖阻碍 1; 是"提示词+框架直接出代码和效果"的终验 (§8 已挂 S6)。
3. **[债·安全对称性] orm `DEPT`/`SELF` case 字段级 fail-open** — deptId/userId 为 null 时不追加过滤,
   与 DEPT_AND_CHILD/CUSTOM 的 `1=0` fail-closed 不对称 (pre-existing, 生产路径值恒在, 风险低;
   proposals.md 2026-07-10 条目)。
4. **[债·流程] G4 类门禁均为文本级** — 构建产物级校验 (如 FE 的 production grep) 未进 CI (proposals)。

## 完善设计 (阶段 ① quantum 优先, 建议顺序)

| 步 | 内容 | 解锁 |
|---|---|---|
| 1 | **补 `deploy/docker-compose.yml`** (postgres16 + redis, 挂 init.sql 自动初始化) — 把阻碍 1 从"手动装中间件"降为一条命令 | 一切动态验证 |
| 2 | 起环境后**补两笔欠据**: boot + `curl /actuator/health` 200 (回填 runtime-env.md 校准段) + MCP OAuth 冒烟 (mcp-test-access.md §4 三条 curl) | 清 BLOCKED 标注 |
| 3 | **F7/S6 sprint** (跨三仓): BE `ai.mcp.enabled=true` + 测试账号 → cowork `McpToolProvider` 消费 `/mcp` → FE mock-off 联调 → playwright 证据链 | 终验交付 |
| 4 | orm 对称加固小 sprint: DEPT/SELF null 分支补 `1=0` (TDD, 影响面 = 2 个 case 分支) | 清阻碍 3 |
| 5 | 业务生产: 用 `scaffold-module-gen` 生成真实业务模块 (S2 已证零返工), 权限 SQL 走 menu-permission 模板 | 业务迭代提速 |

> 阶段 ② (Rlues) 与 ③ (AI infra) 的对应文档: `Rlues/docs/blockers-and-roadmap.md` ·
> S5 chat 产品单开项目维护 (平面 C, 接口已由 S3 preflight 冻结, 本仓无需再动)。
