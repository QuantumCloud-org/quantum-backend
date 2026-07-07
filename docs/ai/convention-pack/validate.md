# quantum-backend Convention Pack — 校验与自修

生成模块后必须依次通过：**编译校验 → 安全门禁 (grep) → 人工确认清单**，全部通过才算完成。
涉及 DB 生成（`db-schema-gen`）或测试生成（`unit-test-gen`）时，额外通过 **G5（DB 双文档）/ G6（测试覆盖）**。
分工：结构/语法正确性由编译器兜底；**权限/数据域正确性由安全门禁 + 人工清单兜底**（编译器只能发现语法错误，发现不了被删掉的权限校验）。

## 1. 编译校验

```bash
# JDK 25 环境。新增模块后先在根 pom 注册 <module>，再编译目标模块及其依赖：
mvn -q -pl quantum-biz-<module> -am -DskipTests compile
```

- 全量兜底：`mvn -q -DskipTests compile`
- 若改动涉及 mapper 生成（MyBatis-Flex processor 产出 `*TableDef`），首次需完整 compile 让 processor 生成表定义类，
  否则 `import ...table.XxxTableDef` 会找不到符号。

自修流程：

1. 跑校验命令，收集 `[ERROR] ... cannot find symbol / package does not exist / ...`。
2. 按错误定点修复（常见：包名/TableDef 未生成/缺 convert 方法/权限点拼写）。
3. 重跑校验，直到 `BUILD SUCCESS`。

## 2. 安全门禁（grep，硬性 — 任一 FAIL 即未完成）

生成器（AI）**必须**在编译通过后执行以下检查并把结果写进生成报告：

```bash
IMPL=<生成的 ServiceImpl 路径>

# G1 写侧数据权限：update/deleteByIds 必须调用 assertWritable（防 IDOR-write）
grep -qE "assertWritable\(" "$IMPL" && echo "G1 PASS" || echo "G1 FAIL"

# G2 读侧数据权限：selectById 禁裸 getById 返回，必须调用 assertReadable（防 IDOR-read）
grep -qE "assertReadable\(" "$IMPL" && echo "G2 PASS" || echo "G2 FAIL"

# G3 新增数据权限：insert 必须调用 assertInDataScope
grep -qE "assertInDataScope\(" "$IMPL" && echo "G3 PASS" || echo "G3 FAIL"

# G4 SQL 模板占位残留：menu-permission.sql 不得残留 {{...}}，5 个 ID 不得重复
SQL=<生成的 menu-permission.sql 路径>
! grep -q "{{" "$SQL" && echo "G4a PASS" || echo "G4a FAIL"
[ "$(grep -oE '^\s*\(([0-9]+)' "$SQL" | grep -oE '[0-9]+' | sort | uniq -d | wc -l)" -eq 0 ] && echo "G4b PASS" || echo "G4b FAIL"
```

**豁免规则（唯一例外路径）**：实体无部门维度（表无 dept_id 列）且无行级敏感性时，允许删除
assert*/@ DataScope/applyDataScope，此时 G1-G3 免检，但生成报告**必须**包含一行：

```
data-scope-exempt: <Entity> <原因，如"字典表，无行级归属">
```

无豁免行而 G1-G3 FAIL = 生成未完成，回到模板重新补齐。禁止以"业务简单"为由静默跳过。

## 3. 生成后人工确认项（AI 不得擅自略过）

- [ ] 每个写接口（add/edit/remove/changeStatus 等）都带 `@RequiresPermission` 且 serviceImpl 写侧校验为真实调用（非注释/空壳）
- [ ] `selectById` 未裸返回 `getById` 结果（有 `assertReadable` 或已声明豁免）
- [ ] 数据权限使用正确：实体**有** dept 维度 → `@DataScope` 与 `applyDataScope` 成对出现；
      实体**无** dept 维度 → 两者与 assert* 辅助一起删除，并已输出 `data-scope-exempt` 豁免行
- [ ] `menu-permission.sql` 的权限点与 controller 上的 `@RequiresPermission` 完全一致，5 个 ID 为显式分配（非拼接）
- [ ] 敏感字段已 `@Sensitive` / `@JsonIgnore`
- [ ] 新模块已在根 `pom.xml` 的 `<modules>` 注册，且被 `quantum-server` 依赖

