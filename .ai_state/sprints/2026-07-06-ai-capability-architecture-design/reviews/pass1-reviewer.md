## Reviewer (代码层 findings, 2026-07-06T12:00:00Z)

### P0 (REWORK 级)

无。设计 sprint 无业务代码上线；治理文档 docs/ai-sprint-design.md 已按 Round1/Round2 收敛（chat/RAG/Provider/SSE/配额移出本仓库），与 .ai_state/sprints/2026-07-05-main-merge-athena-init/design.md 契约驱动路线（codegen 先于 MCP 自动写代码）无冲突；quantum-mcp 仍待实现（S3 pending），未提前引入运行时代码或密钥。

### P1 (CONCERNS 级)

1. ServiceImpl 模板的写侧数据权限校验是注释掉的 TODO, 非编译期强制, runtime-verify 未验证到该分支
   - File: docs/ai/convention-pack/templates/ServiceImpl.java.tmpl:67-68 (update), :55 (insert), :79 (deleteByIds)
   - 问题: 三处均为 "// TODO: 写操作数据权限校验..." / "// assertWritable(old);"（被注释掉）。对照真实 quantum-biz-system/src/main/java/com/alpha/system/service/impl/SysUserServiceImpl.java:166,202,227,257,365 都是真实调用 assertTargetUserWritable/assertDeptInDataScope。而 .ai_state/sprints/2026-07-05-main-merge-athena-init/design.md:30-33 记录的 P1 IDOR-write（导入路径裸 updateById 越权改写数据域外用户）正是同一类缺陷。模板把这条硬性要求降级为注释提示, validate.md:25（人工确认清单）是唯一兜底, 且清单勾选无强制机制——若跳过 TODO, mvn compile 依然 BUILD SUCCESS, 与 conventions.md/validate.md:3 宣称的"正确性由编译器兜底"不符: 编译器只能兜底语法, 不能兜底权限逻辑被略过。
   - 证据: runtime-verify.md:6-7 明确用无部门维度的 SysNotice 试算模块验证编译, 写侧越权分支（有 dept 维度实体）从未被实际编译/测试路径触达, 实证范围小于宣称的"模板全层齐套"。
   - 建议: 二选一 —（a）validate.md 自修流程加硬性 grep 门禁（生成后检测 update/delete 方法体内必须出现 assert.*Writable|assert.*DataScope 字样, 否则判定未完成）;（b）在 conventions.md 明确注明"数据权限校验为人工必查项, 非编译期保证"。
   - 引用: rules/coding-standards.md P0 安全（用户输入必须验证）、P0 Sisyphus 完整性。

2. 读侧 selectById 同样只留 TODO 注释, 复现 2026-07-05 P1 IDOR-read 模式
   - File: docs/ai/convention-pack/templates/ServiceImpl.java.tmpl:48
   - 问题: "// TODO: 若为敏感数据，补充读数据权限校验（参照 SysUserServiceImpl.assertTargetUserReadable）"。对照 .ai_state/sprints/2026-07-05-main-merge-athena-init/design.md:29 记录的真实 IDOR-read（selectUserById 曾裸 getById, 现已用 assertTargetUserReadable 修复）。模板复刻的是"裸 getById + TODO"的修复前状态, 对新模块相当于把已修复的漏洞模式当默认起点重新引入。
   - 建议: validate.md 人工确认清单补一条"若实体含 dept 维度或敏感字段, selectById 必须调用读权限校验, 不得裸 getById"。当前清单（validate.md:25-29）唯独没有单独点名 selectById 读侧。
   - 引用: rules/security-checklist.md 权限检查 P0。

3. menu-permission.sql.tmpl 的子节点 ID 生成方式是字符串拼接 magic pattern, 非可执行 SQL 表达式
   - File: docs/ai/convention-pack/templates/menu-permission.sql.tmpl:6-9
   - 问题: {{menuId}}1, {{menuId}}2, {{menuId}}3, {{menuId}}4 依赖模板变量替换后人工目测拼接十进制数字（如 menuId=100 → 1001/1002/1003/1004）, 不是数据库自增或表达式。位数变化或替换方式不同会产生 ID 冲突, 且 validate.md 只校验 Java 编译, 未校验 SQL 可执行性（对照 .ai_state/sprints/2026-07-05-main-merge-athena-init/design.md:235 建议的"migration 可执行性测试"）。
   - 建议: 改用真实 SQL 自增或显式列出独立占位（childMenuId1..4）, 或补 SQL 语法/去重校验步骤。
   - 引用: rules/coding-standards.md P0 DRY/Magic number。

### P2 (建议)

1. conventions.md/validate.md 措辞"正确性由编译器兜底"与实际权限校验非编译期保证之间的落差
   - File: docs/ai/convention-pack/validate.md:3
   - 建议: 改为"结构/语法正确性由编译器兜底, 权限/数据域正确性依赖人工确认清单", 避免读者误判校验强度。

2. Entity.java.tmpl 的 dept_id 注释提示依赖使用者自行判断
   - File: docs/ai/convention-pack/templates/Entity.java.tmpl:22
   - 建议: 非阻塞, ServiceImpl.java.tmpl 已有对应警示注释（:31-32）, 可考虑后续补静态检查（超出本 sprint 范围）。

### 无 finding 的维度

- Correctness（设计断言与仓库事实）: 治理文档 Round1/Round2 定位收敛（chat/RAG 移出仓库、ToolRegistry 归 quantum-mcp）与用户拍板记录一致; docs/ai-sprint-design.md:258-264 OAuth 2.1 定案、S3 两条硬性技术要求（fail-closed UserContext、@Sensitive 序列化路径）与现有 DataScopeAspect/@Sensitive 机制核实吻合, 无幻觉引用。Controller.java.tmpl:35 全量 @Validated 覆盖修复了 2026-07-05 P2#3 分页校验死代码问题, 属正向亮点。
- Security（skill 层面）: project-data-reader/SKILL.md:45 明确"禁止把长期 token 写进 agent 配置文件或 prompt"; install-to-rlues.sh 全部变量加引号、set -euo pipefail、路径存在性检查, 无命令注入/路径穿越风险。
- Design 一致性（依赖引入）: 未新增运行时依赖; quantum-mcp 仅设计未落代码, 与 2026-07-05 定案的"角色聚合优先, SysUser.dataScope 仅回退"语义未冲突。
- Test risk: runtime-verify.md 提供真实 mvn compile 实证（非纸面断言）, 但覆盖面有限（见 P1#1）, 已按 P1 记录而非单独 P0, 因为本 sprint 交付物是模板非生产代码。
