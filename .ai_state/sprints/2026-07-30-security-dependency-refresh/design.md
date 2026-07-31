---
sprint_slug: "2026-07-30-security-dependency-refresh"
path: "System"
stage: "impl"
status: "approved"
---

# Security and Dependency Refresh — quantum-backend

## Why

建立当前后端仓库的可审计安全基线，逐一从 Maven Central 权威元数据核验所有直接及集中受管组件，升级到最新稳定版，并证明四模块仍可编译、测试和启动。非依赖类源码漏洞本轮负责发现、验证和报告；未经用户进一步授权不扩大为任意业务安全重构。

## Scope

- 仓库：`quantum-backend`，扫描范围 `.`。
- 模块：`quantum-common`、`quantum-server`、`quantum-biz-system`、`quantum-mcp`。
- 依赖：父 POM、root properties/BOM、所有模块显式 direct dependencies、显式 Maven plugins；默认最新稳定版，不采用 alpha/beta/rc/SNAPSHOT。
- 锁定树：升级 manifest 后重新解析完整传递依赖树，并执行漏洞审计；传递依赖通过父 BOM/lock resolution 更新，不逐项手工钉死。
- 安全：标准 repository-wide Codex Security 四阶段扫描；报告与 canonical JSON 复制到本 sprint 的 `security/` 目录。
- 写入：依赖升级在独立 Git worktree 中完成；现有主 checkout 的用户改动不覆盖、不回滚。

## Round 1 · Initial Plan

1. 固定当前提交/工作树快照，完成 threat model、全仓 discovery、validation、attack-path 与 canonical report。
2. 解析所有 POM 中的外部坐标及版本来源，用 `repo1.maven.org/maven2/.../maven-metadata.xml` 核验声明版本存在性和最新稳定 release。
3. 将 registry 证据保存为机器可读清单，按 BOM/parent、runtime、build plugin 分组制定升级顺序。
4. 设计评审通过后，在隔离 worktree 按组升级；每组先解析/编译，再跑针对性测试，最后跑全量 clean test。
5. 对升级后的依赖树执行漏洞审计与安全复核；记录仍存在的源码漏洞、上游漏洞或无法升级项及精确原因。

## Round 2 · Resolved Dependency Plan

在线元数据、effective POM、CycloneDX 与 OSV 基线已完成。实现限定在 root `pom.xml` 的版本治理，目标如下：

| 组件 | 当前实际解析 | 目标 |
|---|---:|---:|
| Hutool | 5.8.46 | 5.8.47 |
| Jackson 3 | 3.1.4 | 3.2.1 |
| Jackson 2 | 2.21.4 | 2.22.1 |
| Netty | 4.2.15.Final | 4.2.16.Final |
| PostgreSQL JDBC | 42.7.12 | 42.7.13 |
| AWS SDK S3 | 2.46.21 | 2.49.6 |
| Tomcat | 11.0.22 | 11.0.24 |
| HikariCP | 7.0.2 | 7.1.0 |

Spring Boot BOM 当前优先于项目的 Netty/Jackson BOM，导致属性值与实际解析版本不一致。Round 1 critic 指出 root 已通过 `spring-boot-starter-parent` 继承 dependency management，无需重复导入 Boot 或组件 BOM。实现改为删除 root 中冗余的 Boot、Netty、Jackson BOM import，并使用 Boot 4.1.0 官方覆盖属性 `netty.version`、`jackson-bom.version`、`jackson-2-bom.version`、`hikaricp.version`、`tomcat.version`；每项都以升级后的 effective POM 与 SBOM 为准，不以文本属性值作为完成证据。

基线 OSV 共命中 13 个唯一公告：Jackson 3 一个、Jackson 2 三个、Netty compression/DNS/HTTP/HTTP2 九个。升级后必须重新查询并证明这些命中归零。

## Acceptance Criteria

- AC1：Codex Security 标准扫描成功完成；所选 worklist 每行有完成 receipt，所有候选均有 discovery/validation/attack-path receipt，canonical `report.md`、`scan-manifest.json`、`coverage.json`、`findings.json` 可验证且复制到 `.ai_state/sprints/2026-07-30-security-dependency-refresh/security/`。
- AC2：所有 root/module POM 的外部 direct/managed dependency 与显式 plugin 坐标均出现在依赖清单；每个声明版本有 Maven Central 存在性结果，每个组件有权威 metadata 的最新稳定版或明确的“中央仓无元数据/私服”结论。
- AC3：所有可从权威 registry 确认的组件均升级到最新稳定版；无预发布版、无漏掉的旧 direct/managed/plugin 版本；BOM 管理项不重复钉版本。
- AC4：升级后 `mvn clean test` 对全部模块退出码为 0，且不存在只跑增量编译造成的假绿。
- AC5：升级后实际解析的 dependency tree 成功生成，依赖漏洞审计完成；任何仍开放的 CVE/GHSA/上游问题均列出组件、解析版本、影响范围和阻塞原因。
- AC6：`quantum-server` 在可用本地配置下完成启动/健康检查，MCP disabled 默认与鉴权失败路径保持 fail-closed；若外部 PostgreSQL/Redis 环境不可用，使用明确的可复现 blocker 证据而非伪报通过。
- AC7：升级产生的 breaking API 改动有最小兼容性修复和回归测试；不增加无调用方抽象、配置开关或静默降级。
- AC8：扫描结果、registry 清单、升级 diff、测试/运行时证据、review 和 cleanup 均记录在本仓 `.ai_state`；不改写用户已有 token/tool trace 文件。

## Done Contract

1. `test -f .ai_state/sprints/2026-07-30-security-dependency-refresh/security/report.md`，并用 canonical finalizer/manifest 校验通过。
2. 依赖清单校验脚本返回 `unqueried=0`、`outdated=0`、`prerelease_selected=0`；无法核验项必须带 registry URL、HTTP 状态和原因。
3. `mvn clean test` 返回 exit 0，并在 `runtime-verify.md` 记录模块/测试摘要。
4. `mvn dependency:tree -DoutputFile=...` 返回 exit 0，漏洞审计命令及结果保存到 sprint artifact。
5. 启动/健康、MCP disabled、未授权请求三个场景均有命令、状态码和期望断言，或有外部环境 blocker 的完整 stderr。
6. review pass 包含 Spec Compliance 与逐 AC 结论，最终 VERDICT 为 PASS；System 路径 cleanup 与 architecture 更新完成。

## Non-goals

- 不自动采用预发布组件。
- 不在没有验证证据时宣称修复源码漏洞。
- 不提交或推送，除非用户另行要求。
