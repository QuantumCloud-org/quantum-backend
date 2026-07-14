# AI Sprint 设计资料（重整版）

> 状态：System / Design（业务源码未改，待评审通过后进入实现）
> 本文替代 codex 上一版 AI Sprint 的定位假设。核心结论：**quantum-backend 不含任何 AI 运行时**，
> 它只是"AI 作用的目标之一"。上一版把 chat / RAG / SSE / Provider SPI / token 配额塞进 quantum-backend
> 是定位错误，本文将其明确剥离到下游产品与提示词层。

---

## 0. 一句话定位

- **quantum-backend** = 企业 web 框架，承载内部业务的展示界面。**不涉及任何 AI 功能**。
- AI 只在两处"作用于"它，但都不是它的内置能力：
  1. **生成期（dev-time）**：用 AI + skill 快速生成模块 / 界面 → 快速部署。
  2. **运行期读取（runtime read）**：用 **MCP** 让模型读取它的数据与业务流程。
- **chat / 对话式 AI 是另一个独立项目**，以后作为下游消费者接入下面的 CLI / MCP，不在本仓库范围内。
- 抽象出来的 **CLI 与 MCP 最终是提示词架构（aether / pace）里的两个 skill**，与既有的 vm 叠加。
- **quantum-backend 不是唯一目标**：还有 quantum-front，以及未来 B 端客户**自带的脚手架**。
  因此 CLI / MCP 必须**脚手架无关（scaffold-agnostic）**。

---

## 1. 三平面架构

AI 相关的一切分布在三个平面，quantum-backend 只处在中间平面，且只承担两个被动契约。

```
┌────────────────────────────────────────────────────────────────────┐
│ 平面 A — 提示词 / Agent 智能层（aether / pace，你的个人提示词架构）     │
│   · vm（已有）                                                        │
│   · skill ①: scaffold-module-gen —— 基于需求 + 框架约定，快速生成模块   │
│   · skill ②: project-data-reader —— 通过 MCP 读取运行中项目的数据能力  │
│   两个 skill 都是【目标无关】的，只认下面两份契约，不认具体脚手架        │
│   均遵循官方 Agent Skills 规范（SKILL.md + frontmatter），可直接装进    │
│   你的 aether/pace，与 vm 叠加                                          │
└───────────────┬──────────────────────────────┬───────────────────────┘
     生成期：读约定包、生成代码、跑校验          运行期：连 MCP、带身份读数据
                │                                │
┌───────────────▼──────────────────────────────▼───────────────────────┐
│ 平面 B — 目标脚手架层（生成 & 能力目标，可插拔多个）                    │
│                                                                        │
│   quantum-backend（你的 BE）   quantum-front（你的 FE）   客户自有脚手架  │
│   每个目标"参与"AI 只需提供两份契约的实现：                              │
│     契约①  Convention Pack（约定包）   → 供 scaffold-module-gen 消费     │
│     契约②  Capability Manifest + MCP  → 供 project-data-reader 消费      │
│   quantum-backend 在本平面【只做这两件事，没有任何 AI 运行时】           │
└────────────────────────────────────────────────────────────────────┘
                                │  被消费
┌───────────────────────────────▼───────────────────────────────────────┐
│ 平面 C — 下游 AI 产品层（独立项目，本 Sprint 不做）                     │
│   chat 产品 / 其他终端 AI 应用                                          │
│   · Provider SPI（Claude 官方 SDK / OpenAI-compatible 可切）           │
│   · Chat 编排、RAG（pgvector）、SSE、token 配额、会话审计               │
│   作为平面 B 的 MCP 消费者接入，身份仍由平面 B 各系统裁决               │
└────────────────────────────────────────────────────────────────────┘
```

**关键认知**：codex 上一版把"平面 C 的东西"当成"平面 B 的 quantum-biz-ai 模块"来设计了。
本 Sprint 只交付 **平面 B 中 quantum-backend 的两份契约实现** + **平面 A 两个 skill 的接口约定**。