## 4. DB 双文档门禁（G5，硬性 — FAIL 即未完成）

供 `db-schema-gen` skill 消费，校验 `db-conventions.md` 的"双文档分离原则"是否真正落地
（而非只出一份、或两份字段对不上）：

```bash
DESIGN=docs/db/<module>-schema-design.md
DDL=deploy/sql/<module>-ddl.sql

# G5a 成对出现：设计文档与 DDL 必须同时存在
[ -f "$DESIGN" ] && [ -f "$DDL" ] && echo "G5a PASS" || echo "G5a FAIL"

# G5b 互引：设计文档须引用 DDL 路径，DDL 须引用设计文档路径
grep -q "$DDL" "$DESIGN" && echo "G5b-design PASS" || echo "G5b-design FAIL"
grep -q "$DESIGN" "$DDL" && echo "G5b-ddl PASS" || echo "G5b-ddl FAIL"

# G5c 模板占位残留：两份文件不得残留 {{...}}
! grep -q "{{" "$DESIGN" && echo "G5c-design PASS" || echo "G5c-design FAIL"
! grep -q "{{" "$DDL" && echo "G5c-ddl PASS" || echo "G5c-ddl FAIL"

# G5d 审计五件套 + dept_id 判定不漂移：DDL 声明的列名需在设计文档「字段说明」表中逐一出现
# （占位实现：抽取 DDL 双引号列名，逐个 grep 设计文档；生成器执行时按实际列名展开）
for col in $(grep -oE '"[a-z_]+"' "$DDL" | tr -d '"' | sort -u); do
  grep -q "$col" "$DESIGN" || echo "G5d FAIL: $col missing in design doc"
done
```

**豁免规则**：无（本门禁无业务豁免路径——双文档分离是硬约定，任何模块都不得跳过）。

## 5. 生成测试覆盖门禁（G6，硬性 — FAIL 即未完成）

供 `unit-test-gen` skill 消费，校验 `test-conventions.md` 要求的四类必测用例是否真实生成
（而非只生成部分、或用假测试凑数）：

```bash
TEST=<生成的 {Entity}ServiceImplSecurityTest.java 路径>
CONTRACT=<生成的 {Entity}ControllerContractTest.java 路径，若沿用跨模块 PageQueryValidationContractTest 可指向该文件>

# G6a 数据域越权-读
grep -qE "void .*[Rr]eject.*OutOfScope.*\(" "$TEST" && echo "G6a PASS" || echo "G6a FAIL"

# G6b 数据域越权-写
grep -qE "void .*(update|delete|remove).*Reject.*OutOfScope.*\(" "$TEST" && echo "G6b PASS" || echo "G6b FAIL"

# G6c 权限码（Controller 契约测试断言 @RequiresPermission）
grep -qE "RequiresPermission" "$CONTRACT" && echo "G6c PASS" || echo "G6c FAIL"

# G6d 分页校验
grep -qE "void .*[Pp]age[Qq]uery.*[Vv]alidat.*\(" "$CONTRACT" && echo "G6d PASS" || echo "G6d FAIL"

# G6e 乐观锁冲突
grep -qE "void .*[Cc]onflict.*[Vv]ersion.*\(|void .*[Vv]ersion.*[Mm]ismatch.*\(" "$TEST" && echo "G6e PASS" || echo "G6e FAIL"
```

**豁免规则**：实体经 `db-conventions.md` 判定为无 `dept_id`（`data-scope-exempt`）时，
G6a/G6b 改为校验"豁免生效"的正向测试（方法名含 `WithoutDataScope`/`Exempt`），
且生成报告需在 `templates/test-report.md.tmpl` 的 frontmatter 设 `data_scope_exempt: true`。
无豁免声明而 G6a/G6b FAIL = 生成未完成。

## 6. 报告

完成后报告：新增/修改文件清单 + 安全门禁 G1-G4 结果（或豁免行）+ 生成的 `menu-permission.sql`
+（涉及 DB 生成时）G5 结果 + （涉及测试生成时）G6 结果与 `test-report.md.tmpl` 产出路径。
