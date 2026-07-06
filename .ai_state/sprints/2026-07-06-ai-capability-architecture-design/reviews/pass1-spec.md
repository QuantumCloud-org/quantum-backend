## Spec Compliance (spec-compliance, 2026-07-06T11:44:12Z)

范围: git diff 8dde00b..c0683b1 (22 files, +1018/-4)。design.md 经两轮 critic 重定范围 (Round1 VERDICT NEEDS_REVISION → Round2 re-scope)，权威范围以 docs/ai-sprint-design.md §8 阶段表 + checklist.yaml 为准: 本 sprint 实际交付 = S0(设计文档) + S1(Convention Pack 全层模板 + runtime-verify)；S2/S3/S4/S5 均标注"待办"。

### MISSING (做少了)
无。checklist.yaml 中 status=done 的 12 项全部有对应 diff 产物:
- convention-pack-complete → docs/ai/convention-pack/templates/ 11 个 .tmpl (Entity/Mapper/IService/ServiceImpl/Controller/Query/Create/Update/VO/Convert/menu-permission.sql)，与 conventions.md L74 声明的组成一致
- runtime-verify-trial → runtime-verify.md 记录 mvn compile BUILD SUCCESS 实证，且其声称"回写 conventions.md groupId 约定缺口"在 conventions.md:79-81 可核实存在
- claude-critic-round1 / rescope-round2 → design.md#Round 1/Round 2 段落存在
- 两个 skill (scaffold-module-gen / project-data-reader) → docs/ai/skills/ 下 SKILL.md 齐备，frontmatter 与 ai-sprint-design.md §5.5 示例逐字一致
- quantum-mcp-s3 状态为 pending (非 done)，无产物属预期，不计 MISSING

### EXTRA (做多了)
- E1 [合理补充]: docs/ai/skills/install-to-rlues.sh — 设计文档未逐字提及此安装脚本文件，但属于"两个 skill 迁入 aether/pace"既定目标 (ai-sprint-design.md §5.5/§6/§7) 的自然收尾工具，不引入新范围，判定合理。
- E2 [合理补充]: commit c0683b1 消息含"MCP OAuth 2.1 授权定案"，但 diff 中无任何 quantum-mcp 模块代码、无 OAuth 路由实现 (grep oauth/quantum-mcp 均为空)，实际只在 project-data-reader/SKILL.md 与 ai-sprint-design.md §9 记录为"决策已定案、S3 待实现"。文字先行不算代码 scope creep，且 checklist 明确标 quantum-mcp-s3: pending，未过度承诺。
- 无 scope creep 项（无 quantum-biz-ai / SSE / RAG / Provider SPI / token 配额代码，均按 Round2 裁决正确移出本仓库范围）

### DEVIATED (做偏了)
无重大偏离。细节核对:
- design.md 原 Round0 方案 (quantum-biz-ai 单体先行) 被 Round1 critic 判 P0 冲突后经 Round2 re-scope 作废，属**同一文档内部自我修正并记录**，非"文档说 A 实际做 B"的偏离，不计入 DEVIATED。
- Convention Pack 实际内容 (conventions.md 分层/命名/权限/DataScope/MyBatis-Flex 惯例 + templates + validate.md) 与 ai-sprint-design.md §2 契约①定义的四组成 (约定文档/代码模板/校验命令/能力清单映射) 一一对应，menu-permission.sql.tmpl 落地了"能力清单映射"要求。
- runtime-verify.md 声明"现场清理，试算模块不进 git 历史" — 核实 diff/name-only 中确无 quantum-biz-notice 相关文件，说法属实。

### 总评: PASS
