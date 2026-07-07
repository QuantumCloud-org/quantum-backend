# quantum-backend Convention Pack — DB 生成约定

> 契约①实现，供未来 `db-schema-gen` skill 消费。描述"怎么给 quantum-backend 正确生成一张表的设计文档 + DDL"。
> 生成后必须通过 `validate.md` 的 G5/G6 校验。

## 方言（实探结论）

`deploy/init.sql` 是 PostgreSQL（`Source Server Type: PostgreSQL`，`OWNER TO "admin"`，双引号标识符，
`bpchar`/`int8`/`int4`/`timestamp(6)` 类型），**不是 MySQL**。DDL 模板一律按 PG 语法产出，标识符加双引号。

## 双文档分离原则（硬约定，不可省略任一份）

每个模块的每张表固定产出**两份产物**，职责不重叠：

| 产物 | 路径 | 职责 |
|---|---|---|
| ① 表设计文档 | `docs/db/{module}-schema-design.md` | 业务语义：为什么有这张表、每个字段的业务含义、索引为什么建、数据域归属判定、审计/乐观锁列说明、敏感字段标注 |
| ② DDL | `deploy/sql/{module}-ddl.sql` | 可执行建表语句，只管"表长什么样"，不解释"为什么" |

- ①、②**互相引用**：设计文档顶部注明对应 DDL 文件路径；DDL 顶部注释注明对应设计文档路径。
- **①改必须同步②**（反之亦然）：字段增删/索引变更/类型调整，两份文件必须在同一次提交内一起改。
- 禁止只改 entity（Java）不出 DDL——entity 是运行时映射，DDL 才是数据库真相；两者不同步会导致
  `mvn compile` 能过但线上建表语句和代码字段对不上。
- 禁止 DDL 与设计文档漂移（字段数量/名称/类型不一致）：`validate.md` G5 门禁做字段级 diff 检测。

## 列约定（从 `deploy/init.sql` 实探提炼）

### 主键策略

- 主键统一 `id`，类型 `int8`（bigint）。
- 应用层由 `BaseEntity` 的 `@Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)` 生成雪花 ID，
  DDL 侧不使用自增序列（`serial`/`identity`），主键列只需 `int8 NOT NULL`。

### 审计五件套（固定四列 + version 单列，来自 `sys_config`/`sys_dept`/`sys_user` 等表实探）

```sql
"create_time" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
"create_by" int8,
"update_time" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
"update_by" int8,
```

- 对应 `BaseEntity` 的 `createTime/createBy/updateTime/updateBy`，由全局 `EntityInsertListener` /
  `EntityUpdateListener` 自动填充，DDL 侧只需给默认值，不需要触发器。
- 新表**必须**带齐这四列，缺一即视为偏离约定（唯一例外：纯日志类流水表如 `sys_login_log`，
  历史上未继承审计列，属存量特例，**新表不得比照**）。

### version 乐观锁

```sql
"version" int8 DEFAULT 0
```

- 对应 `BaseEntity.version`，标 `@Column(version = true)`，MyBatis-Flex 更新时自动做
  `WHERE id = ? AND version = ?` 并 `version = version + 1`。
- 新表**必须**带 `version`，更新类写接口必须校验 `updateById` 返回值，返回 `false` 时抛
  `BizException(ResultCode.DATA_CONFLICT, "...")`（参照 `SysUserServiceImpl.updateUser/resetPassword`），
  不允许静默吞掉冲突。

### deleted 软删

```sql
"deleted" int4 DEFAULT 0
```

- 对应 `BaseEntity.deleted`，标 `@Column(value = "deleted", isLogicDelete = true)`。0-未删除，1-已删除。
- 新表**必须**带 `deleted`，删除类接口一律走逻辑删除（`removeByIds`），禁止物理 `DELETE`。

### dept_id 数据域列判定规则

