# Cleanup Pass

sprint: 2026-07-06-ai-capability-architecture-design (path=System)
执行者: 主 agent (CC)。偏差说明: polish_worker subagent 未安装于 CC 端 (本机 Athena platforms_enabled=["cx"]),
改动均为小型文档/模板修订, 由主 agent 直接执行并在此留痕。

## 5 检查项

| 检查项 | 结果 |
|---|---|
| 临时代码 / 调试痕迹 | ServiceImpl.java.tmpl 三处安全性 TODO 已消除 (改为默认启用的真实调用); 保留的 2 处 TODO (唯一性校验/query 拼接) 属业务定制点, 合规。 |
| 注释完整性 | 模板头部补齐豁免规则与历史教训引用; assertInDataScope 带 docstring (SELF 语义细化说明)。 |
| 冗余 / 重复代码 | 数据权限辅助方法在各生成模块会重复 (~30 行/模块), 决策记录为"≥3 模块使用后再上提 common" (见 decision 档)。 |
| 低效模式 | deleteByIds 逐个 getById 校验 — N 次查询换权限正确性, 批量删除场景可接受; 未发现其他低效。 |
| 过度设计 | 无新增抽象; grep 门禁用现有 shell 工具, 未引入框架。 |

## review 意见合并 (pass1 P1×3 + P2×2)

| Finding | 处理 |
|---|---|
| P1#1 写侧校验为注释 TODO | ✅ ServiceImpl.java.tmpl: update/deleteByIds/insert 改为默认调用 assertWritable/assertInDataScope, 辅助方法语义对照 SysUserServiceImpl.isDeptInScope (ALL/DEPT/DEPT_AND_CHILD/CUSTOM/SELF 全分支, fail-closed); validate.md §2 增设 G1/G3 grep 硬门禁 + data-scope-exempt 显式豁免规则 |
| P1#2 selectById 裸 getById | ✅ 模板默认调用 assertReadable; validate.md G2 门禁 + 人工清单点名 selectById |
| P1#3 SQL 模板 ID 拼接 | ✅ menu-permission.sql.tmpl 改 5 个独立占位 ({{btnQueryId}} 等), 注释明确禁拼接 + 落库前查重; validate.md G4 检测占位残留与 ID 重复 |
| P2#1 "编译器兜底"措辞 | ✅ validate.md 开头改为三段校验分工表述 (编译器只兜语法, 权限靠门禁+清单) |
| P2#2 Entity dept_id 注释依赖人判断 | ⏸ 按 reviewer 意见非阻塞, 静态检查超出本 sprint 范围, 留待 S1 实施时评估 |

## Finishing-a-development-branch

- 无活动 worktree (impl 由 CX 侧经 PR #2/#3 合入 main, 本地 polish 直接在 main)。
- `mvn test` PASS (BUILD SUCCESS, 66 tests) — polish 仅改 docs/模板, 代码域零改动, 回归确认无破坏。
- 模板辅助方法引用的 API (UserContext.getUser / LoginUser.isAdmin/getDataScope/getDeptId/getDeptIds / DataScopeType.fromCode / BizException(ResultCode[,String])) 逐一对照真实源码确认存在。

## 归档到 compound/

- learning: 2026-07-06-learning-templates-replicate-fixed-vulnerabilities.md (模板是安全债的复制器)
- decision: 2026-07-06-decision-codegen-security-gates-default-on.md (校验默认启用+显式豁免+grep 门禁, 三选项对比)

## architecture/ 更新 (铁律[架构现状即真相])

- 新增 architecture/ai-collaboration.md (生成期/运行期两主线现状)
- ARCHITECTURE.md 补 AI 协作基座段 + 子系统索引 + 时间戳

## 铁律[零写入] 豁免留痕 (skip_impl_subagent_check=true)

delivery-gate 要求 System 路径 impl 经 generator subagent, 但本 sprint 为**跨平台协作**:
- impl (S1 Convention Pack, 20+ 文件) 由 CX 端 (Codex) 经其 generator 管线完成, 以 PR #2/#3 合入 main;
  证据链在 checklist.yaml (12 done 全有产物, evaluator Evidence Cross-Check 过) + runtime-verify.md, 不在 CC 端 subagent-log.md。
- CC 端本 sprint 实际写入仅 polish 阶段 4 个文档/模板修订 (绿区), 已在上文"执行者"段说明。
- 故按门禁解锁路径 B 设 `_index.skip_impl_subagent_check=true` (自负责), 不伪造 generator 日志。
- 后续改进: 跨平台 sprint 的 generator 证据互认, 属 Athena 框架议题, 已在 proposals 层面记录。

## VERDICT

PASS — 3 条 P1 全部闭环, P2 一修一缓 (缓项有记录), next_action=ship。
