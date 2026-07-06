---
doc_type: decision
slug: "codegen-security-gates-default-on"
created: "2026-07-06"
sprint_slug: "2026-07-06-ai-capability-architecture-design"
status: accepted
deciders: ["Mi_Manchi", "Claude Fable 5 (CC review/polish)", "Codex (CX impl)"]
---

# Decision: codegen-security-gates-default-on

## 背景 (context)

Convention Pack 驱动 AI 生成新业务模块, 但 review 发现模板把数据权限校验降级为 TODO 注释, 编译门禁无法发现"校验被略过"。需要决定: 生成代码的权限正确性靠什么保证。

## 选项 (options considered)

### 选项 A: 模板默认启用校验 + 显式豁免 + grep 硬门禁
- ✅ 优点: fail-closed, 忘记处理 = 生成失败而非埋雷; 豁免留痕可审计; 不引入新工具
- ❌ 缺点: 无部门维度实体要手动删除校验块并声明豁免, 生成流程多一步

### 选项 B: 保持 TODO 注释 + 人工确认清单
- ✅ 优点: 模板对所有实体形态通用, 零额外步骤
- ❌ 缺点: 清单勾选无强制机制, 复刻已修复的 IDOR 模式, 违反 rules/security-checklist.md 权限检查 P0

### 选项 C: 运行期统一 AOP 拦截, 模板不写校验
- ✅ 优点: 单点控制
- ❌ 缺点: 按 ID 直达的读写无法从注解推断目标 dept, 现有 @DataScope 只覆盖列表查询; 需要大改 orm 层, 超出本 sprint 范围

## 决定 (decision)

选 A。模板内置 assertReadable/assertWritable/assertInDataScope (语义对照 SysUserServiceImpl.isDeptInScope), 默认调用; validate.md 增设 G1-G4 grep 门禁, 豁免唯一合法形式是生成报告输出 `data-scope-exempt: <Entity> <原因>` 行。

## 权衡 (trade-offs)

接受生成流程多一步豁免声明的成本, 换取安全默认值方向正确; 接受辅助方法在各 ServiceImpl 重复 (每模块 ~30 行), 暂不上提到 common (等 ≥3 个模块使用后再抽, 避免过早抽象)。

## 影响 (consequences)

- 对本次 sprint: polish 阶段完成模板与 validate.md 改造。
- 对后续 sprint: scaffold-module-gen skill 生成任何模块必须跑 G1-G4 并在报告附结果或豁免行; S1 (quantum-mcp) 实现时同样适用。
- 对 architecture/: 已触发 ai-collaboration.md 子系统档案创建。
