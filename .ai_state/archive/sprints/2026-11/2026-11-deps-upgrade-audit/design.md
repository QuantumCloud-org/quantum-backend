---
sprint: "2026-11-deps-upgrade-audit"
path: "System"
created: "2026-11-18"
req: ".ai_state/requirements/deps-upgrade-and-audit-2026-11.md"
roadmap: ""
item: ""
base_commit: "8176e44"
branch: "chore/deps-upgrade-audit-2026-11"
worktree: "/Users/mi_manchi/workspace/quantum/quantum-backend-wt-deps"
review_ignore: []
---

# Design — 2026-11-deps-upgrade-audit

## 背景

见 req。基线 = 8176e44 + 工作区未提交的 pom 改动（Boot 4.1.2、tomcat 11.0.27、netty 4.2.19、jackson 3.2.4/2.22.4、postgresql 42.7.14、hutool 5.8.48、删 spring-jcl 与 commons-logging 排除）。

registry 核验（repo1.maven.org maven-metadata.xml `<release>`，2026-11-18 02:21 UTC）：

| 组件 | 基线 | 目标 |
|---|---|---|
| spring-boot-starter-parent | 4.1.2 | 4.1.2（4.2.0 仅 RC2，不升） |
| tomcat | 11.0.27 | 11.0.29 |
| netty-bom | 4.2.19.Final | 4.2.21.Final |
| tools.jackson:jackson-bom | 3.2.4 | 3.3.0 |
| com.fasterxml.jackson:jackson-bom | 2.22.4 | 2.23.0 |
| nimbus-jose-jwt | 10.9.1 | 10.10 |
| redisson | 4.6.1 | 4.8.0 |
| caffeine | 3.2.4 | 3.3.1 |
| springdoc | 3.0.3 | 3.2.0 |
| ip2region | 3.3.7 | 3.3.8 |
| awssdk s3 | 2.49.6 | 2.59.3 |
| postgresql / mybatis-flex / disruptor / HikariCP / hutool / fastexcel | — | 已最新 |

## 方案

- 写者：Codex `gpt-6.2-sol`（用户点名），隔离 worktree `../quantum-backend-wt-deps`，分支 `chore/deps-upgrade-audit-2026-11`；逐组升级、每组编译，不兼容者回退并记原因。
- 再由写者做代码现代化（弃用 API、Java 25 写法）与安全/性能/写法审查，低风险问题直接修，其余进报告。
- 第二轮（用户 2026-11-18 扩范围）：低效、安全问题、重复实现、垃圾代码全部重写或重构。只读 Opus 审查清单 + Codex 第一轮 audit-report 合并后，交 Codex gpt-6.2-sol 在同一 worktree 执行；按清单逐条处理，行为改动先补测试。
- 收尾整体复核（用户 2026-11-18 指定）：重构完成后用 Codex gpt-6.2-astra 只读通读整个 diff 并跑全量构建，交回发现项；之后主 agent 复跑 `mvn clean test`，再派独立 reviewer 审。
- 备选：只升 patch、minor 出报告 → 用户否决（2026-11-18 选全部升）。备选：去掉 jackson/tomcat/netty 覆盖、交回 Boot BOM → 会降级到 Boot 4.1.2 自带的旧版，与安全刷新目标冲突，不采用。

## 验收标准

- AC1: 上表组件均升到目标版本或在 `upgrade-report.md` 写明回退原因；无预发布版。验证: athena run --covers AC1 -- mvn -q -DskipTests validate
- AC2: `mvn clean test` 全模块退出码 0。验证: athena run --covers AC2 -- mvn clean test
- AC3: 升级引入的 breaking/弃用有最小修复；项目代码编译无 deprecation 告警新增。验证: athena run --covers AC3 -- mvn clean compile
- AC4: `audit-report.md` 覆盖安全/性能/写法，每条含 文件:行、严重度、处置；已修项随 AC2 测试通过。验证: athena run --covers AC4 -- mvn clean test
- AC5: 合并清单里的低效/安全/重复/垃圾项（P0-P2）全部已修，或写明不修的原因；重构后测试全绿。验证: athena run --covers AC5 -- mvn clean test
- AC6: 用户批准的高风险项 S17（按下方契约）、D03（DelegatingPasswordEncoder；遗留的 username+password 哈希在登录时升级）、S19（Redis codec 加类型白名单）、S23（审计表清空仅限超管）都已实现并有测试；S06（yml 凭证）按用户裁定不动。验证: athena run --covers AC6 -- mvn clean test
- AC7: 独立 review 放行（H3）。

## 允许写集

全部模块 `pom.xml`、`src/main/**`、`src/test/**`、`src/main/resources/**`；本 sprint 目录。

## 不做

Boot 4.2 RC；S06 仓库凭证（用户 2026-11-18：不要动）；2026-07-30 sprint 的安全 writeup；推送远端；不加无调用方抽象/配置开关。
## S17 刷新契约（前后端共用，2026-11-18 主 agent 定）
- 后端新增 `POST /auth/refresh`（permitAll，不需要 access token）：从 HttpOnly refresh cookie 读取 refresh token 并轮换。成功返回 200，body 复用 /auth/login 的 LoginResponse 中 token 相关字段（accessToken、expireTime 等，以后端现有类为准；前端只依赖 accessToken），同时设置 `Authorization: Bearer <access>` 响应头，并下发新的 refresh cookie。失败返回 401，并清除 cookie。
- refresh cookie 的 Path 改为配置项 `security.refresh-cookie-path`，默认值 `/api/auth`。浏览器侧路径带 /api 前缀，这个 Path 能同时覆盖 /api/auth/refresh、/api/auth/logout 和 /api/auth/login。写新 cookie 时，再下发一条 `Path=/; Max-Age=0` 的同名 cookie，清掉旧路径的 cookie。
- `TokenAuthenticationFilter` 不再自动续期：access token 无效时直接按未认证处理，返回 401。
- 前端：除 login 和 refresh 外，任何请求收到 401 都先单飞（并发请求共享同一个 Promise）调用一次 `/auth/refresh`，成功后用新 token 重放原请求，每个请求最多重放 1 次；refresh 失败才执行 reset 并跳转登录页。应用启动时如果没有 access token，先尝试调用一次 refresh。
- 生产 cookie Path（用户 2026-11-18 裁定）：application-prod.yml 只加一行 `security.refresh-cookie-path: /talent_manger/auth`，其他配置和凭证不动。由主 agent 在第二轮结束后写入。
