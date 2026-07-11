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
- **行级数据权限**（三条硬规则，违反其一即错误代码）：
  - **仅当实体含部门维度**（表有 `dept_id` 列，或注解指定了 `deptField`）时才使用。
    无部门维度的实体**禁止**标 `@DataScope` / 调 `applyDataScope`——编译不会报错，
    但运行期注入的 `dept_id` 条件会直接导致 SQL 错误。
  - 查询：service 方法标 `@DataScope`（**默认不带 type** = 按用户配置的数据权限；
    指定 `type = ...` 是强制覆盖，会把配置为"全部数据"的用户也压到指定范围，
    仅在业务明确要求时使用），构建 `QueryWrapper` 后调
    `DataPermissionInterceptor.applyDataScope(wrapper, "<别名>")`——注解与调用必须成对出现。
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
统一存放路径：`quantum-biz-<module>/src/main/resources/sql/menu-permission.sql`（S2 实跑回写，
避免各次生成自选路径导致落库脚本散落）。

## 生成清单（一个标准 CRUD 模块）

后端每个实体固定生成：Entity、Mapper、I*Service、*ServiceImpl、*Controller、
*Query、*CreateRequest、*UpdateRequest、*VO、*Convert、menu-permission.sql。

## 校验

见 `validate.md`。生成后未通过 `mvn compile` 不算完成。

## 验证 / 演示实体选择原则（2026-07-10 · 与 FE conventions.md 对称）

- 用于验证脚手架生成链的**演示实体不得选无关联维度的简单实体**（如纯字段的公告 `SysNotice`）。
  教训见 `compound/2026-07-06-learning-templates-replicate-fixed-vulnerabilities`：无部门维度的实体
  会让 `@DataScope` 数据权限分支从未被真实走到，掩盖越权/数据域缺口（S1 用 SysNotice 试算即踩过）。
- 演示实体**必须含部门或角色关联字段**（如资产 `asset` 带 `deptId`，用 `@DataScope(type = DEPT_AND_CHILD)`
  标注查询方法），这样 `assertReadable` / `assertWritable` / `applyDataScope` 的数据权限链才被实证覆盖。
- 数据权限现状（2026-07-10 fail-closed 加固后）：无用户上下文时 `DataScopeAspect` 注入 `DENY_ALL`
  （`1 = 0` 恒假），`DataPermissionInterceptor` 的 switch 有 `default` fail-closed 兜底；系统级任务
  需数据全域时显式用 `SystemDataScope.execute(...)`。生成的模块沿用 `@DataScope` 即自动受此保护。

## 新增模块 pom（runtime-verify 实证补充）

- parent：`com.alpha:quantum-backend:${revision}`；新模块自身 groupId 惯例为 `com.alpha.<module>`。
- **依赖 common 模块时 groupId 不是统一 `com.alpha`**，而是各自的 `com.alpha.<name>`：
  `com.alpha.logging:quantum-common-logging`、`com.alpha.security:quantum-common-security`、
  `com.alpha.orm:quantum-common-orm`、`com.alpha.file:quantum-common-file`（version 用 `${revision}`）。
- 新模块必须注册进根 pom `<modules>`，并被 `quantum-server` 依赖后才会参与部署。