---

## 2. 两份契约（可复用核心，也是"支持客户自有脚手架"的关键）

契约是**脚手架无关的接口**；每个目标脚手架各自提供一份实现。skill 只依赖契约，不依赖具体脚手架，
所以将来接第二个脚手架（quantum-front / 客户脚手架）时，skill 一行不改。

### 契约① Convention Pack（约定包）—— 面向代码生成

一个脚手架发布的、机器可读 + 文档化的"怎么给我正确生成代码"说明。

| 组成 | 内容 | quantum-backend 的实现 |
|---|---|---|
| `SKILL.md` 风格约定文档 | 模块结构、分层、命名、权限/数据权限约定 | biz 模块 + common 六件套结构；controller/service/mapper/DTO/convert 模式；`@RequiresPermission` / `@DataScope` 用法；MyBatis-Flex 惯用法；测试约定 |
| 代码模板 | 骨架文件的占位模板 | entity / mapper / service / serviceImpl / controller / DTO / convert / 菜单权限 SQL |
| 校验命令 | 生成后闭环校验 | `mvn -q -pl <module> -am compile`（编译兜底） |
| 能力清单映射 | 生成的接口如何登记权限点 / 菜单 | 权限点命名规则 `system:<entity>:<action>` + 菜单落库脚本 |

- quantum-front 的实现：组件/页面/路由/状态约定；校验 `pnpm build` 或 `tsc --noEmit`。
- 客户脚手架：他们按同一份契约结构自备。

### 契约② Capability Manifest + MCP —— 面向运行期读取

每个想把"数据 / 业务流程"开放给模型的目标系统，暴露一个 MCP Server。

| 组成 | 内容 | quantum-backend 的实现 |
|---|---|---|
| MCP tool schema | 可读能力的入参 / 出参 / 语义描述 | 读用户、读部门树、读字典、读某业务流程状态、读报表等（只读优先） |
| 认证 | 每次调用的身份来源 | 用户 JWT 透传，进入 quantum-backend 后重建 `LoginUser` |
| 授权 | 每个能力对应的权限点 | 复用 `@RequiresPermission` |
| 数据过滤 | 行级数据权限 | **必须穿过现有 `DataScope` 链**（见 §5 铁律） |
| 审计 | 能力调用留痕 | 复用操作日志 + `@Sensitive` 脱敏 |

---

## 3. quantum-backend 在本仓库的落地范围（收窄为两件事）

本 Sprint 在 **quantum-backend 仓库内**只交付：

### 3.1 Convention Pack（契约①实现）—— dev-time，纯文档 + 模板，零运行时代码

- 目录：`docs/ai/convention-pack/`
  - `SKILL.md`：quantum-backend 模块生成约定（可直接被 aether/pace 的 scaffold-module-gen skill 引用）
  - `templates/`：各层骨架模板
  - `validate.md`：校验命令与自修流程说明
- 产物不进 jar，不影响运行时。

### 3.2 MCP Capability Adapter（契约②实现）—— 设计现在定，实现分期

- 新模块：`quantum-mcp`，**与 biz 模块平级**（它依赖 biz service 接口，故不能放在 quantum-common 下——
  common 层不得依赖 biz 层）。依赖方向：`quantum-mcp` → biz service 接口；biz 不反向依赖它。
- **协议：走标准 MCP 协议**（决策已定），通用 agent（Claude Desktop / 你的 aether/pace / 第三方）都能连：
  - 唯一服务端传输：**Streamable HTTP / SSE**，内嵌在 Spring 应用中暴露。
  - 本地 stdio 场景：**不做第二个服务端实现**，用桥接进程（mcp-remote 模式：stdio ⇄ HTTP 转发）。
    理由：独立 stdio 进程要么绕过 servlet 安全链、要么重复实现认证，二者都不可接受；
    桥接保证**认证与数据权限只有一条执行路径**。
