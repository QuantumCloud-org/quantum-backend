# quantum-backend Convention Pack — 生成约定

> 契约①实现，供 `scaffold-module-gen` skill 消费。描述"怎么给 quantum-backend 正确生成一个业务模块"。
> 生成后必须通过 `validate.md` 的校验命令。

## 模块结构

业务模块放在独立 maven 模块（参照 `quantum-biz-system`），包根 `com.alpha.<module>`：

```
quantum-biz-<module>/
  src/main/java/com/alpha/<module>/
    domain/            实体（继承 BaseEntity，MyBatis-Flex @Table）
    mapper/            Mapper 接口（继承 BaseMapper<T>）
    service/           Service 接口（I<Entity>Service）
    service/impl/      ServiceImpl（继承 ServiceImpl<Mapper, Entity>）
    controller/        Controller（@RestController + @RequestMapping）
    dto/request/       入参（*Request / *Query，Jakarta Validation 注解）
    dto/response/      出参（*VO）
    convert/           MapStruct/手写 转换器（Entity <-> VO/DTO）
```

## 分层约定

- **Controller**：只做参数绑定 + 调用 service + 包 `Result`。统一 `@RequestMapping("/<module>/<entity>")`，
  未来全局加 `/api/v1` 前缀。每个写接口标 `@SystemLog` + `@RequiresPermission`。
- **Service**：业务逻辑、事务（`@Transactional(rollbackFor = Exception.class)`）、唯一性校验、
  **写操作数据权限校验**（见下）。
- **Mapper**：MyBatis-Flex，复杂查询用 `QueryWrapper` + 生成的 `TableDef`（如 `SYS_USER`）。

## 命名与返回

- 统一响应体 `com.alpha.framework.entity.Result`；分页出参 `PageResult.of(page, convert::toVO)`。
- 分页入参继承/组合 `PageQuery`；查询入参 `*Query`，创建/更新 `*Request`（带 `@NotNull version` 做乐观锁）。
- 异常一律抛 `BizException`（含 `ResultCode`），由 `GlobalExceptionHandler` 统一处理，禁止在 controller try-catch。

## 权限约定（不可自建，一律复用）

- 功能权限：方法上 `@RequiresPermission("<module>:<entity>:<action>")`，action ∈ `list/query/add/edit/remove/export/import`。
- 角色限定：`@RequiresRole("admin")`。
- **行级数据权限**：
  - 查询：service 方法标 `@DataScope(type = ...)`，构建 `QueryWrapper` 后调
    `DataPermissionInterceptor.applyDataScope(wrapper, "<别名>")`。
  - **写操作（update/delete/reset/changeStatus/insert）按 ID 直达，必须单独校验**目标记录/目标部门
    是否在操作者数据权限范围内（参照 `SysUserServiceImpl.assertTargetUserWritable / assertDeptInDataScope`），
    否则拥有功能权限的用户可跨部门越权。**这是硬性要求，生成的写接口必须带此校验。**

## 实体约定

- 继承 `BaseEntity`（含 `id/createTime/createBy/updateTime/updateBy/deleted/version`），
  并 `implements Serializable`（带 `@Serial serialVersionUID`），**不加** `@Accessors`（对齐 `SysUser` 风格）。
- 逻辑删除 `deleted`、乐观锁 `version`；审计字段（createBy/createTime/updateBy/updateTime）由
  `MybatisFlexConfig` 注册的全局 `EntityInsertListener` / `EntityUpdateListener` 自动填充
  （注意：这与 `mybatis-flex.audit` 配置无关，后者是 SQL 审计）。实体代码中**不要**手动 set 审计字段。
- 密码等敏感字段出参用 `@Sensitive` 脱敏 / `@JsonIgnore`。

## 菜单与权限落库

生成模块时同步产出 `menu-permission.sql`：插入菜单节点 + 对应的 `<module>:<entity>:<action>` 权限点，
并挂到目标父菜单下（参照 `deploy/init.sql` 的 sys_menu 结构）。

## 生成清单（一个标准 CRUD 模块）

后端每个实体固定生成：Entity、Mapper、I*Service、*ServiceImpl、*Controller、
*Query、*CreateRequest、*UpdateRequest、*VO、*Convert、menu-permission.sql。

## 校验

见 `validate.md`。生成后未通过 `mvn compile` 不算完成。
