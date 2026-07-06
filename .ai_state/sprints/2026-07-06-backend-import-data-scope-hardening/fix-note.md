# Fix Note

## 修复范围

- 新增 `UserImportRequest`, 用户导入不再直转 `SysUser`, 改为复用 create/update DTO 的 Bean Validation。
- Excel 用户导入/导出补齐 `部门ID` 和 `角色ID列表`; 导出按用户批量回填角色 ID, 导入新增用户持久化用户角色关系。
- `assertDeptInDataScope` 在缺少 `UserContext` 时改为 `UNAUTHORIZED`, 避免 service 直调 fail-open。
- 登录态数据权限以有效角色集合聚合生成 `dataScope/deptIds`; `SysUser.dataScope` 仅在无角色时兼容回退。
- 新增 MVC 契约测试、角色批量映射测试、导入校验/角色持久化/无上下文拒绝测试。

## 排除范围

- 本轮按用户确认排除 AI 审查/生成体系: `contracts/`, codegen, CLI, MCP 自动创建后端代码均未实现。

## 验收

- `git diff --check` PASS。
- `mvn -pl quantum-biz-system -am test` PASS, Reactor 9/9 SUCCESS, `quantum-biz-system` 19 tests, 0 failures.
- `mvn test` PASS, Reactor 10/10 SUCCESS, `quantum-biz-system` 19 tests, 0 failures.

## VERDICT

PASS
