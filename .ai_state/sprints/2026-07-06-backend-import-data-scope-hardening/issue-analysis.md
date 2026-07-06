# Issue Analysis

## 根因

- 导入路径使用 `UserExportVO` 直转 `SysUser`, 没有独立 import DTO 或 Bean Validation。
- 导入 Excel 没有部门/角色字段, service 无法按新增契约建立用户角色关系。
- `assertDeptInDataScope` 把 null context 当系统上下文放行, 与 read/write guard 不一致。
- 登录态数据权限以 `SysUser.dataScope` 为主, 角色只在 CUSTOM 时补部门集合, 形成双来源。

## 修复策略

1. 扩展导入 DTO/Excel 字段: 部门 ID、角色 ID 列表, 导入时构造并校验 `UserCreateRequest`/`UserUpdateRequest`。
2. service 导入新增分支写入用户角色; 更新分支按导入角色列可选更新角色绑定。
3. `assertDeptInDataScope` 无用户上下文时抛 `UNAUTHORIZED`, 与读写 guard 保持 fail-closed。
4. 新增角色聚合器, 登录时以角色集合计算最终 dataScope 和 deptIds; `SysUser.dataScope` 仅作兼容回退。
5. 补服务层单测和 controller/MVC 契约测试, 最后跑 `mvn test`。
