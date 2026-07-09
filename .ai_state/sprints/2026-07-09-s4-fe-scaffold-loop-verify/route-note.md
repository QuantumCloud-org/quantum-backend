# Route Note — 2026-07-09-s4-fe-scaffold-loop-verify

> v9.9.0 路由审议落盘. 写给三个月后 debug 路由失误的自己.

- **输入**: "起 S4: FE 生成链闭环 — quantum-front athena-init + scaffold-page-gen 端到端实跑 + 验证脚手架无关"
- **候选**: Feature (证据: 单能力验证, 生成物验后回滚, S2 同构先例) vs System (证据: 双仓+skill 三方; 反对: 无 ≥5 永久生产文件, .ai_state 纯附加)
- **权衡**: 爆炸半径=生成物临时+FE .ai_state 附加 · 可逆=高 (git 回滚) · 紧急=低 (建设) · 不确定性=低 (验收标准可直写)
- **决策**: **Feature** · 置信度 0.82
- **假设**: scaffold-page-gen 核心 (SKILL.md/workflow/scripts) 零改动即可跑 quantum-front
- **廉价退出**: 若跑通必须改 skill 核心 → 脚手架无关论证伪, 停 impl 上报为设计发现; 若只是 pack/adapter 数据纠偏 → 留痕继续
- **家**: sprint 状态落 BE .ai_state (bridge, 同 fe-be-convention-pack-expansion 先例); FE athena-init 后, 未来 FE sprint 迁 FE 自有 .ai_state