- 形态：MCP server 逻辑 **in-process 跑在 Spring 应用内**（这样才能复用 `PermissionAspect` + `DataScope`，
  不复制任何业务/权限逻辑）；对外以标准 MCP 协议暴露，不是私有 REST。
- 开关：`ai.mcp.enabled=false` 默认关闭，不给不需要的部署引入表面积。
- SSE 端点需从既有安全链排除防重复提交/限流的误杀（长连接），但仍走 Token 认证 + 数据权限。

**S3 实现的两条硬性技术要求**（源于对现有切面机制的核实，实现者不得省略）：

1. **线程上下文 fail-closed**：`PermissionAspect` / `DataScopeAspect` 依赖 `UserContext`（ThreadLocal）。
   经核实，`DataScopeAspect` 在拿不到用户时会**静默跳过**、`applyDataScope` 退化为 no-op —— 即
   **无用户上下文 = 数据权限被旁路（fail-open）**。因此 MCP tool handler 必须满足其一：
   在完成认证的请求线程上同步执行；或显式把 `LoginUser` 传递到执行线程并重建 `UserContext`。
   且适配层入口必须自带断言：`UserContext.getUser() == null` 时直接拒绝调用，不进业务层。
2. **脱敏不可旁路**：`@Sensitive` 依赖应用配置的 Jackson 序列化器。MCP SDK 自带的序列化
   不会经过它，脱敏会**静默失效**。适配层必须先用应用的 `ObjectMapper`（`JsonUtil`）把 VO
   序列化为 JSON 文本，再交给 MCP 响应，禁止把实体/VO 对象直接交给 MCP SDK 序列化。

---

## 4. 明确移出 quantum-backend 的清单（给 codex 的纠正项）

以下 codex 上一版放进 quantum-backend 的内容，**全部移出本仓库**，归入平面 C（下游 chat 产品）或平面 A（提示词层）：

| 项目 | 原归属（错） | 正确归属 |
|---|---|---|
| `quantum-biz-ai` 业务模块 | quantum-backend 模块 | ❌ 删除，不存在于本仓库 |
| Provider SPI（Claude / OpenAI 切换） | quantum-backend | 平面 C 下游产品 |
| SSE 流式接口 + 与安全链冲突处理 | quantum-backend | 平面 C 下游产品 |
| pgvector RAG、向量表 | quantum-backend | 平面 C 下游产品 |
| token 配额 / 计量 | quantum-backend | 平面 C 下游产品 |
| "拆微服务触发条件" | quantum-backend 架构 | ❌ 作废（本就分属不同平面/项目，无需再讨论拆分） |
| Tool/MCP 演进 | 混在 biz-ai | 收敛为契约②（`quantum-mcp` 只读适配） |

保留并重构的只有：**契约②的 MCP 只读能力适配 + 其审计部分**。

---

## 5. 安全铁律（不可协商）

1. **身份与数据权限永远留在目标系统一侧**。scaffold-module-gen / project-data-reader / 下游 chat 产品都**不得**自建一套权限。
   - MCP 每次能力调用：JWT 透传 → quantum-backend 重建 `LoginUser` → 走 `@RequiresPermission` + `DataScope`。
   - 一旦 MCP 适配层绕过 `DataScope` 直接查库，之前修复的 fail-closed 数据权限（含越权写修复）全部失效。
2. **MCP 默认只读**。写操作若开放，必须逐个能力显式声明，并复用写操作数据权限校验（`assertTargetUserWritable` 等）。
3. **codegen 模板定骨架，AI 填业务语义，编译器兜底**。不让 AI 从零凭记忆写整模块（会漂移、违反约定）。
4. **MCP 输出复用 `@Sensitive` 脱敏**；API Key / 凭证走环境变量或 KMS，禁止入库、入 yml、入 prompt。
5. **prompt 注入面**：MCP 返回的业务数据可能进入模型上下文，下游产品需把"数据"与"指令"分离，不信任数据内容里的指令。

