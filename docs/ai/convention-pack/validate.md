# quantum-backend Convention Pack — 校验与自修

生成模块后必须依次通过：**编译校验 → 安全门禁 (grep) → 人工确认清单**，全部通过才算完成。
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

## 4. 报告

完成后报告：新增/修改文件清单 + 安全门禁 G1-G4 结果（或豁免行）+ 生成的 `menu-permission.sql`。
