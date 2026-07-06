---
doc_type: learning
slug: "templates-replicate-fixed-vulnerabilities"
created: "2026-07-06"
sprint_slug: "2026-07-06-ai-capability-architecture-design"
severity: "P1"
status: executed
---

# Learning: templates-replicate-fixed-vulnerabilities

## 现象 (what happened)

AI 协作基座 sprint 交付的 Convention Pack 中, ServiceImpl 模板把读/写侧数据权限校验写成注释掉的 TODO (`// assertWritable(old);`), selectById 是裸 `getById` + TODO 提示。而同仓库 2026-07-06 当天刚修复的 IDOR-read/write (用户详情裸查 + 导入裸 updateById) 正是同一模式 — 模板等于把已修复漏洞的"修复前形态"固化为所有新模块的默认起点。review pass1 判 P1×2。

## 根因 (why)

模板作者以"编译器兜底正确性"为设计假设, 但编译器只能兜底语法; 权限校验被注释/删除后 `mvn compile` 依然 BUILD SUCCESS。安全语义的默认值方向写反了: 默认不安全 + 靠人记得加, 而不是默认安全 + 显式豁免。runtime-verify 又恰好用无部门维度的实体 (SysNotice) 试算, 校验分支从未被实证触达, 掩盖了缺口。

## 教训 (lesson)

代码生成模板是安全债的复制器: 模板里的每一个"TODO 记得加安全校验"都会以模块数 ×N 的速度复制到生产; 安全校验在模板中必须默认启用, 豁免必须显式声明并被门禁检测。

## 通用化 (generalization)

适用: 任何 codegen 模板/脚手架/snippet 库中涉及 authn/authz/输入校验/资源释放的部分 — 默认值必须是安全侧, 检验手段必须覆盖"被删掉"而非只覆盖"写错了"。不适用: 纯结构性模板 (DTO/VO 字段映射) 无安全语义, TODO 可接受。

## 相关引用

- 源代码: docs/ai/convention-pack/templates/ServiceImpl.java.tmpl (本 sprint polish 已修复为默认启用)
- 门禁: docs/ai/convention-pack/validate.md §2 安全门禁 G1-G4
- review: .ai_state/sprints/2026-07-06-ai-capability-architecture-design/reviews/pass1.md P1#1/#2
- 历史决策: compound/2026-07-06-decision-codegen-security-gates-default-on.md