---

## 5.5 两个 Skill 的边界（必须分清，遵循官方 Agent Skills 规范）

两个 skill 是**不同物种**，混在一起会同时污染两边。用一张表钉死边界；各自是一个独立的
`SKILL.md`（官方格式：YAML frontmatter `name` + `description`，正文 markdown），装进 aether/pace 即可。

| 维度 | ① `scaffold-module-gen` | ② `project-data-reader` |
|---|---|---|
| 一句话 | **基于需求 + 框架约定，快速生成项目模块** | **通过 MCP 读取运行中项目的数据能力** |
| 时机 | dev-time（写代码 / 部署前） | runtime（项目已跑起来） |
| 作用对象 | 目标脚手架的**源码仓库** | 目标系统的**运行实例** |
| 输入 | 需求描述 + 目标脚手架 id | MCP endpoint + 操作者身份（JWT） |
| 依赖契约 | 契约① Convention Pack | 契约② Capability Manifest + MCP |
| 产物 | 可编译的模块骨架 + 菜单/权限 SQL | 结构化业务数据（只读） |
| 是否碰运行时数据 | 否 | 是（穿权限 + 脱敏） |
| 是否碰源码 | 是（生成/修改文件） | 否 |

- 官方 SKILL.md frontmatter 示例（两个 skill 各一份）：
  ```yaml
  ---
  name: scaffold-module-gen
  description: >-
    Use when the user wants to generate a new business module or page in an
    existing enterprise scaffold (backend or frontend) from a requirement.
    Reads the target scaffold's Convention Pack, generates code from templates,
    then closes the loop with the scaffold's compile/build command.
  ---
  ```
  ```yaml
  ---
  name: project-data-reader
  description: >-
    Use when you need to read live data or business-process state from a
    running project (users, departments, dictionaries, workflow status,
    reports) through its MCP capability server, with the caller's identity and
    data-permission enforced by the target system.
  ---
  ```
- ~~本仓库提供两个 skill 的**参考起点**于 `docs/ai/skills/`~~（**2026-07-14 已清理**：孵化使命完成，
  两 skill 已迁入 Rlues 并演化为 9.9.3 的 `quantum-codegen` 六 mode 与 `quantum-data`，唯一真相源在
  `Rlues/vibeCoding/`，本仓库不留副本）；quantum-backend 的 Convention Pack 于 `docs/ai/convention-pack/`
  （属平面 B，本仓库的活，长期保留）。

---

## 6. scaffold-module-gen skill 工作流（你的 #1：快速生成模块 / 界面）

```
输入：目标脚手架 id（quantum-backend / quantum-front / <客户>） + 需求描述
  │
  1. 加载该脚手架的 Convention Pack（约定 + 模板）
  2. AI 按约定生成代码：
       后端：entity / mapper / serviceImpl / controller / DTO / convert / 权限点 / 菜单 SQL
       前端：page / api / route / 组件
  3. 运行 Convention Pack 声明的校验命令（mvn compile / pnpm build）
  4. 校验失败 → AI 读编译错误自修 → 回到 3，闭环
  5. 产出：可编译的模块骨架 + 菜单/权限落库 SQL
```

- 原则：**结构确定性交给模板，业务语义交给 AI，正确性交给编译器**。
- 与 aether/pace 集成：本流程整体打包为一个 skill（`SKILL.md` + 引用各脚手架的 Convention Pack），叠加在 vm 之上。
- 多脚手架：skill 不变，换 Convention Pack 即可对接 quantum-front / 客户脚手架。

---

## 7. project-data-reader skill 工作流（你的 #2：把系统能力开放给模型）

