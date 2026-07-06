# quantum-backend Convention Pack — 校验与自修

生成模块后必须通过编译校验，未通过不算完成。**结构照模板、语义由 AI、正确性由编译器兜底。**

## 校验命令

```bash
# JDK 25 环境。新增模块后先在根 pom 注册 <module>，再编译目标模块及其依赖：
mvn -q -pl quantum-biz-<module> -am -DskipTests compile
```

- 全量兜底：`mvn -q -DskipTests compile`
- 若改动涉及 mapper 生成（MyBatis-Flex processor 产出 `*TableDef`），首次需完整 compile 让 processor 生成表定义类，
  否则 `import ...table.XxxTableDef` 会找不到符号。

## 自修流程

1. 跑校验命令，收集 `[ERROR] ... cannot find symbol / package does not exist / ...`。
2. 按错误定点修复（常见：包名/TableDef 未生成/缺 convert 方法/权限点拼写）。
3. 重跑校验，直到 `BUILD SUCCESS`。
4. 报告新增/修改文件清单 + 生成的 `menu-permission.sql`。

## 生成后人工确认项（AI 不得擅自略过）

- [ ] 每个写接口（add/edit/remove/changeStatus 等）都带 `@RequiresPermission` 且 serviceImpl 里有写操作数据权限校验（TODO 已落实，非空壳）
- [ ] `menu-permission.sql` 的权限点与 controller 上的 `@RequiresPermission` 完全一致
- [ ] 敏感字段已 `@Sensitive` / `@JsonIgnore`
- [ ] 新模块已在根 `pom.xml` 的 `<modules>` 注册，且被 `quantum-server` 依赖