判定一张表**是否需要** `dept_id` 列（进而是否启用 `@DataScope`/`assertReadable`/`assertWritable`/
`assertInDataScope` 数据权限三件套，见 `conventions.md`），按以下规则：

1. **表的记录天然归属某个部门/组织**（如 `sys_user.dept_id`）→ 必须有 `dept_id int8` 列，且启用数据权限。
2. **表是全局配置/字典类**（如 `sys_config`/`sys_dict_type`/`sys_dict_data`）→ 无部门归属，不加 `dept_id`，
   数据权限走 `validate.md` 的豁免路径（`data-scope-exempt` 行）。
3. **表是多对多关联表**（如 `sys_role_dept`/`sys_user_role`/`sys_role_menu`）→ 不需要独立审计/乐观锁/
   `dept_id` 列（实探：这三张关联表只有业务列，无 `BaseEntity` 五件套），关联表的数据权限由两端主表控制。
4. **不确定归属**（业务上"这条数据算谁的"含糊）→ **默认视为需要 dept_id**（安全默认启用原则），
   除非能在设计文档写清楚不需要的理由，否则不得省略。
5. **表本身就是组织架构树**（如 `sys_dept` 用 `parent_id` 自引用表达层级）→ 不适用本判定规则，
   不需要额外 `dept_id` 列（`parent_id` 不是数据权限意义上的 `dept_id`）；这类表的数据权限由
   "自身即部门维度"决定，读写校验对齐 `id` 本身在操作者可见部门/子部门集合内，而非另建列。

### 敏感字段标注

- 密码/密钥类列（如 `sys_user.password`）在设计文档中必须标注"敏感字段"，并注明对应 entity 侧
  的脱敏方式（`@Sensitive`/`@JsonIgnore`，参照 `conventions.md` 实体约定）。
- DDL 侧敏感字段不额外加密（应用层用 `PasswordEncoder` 编码后落库），但设计文档必须写明"落库前已编码，
  不可逆"，避免误判为明文。

### 命名规范

- 表名：`{module}_{entity}`，全小写下划线（如 `sys_user`、`sys_dict_type`）。
- 列名：全小写下划线（`create_time` 不是 `createTime`）。
- 字符串列一律 `varchar(n)`（PG 无需 `COLLATE` 强制指定，实探默认 `pg_catalog.default`，模板保留但可省）。
- 状态类列用 `int4`（非 `boolean`/`varchar`），并在设计文档写清楚取值含义
  （如 `status`：0-禁用/停用，1-正常，实探于 `sys_user`/`sys_dept`/`sys_role`/`sys_menu` 一致）。
- 布尔语义的 `char(1)` 列（如 `sys_config.config_type` 的 Y/N）仅在对齐老表历史约定时使用，
  新表优先用 `int4` 状态列，不新增 `char(1)` 布尔列。

## 禁止事项

- 禁止只改 entity 不出 DDL。
- 禁止 DDL 与设计文档漂移（字段/索引/注释三者任一不一致）。
- 禁止新表跳过审计四件套 + version + deleted（除非是纯多对多关联表，见上）。
- 禁止对"归属含糊"的表默认不加 `dept_id`——含糊时默认加，删除需在设计文档写理由（对齐
  `conventions.md` 的 `data-scope-exempt` 豁免精神）。

## 生成清单（一张标准业务表）

1. `docs/db/{module}-schema-design.md`（模板见 `templates/schema-design.md.tmpl`）
2. `deploy/sql/{module}-ddl.sql`（模板见 `templates/ddl.sql.tmpl`）
3. 若表参与数据权限：在①中写明 dept_id 判定结论，供 `db-schema-gen` 之后调用
   `scaffold-module-gen` 生成 entity/service 时对齐（两个 skill 通过设计文档的判定结论串联）。

## 校验

见 `validate.md` 新增的 G5（DDL 与设计文档成对 + 互引）、G6（在 `test-conventions.md` 定义，
生成测试覆盖四类必测用例）。生成后未通过 G5 不算完成。
