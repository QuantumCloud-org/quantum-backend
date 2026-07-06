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

- 新模块：`quantum-mcp`（挂在 `quantum-common` 下或与 biz 平级，建议独立成 `quantum-mcp`）。
- **协议：走标准 MCP 协议**（决策已定），通用 agent（Claude Desktop / 你的 aether/pace / 第三方）都能连：
  - 主传输：**Streamable HTTP / SSE**（quantum-backend 本就是 web 应用，MCP server 内嵌暴露一个 SSE 端点最自然）。
  - 次传输：**stdio**（一个薄启动器，供本地 agent / CLI 直连）。
- 形态：MCP server 逻辑 **in-process 跑在 Spring 应用内**（这样才能复用 `PermissionAspect` + `DataScope`，
  不复制任何业务/权限逻辑）；对外以标准 MCP 协议暴露，不是私有 REST。
- 依赖方向：`quantum-mcp` → 依赖 biz service 接口；biz 不反向依赖它。
- 开关：`ai.mcp.enabled=false` 默认关闭，不给不需要的部署引入表面积。
- SSE 端点需从既有安全链排除防重复提交/限流的误杀（长连接），但仍走 Token 认证 + 数据权限。

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
- 本仓库提供两个 skill 的**参考起点**于 `docs/ai/skills/`（脚手架无关，属平面 A，最终迁入 aether/pace），
  以及 quantum-backend 的 Convention Pack 于 `docs/ai/convention-pack/`（属平面 B，本仓库的活）。

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
| S1 | 补全 quantum-backend Convention Pack（模板全层齐套 + menu SQL 模板） | 待办 |
| S2 | scaffold-module-gen skill 迁入 aether/pace + 编译闭环实跑 | 待办 |
| S3 | `quantum-mcp` 标准 MCP 只读能力适配（契约②） | 待办：身份透传 + DataScope 穿透 + 审计，默认关闭 |
| S4 | quantum-front Convention Pack | 待办：验证"脚手架无关"，skill 不改只换约定包 |
| S5（外部） | 下游 chat 产品对接 MCP | 平面 C，独立项目独立节奏 |

---

## 9. 待确认 / 依赖项

- **MCP 授权流程（S3 前必须定）**：设计已确定"每次调用带用户 JWT、由 quantum-backend 裁决"，
  但通用 agent（如 Claude Desktop）**如何获得这个 JWT** 尚未定义。候选：
  a) MCP 规范的 OAuth 2.1 授权流程（标准、对通用 agent 最友好，但 quantum-backend 需新增授权端点）；
  b) 用户在 agent 配置中粘贴自己的登录 token（零开发，但 token 过期体验差、有泄漏面）；
  c) 为 MCP 单独签发长期受限 PAT（个人访问令牌，范围只读 + 可吊销）。
  倾向 c) 起步、a) 为终态；S3 实现前需拍板。
- aether / pace 的 skill 打包规范（`SKILL.md` 字段、目录约定）以本仓库 Convention Pack 为被引用方，需对齐一次格式。
- 客户脚手架接入时，由客户提供其 Convention Pack 与（可选）MCP 适配；本设计只规定契约结构，不规定其内部实现。
- 下游 chat 产品的 Provider SPI / RAG / 配额属独立项目，另立设计，不在本仓库。