```
输入：目标系统 MCP endpoint + 操作用户身份（JWT）
  │
  1. 连接目标系统的 MCP Server，拉取 Capability Manifest（可用工具清单）
  2. 模型按需调用只读能力（读用户 / 部门 / 字典 / 业务流程 / 报表）
  3. 每次调用：目标系统侧验签身份 → 重建 LoginUser → 过权限 + 数据权限 → 脱敏 → 审计
  4. 结果回给模型；数据与指令分离
```

- MCP Server 归属：**目标系统内（quantum-backend 的 `quantum-mcp` 模块）**，不在 skill 侧、不在下游产品侧。
- skill 只认 MCP 协议 + manifest，天然支持多目标系统。

---

## 8. 分阶段落地顺序

| 阶段 | 交付 | 状态 |
|---|---|---|
| S0（本 Sprint，文档） | 本设计资料 + 两份契约结构定稿 + 两个 skill 起点 + BE Convention Pack 骨架 | ✅ 本 PR |
| S1 | 补全 quantum-backend Convention Pack（模板全层齐套 + menu SQL 模板） | ✅ 本 PR（runtime-verify 实证：模板实例化模块一次编译通过） |
| S2 | scaffold-module-gen skill 迁入 aether/pace + 编译闭环实跑 | ✅ 2026-07-06（skill 双端迁入 + generator 端到端实跑 quantum-biz-asset：全 11 模板独立模块零返工 BUILD SUCCESS + G1-G4 全过，证据见 sprints/2026-07-06-s2-scaffold-loop-verify/runtime-verify.md） |
| S3 | `quantum-mcp` 标准 MCP 只读能力适配（契约②） | 已实现 S3 skeleton：2026-07-07 新增 `quantum-mcp` module、OAuth 独立 token store、Bearer filter、Manifest v1、JSON-RPC `initialize/tools/list/tools/call` 与首批只读 tools |
| S4 | quantum-front Convention Pack | ✅ 2026-07-10（sprint `2026-07-09-s4-fe-scaffold-loop-verify`：scaffold-page-gen 零改动实跑 quantum-front，asset 演示页浏览器渲染实证，Rlues skill 核心 diff=0 → **脚手架无关论成立**；永久产物=FE 会话/导航层 mock 基建 + pack 纠偏；review pass1 REWORK→rework→pass2 PASS） |
| S4.5（补） | BE/cowork 运行时契约 + DataScope fail-closed | ✅ 2026-07-10（`be-runtime-contract-hardening`：runtime-env.md + mcp-test-access.md + DENY_ALL/switch-default/SystemDataScope 16 测试；`cowork-runtime-contract-docs`：docs/ai 三件套。F6 drill 全 blocker 清，`status: ok`） |
| S5（外部） | 下游 chat 产品对接 MCP | 平面 C，独立项目独立节奏（接口已由 S3 preflight 冻结） |
| S6（待环境） | F7 真·动态 E2E（FE mock-off + BE 起服 + OAuth→/mcp tools/call 全链 + playwright 证据） | ⏸ blocked：本机 PostgreSQL/Redis 未运行；环境就绪即立项（提案见 .ai_state/proposals.md） |

---

## 9. 待确认 / 依赖项

- **MCP 授权流程：✅ 已定案（2026-07-06 用户拍板）——直接 OAuth 2.1**（MCP 规范标准流程）。
  用户体验：agent 首次连接 → 401 携带受保护资源元数据（RFC 9728）→ agent 自动拉起浏览器 →
  用户以 quantum-backend 正常账号登录并同意 → 授权码 + PKCE 换短时 access token + refresh token →
  agent 缓存并自动刷新。**用户全程不手动粘贴 token**；撤销在服务端（下线/改密即失效）。
  S3 因此新增实现项：`/.well-known/oauth-protected-resource` 元数据、`/oauth/authorize` +
  `/oauth/token`（授权码 + PKCE，公共客户端 + 回环/自定义 scheme redirect）、access token
  映射回现有 LoginUser 会话体系（复用 TokenService 存储与吊销）；动态客户端注册（RFC 7591）可后置。
