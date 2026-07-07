# quantum-backend Convention Pack — 单测生成约定

> 契约①实现，供未来 `unit-test-gen` skill 消费。描述"怎么给 quantum-backend 生成的模块补齐测试"。
> 生成后必须通过 `validate.md` 的 G6 校验。

## 分层测试形态（从真实测试提炼）

实探 `quantum-biz-system/src/test/java`（`SysUserServiceImplSecurityTest` / `UserDetailsServiceImplDataScopeTest` /
`SysDeptServiceImplTest` / `SysRoleServiceImplRoleMappingTest` / `SysUserControllerContractTest` /
`PageQueryValidationContractTest` / `RoleRequestValidationTest`）得出：

| 层 | 测试方式 | 断言库 | 典型类 |
|---|---|---|---|
| Service (`*ServiceImpl`) | JUnit5 + Mockito 纯单元测试，**不**起 Spring 容器 | AssertJ (`assertThat`/`assertThatThrownBy`) | `SysUserServiceImplSecurityTest` |
| Controller | MockMvc **standalone**（`MockMvcBuilders.standaloneSetup(controller)`），依赖用 `mock(...)` 手工注入，**不**用 `@SpringBootTest`/`@WebMvcTest` | AssertJ + `MockMvcResultMatchers`（`jsonPath`/`status`） | `SysUserControllerContractTest` |
| DTO / Request | 直接构造 `Validator`（`Validation.buildDefaultValidatorFactory().getValidator()`），校验 `validate(request)` 的违规集合 | AssertJ | `RoleRequestValidationTest` |
| 跨模块契约 | 反射扫描多个 Controller 方法签名，断言统一约定（如"分页接口参数必须带 `@Valid`/`@Validated`」） | AssertJ + `java.lang.reflect` | `PageQueryValidationContractTest` |

### 依赖

- 全部依赖来自 `spring-boot-starter-test`（含 JUnit Jupiter + Mockito + AssertJ + spring-test），
  单元测试不额外引入 mockito-inline / testcontainers 等重依赖。
- 无独立 surefire 配置覆盖（用 Maven 默认 `maven-surefire-plugin` 行为，测试类以 `*Test.java` 结尾即被扫描）。

### 命名规范

- 类名：`{Entity}ServiceImpl{关注点}Test`（如 `SysUserServiceImplSecurityTest`，聚焦安全/数据权限）或
  `{Entity}ServiceImplTest`（通用），`{Entity}ControllerContractTest`（controller 契约测试）。
- 方法名：**行为断言式**驼峰命名，`{动作}Should{预期结果}`（如 `selectUserByIdShouldRejectOutOfScopeUser`、
  `importUsersShouldRejectDuplicatePhoneOnCreate`），禁止 `test1`/`testXxx` 无语义命名。
- `@AfterEach` 清理线程级上下文（如 `UserContext.clear()`），避免测试间状态泄漏（参照
  `SysUserServiceImplSecurityTest.tearDown`）。

### Service 层构造模式

- `new {Entity}ServiceImpl(mock(Mapper), ..., publisher)` 直接 `new`，不经 Spring 容器；
  需要 stub 自身方法时用 `spy(newService(...))` + `doReturn/doAnswer(...).when(service)...`
  （参照 `SysUserServiceImplSecurityTest.newService`）。
- 权限上下文用 `UserContext.setUser(operator())` 显式设置 `LoginUser`（含 `deptId`/`deptIds`/`dataScope`），
  测试数据权限时必须真实构造 `DataScopeType`，不允许绕过。

## 每个生成模块必须覆盖的五个必测用例（四类关注点；安全默认启用，硬性要求）

对齐 `conventions.md` 的行级数据权限三条硬规则与乐观锁约定，`unit-test-gen` 为每个生成的
`{Entity}ServiceImpl` **必须**生成以下用例，缺一即视为生成未完成。

**标准方法名与 G6 适用范围（重要）**：
- 生成的测试**必须采用本节给出的标准方法名**（`selectByIdShouldRejectOutOfScopeEntity` 等，
  `{Entity}` 部分可替换为实际实体名），`validate.md` G6 门禁按标准名模式精确检测。
- **G6 只跑在 unit-test-gen 的生成物上，不约束存量测试**。存量测试（如
  `SysUserServiceImplSecurityTest.importUsersShouldRejectOutOfScopeUpdate`）命名风格一致但
  方法名自由，不受 G6 检测；存量中亦无权限码契约/乐观锁冲突用例——这两类正是生成约定要**补齐**的
  增量要求，而非对存量的描述。

### 1. 数据域越权 — 读

```java
@Test
void selectByIdShouldRejectOutOfScopeEntity() {
    // 构造 operator 数据权限范围外的目标记录，断言 assertReadable 拒绝并抛 BizException
}
```

### 2. 数据域越权 — 写

```java
@Test
void updateShouldRejectOutOfScopeEntity() {
    // 目标记录 deptId 不在 operator 范围内，断言 update/deleteByIds 抛 BizException（ACCESS_DENIED）
}
```

### 3. 权限码

- Controller 契约测试断言每个写接口方法标注了 `@RequiresPermission("<module>:<entity>:<action>")`
  且 action 与 `menu-permission.sql` 一致（可用反射读注解值断言，参照
  `PageQueryValidationContractTest` 的反射扫描模式）。

### 4. 分页校验

```java
@Test
void listEndpointShouldValidatePageQueryArgument() {
    // 反射断言 Controller#list(XxxQuery) 参数标注 @Valid/@Validated（参照 PageQueryValidationContractTest）
}
```

### 5. 乐观锁冲突

```java
@Test
void updateShouldThrowConflictWhenVersionMismatch() {
    // stub updateById 返回 false（模拟 version 不一致），断言抛 BizException(ResultCode.DATA_CONFLICT, ...)
}
```

> 说明：需求原文列出"数据域越权（读+写）、权限码、分页校验、乐观锁冲突"四类关注点，本约定拆为
> 上述 5 个具体用例（读/写越权各一），与 `validate.md` G6a-G6e 的标准名检测一一对应。

**豁免**：若表经 `db-conventions.md` 判定为无 `dept_id`（全局配置/关联表/已声明 `data-scope-exempt`），
则第 1/2 类测试改为断言"selectById 直接 getById 无需 assertReadable"（正向验证豁免生效，而非跳过测试），
并在测试报告写明 `data-scope-exempt` 对应行。不允许因豁免就不写测试。

## Debug Loop 约定

生成测试后必须闭环验证，禁止"写完就交"：

1. 跑 `mvn -pl <module> test`（module 用实际 artifactId，如 `quantum-biz-system`；
   若测试依赖同 reactor 内其他模块变更，追加 `-am`）。
2. 若失败：读 `<module>/target/surefire-reports/*.txt`（或终端输出的 `[ERROR]` 段）定位失败用例 + 断言差异。
3. 定点修复（测试断言错误 or 生产代码遗漏校验，二选一，不允许为了让测试通过而删断言）。
4. 重跑第 1 步，直到全绿。
5. **每一轮**（无论成功失败）都要记录进 `test-report.md.tmpl` 的 debug 轮次日志：
   轮次号、跑的命令、失败用例数、失败原因摘要、本轮修复动作。

## 测试报告模板

见 `templates/test-report.md.tmpl`。YAML frontmatter 机器可解析（module/date/rounds/tests_total/
passed/failed/coverage_paths），供 `validate.md` G6 或后续 CI 聚合读取。

## 校验

见 `validate.md` G6：grep 检测生成的测试类是否覆盖四类必测用例的方法名模式。