- **S3 前置设计项（Round 3 critic F4/F5, 2026-07-06）——开工前在 plan 阶段补齐, 不留到实现现场决策**:
  1. OAuth token 存储对齐: access/refresh token 复用现有 `TokenService` 存储还是新建 OAuth token 表;
     撤销语义与现有"下线/改密即失效"如何统一。
  2. Consent 页: `/oauth/authorize` 复用现有登录页 + 新增授权同意步骤, 还是独立 consent 页;
     公共客户端 + 回环 redirect 场景下 state/PKCE 与现有登录态 cookie 的 CSRF 组合校验方案。
  3. 跨项目冻结接口: Capability Manifest 最小 schema 示例 (一个真实 tool 的 JSON, 明确是否保留
     riskLevel/dataScopeMode 自定义 metadata 字段) + ai-service → quantum-mcp 的身份传递约定
     (Authorization: Bearer / RFC 8707 resource indicator 二选一), 双方独立开工前冻结, 防联调撞车。

### 9.1 S3 前置设计结论（2026-07-07）

权威记录见 `.ai_state/sprints/2026-07-07-quantum-mcp-s3-preflight-design/design.md`。

1. **OAuth token 存储**: S3 不把 MCP OAuth access token 当普通 Web access token 直接复用。
   新增 `quantum-mcp` 内部 `OAuthTokenService` / `OAuthTokenStore`, 使用 `quantum:oauth:*`
   独立 key 前缀, 存 `LoginUser` 快照 + `clientId` + `resource` + `scope`; 复用 `CacheClient`,
   `UserDetailsService` 刷新用户权限, 以及用户下线/改密时的吊销语义。普通 `TokenAuthenticationFilter`
   不消费 OAuth token; MCP 入口使用专用 Bearer filter, 验 token 后写 `SecurityContextHolder` + `UserContext`。
2. **Consent 页**: `/oauth/authorize` 复用现有登录态, 但必须新增授权同意页。授权码一次性、短 TTL,
   绑定 `client_id + redirect_uri + code_challenge + resource + userId`; 只允许公共客户端 + PKCE S256;
   S3 首版使用静态 client allowlist, 动态客户端注册（RFC 7591）后置。
3. **Capability Manifest v1 / 身份传递**:
   - Manifest 固定 `schemaVersion/resource/authorizationServers/transport/tools[]`;
     每个 tool 必填 `name`, `readOnly`, `permission`, `dataScopeMode`, `riskLevel`, `inputSchema`, `outputSchema`。
   - ai-service / project-data-reader 调 quantum-mcp 只传 `Authorization: Bearer <OAuth access token>`;
     禁止 `X-User-Id` / `X-Dept-Ids` / `X-Permissions` 等私有身份头。
   - 多资源绑定在授权/换 token 阶段使用 RFC 8707 `resource` 参数; tool 调用期用 token 内 resource 与当前 `/mcp` endpoint 比对。
- **框架加固待办（独立于 AI Sprint 的代码改动）**：`DataScopeAspect` 对"无用户上下文"当前是
  静默跳过（fail-open），建议改为 fail-closed（抛出未认证异常或注入恒假条件）。常规 HTTP 链路
  因认证前置而无法触达此路径，但它是所有非 servlet 入口（MCP / 定时任务 / 消息消费）的共同隐患。
- aether / pace 的 skill 打包规范（`SKILL.md` 字段、目录约定）以本仓库 Convention Pack 为被引用方，需对齐一次格式。
- 客户脚手架接入时，由客户提供其 Convention Pack 与（可选）MCP 适配；本设计只规定契约结构，不规定其内部实现。
- 下游 chat 产品的 Provider SPI / RAG / 配额属独立项目，另立设计，不在本仓库。
