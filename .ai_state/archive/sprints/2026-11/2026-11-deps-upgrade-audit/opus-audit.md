# 只读审查清单：quantum-backend @ 8176e44（src/main 下的 java、mapper xml、yml、log4j2）

没有改任何文件。下面每条都对照源码核对过；本机没实跑验证的标了「待验证」。证据只用了 Glob、Grep、Read，没有构建，也没跑测试。
- 死代码的 grep 范围是 `**/src/main/**/*.java`，不含 src/test。删除前要再搜一次 src/test，单测可能引用这些方法。
- 规模：安全 24 条、低效 21 条、重复 22 条、垃圾 13 条，另有 4 条超出四类的高把握缺陷（X 组）。

## 一、安全问题（S）

| ID | 严重度 | 文件:行 | 问题 | 建议的重构方式 | 风险 / 需补的测试 |
|---|---|---|---|---|---|
| S01 | P1 | quantum-biz-system/src/main/resources/mapper/SysMenuMapper.xml:25-34（selectPermsByUserId）、:6-22（selectMenusByUserId） | 权限和菜单 SQL 没有 JOIN sys_role 来过滤 status=1、deleted=0。角色被禁用后，`SysRoleServiceImpl.updateStatus` 触发的缓存刷新不会去掉该角色的权限（角色查询 SysRoleMapper.xml:6-12 有过滤，两者不一致）。 | 两条 SQL 都加 `INNER JOIN sys_role r ON r.id=ur.role_id AND r.status=1 AND r.deleted=0`；递归部分补 `m.status=1`。 | 补集成测试：禁用角色后 `loadUserByUsername().permissions` 不再含该角色的 perms。 |
| S02 | P1 | quantum-mcp/.../oauth/OAuthTokenService.java:43,78-79,99-104,119-121,168,176；quantum-biz-system/.../security/UserSessionInvalidationService.java:31-36 | MCP 的 code、access、refresh token 里存的是登录当时的 LoginUser 快照，refresh 时原样沿用，可续 30 天。禁用、删除用户或改权限都不影响 MCP token：isEnabled 读的是旧快照，ForceLogoutEvent 只踢 Web token。 | token 里只存 userId。validate 和 refresh 时通过 UserDetailsService 重新加载用户（可加短 TTL 缓存），并校验 enabled。新增 `OAuthTokenService.revokeAllByUser(userId)`（USER_PREFIX 集合已有），在 ForceLogoutEvent / UserCacheRefreshEvent 监听里调用。 | 测试：禁用用户后 /mcp 返回 401、refresh 被拒；改权限后 tool 调用按新权限判定。 |
| S03 | P1 | quantum-common/quantum-common-security/.../annotation/Sensitive.java:17-20；UserVO.java:44,50；DeptVO.java:64,70 | `@JsonSerialize` 放在自定义注解上，但没加 `@JacksonAnnotationsInside`。Jackson 会忽略这类元注解，所以 SensitiveSerializer 从来不生效，手机号和邮箱明文返回。全仓 grep `JacksonAnnotationsInside` 0 命中，也没有测试。 | 在 Sensitive 上加 `@com.fasterxml.jackson.annotation.JacksonAnnotationsInside`。顺带确认 AuthController /auth/info 返回本人信息时是否也该脱敏，以及 UserEditVO 是否需要脱敏。 | 待验证（本机未跑）。补测试：用容器里的 JsonMapper 序列化 UserVO，断言结果已脱敏。 |
| S04 | P1 | SysUserServiceImpl.java:136-138,178-183,384-388；SecurityConfig.java:186-188；FileController.java:109,117 | 分配角色时没有上限：有 `system:user:edit` / `add` / `import` 权限的非超管可以给任何人分配任意 roleId，包括 roleKey=admin 的角色和 dataScope=ALL 的角色。拿到 admin 角色就能访问 actuator（这里按 roles 里有没有 "admin" 判断），也能过 `@RequiresRole("admin")`。 | 新增 `assertRolesAssignable(roleIds)`：非超管不能分配 roleKey=admin 或 dataScope=ALL 的角色，也不能分配权限超出自己权限集的角色。同时统一「管理员」定义（见 D18）。 | 测试：非超管分配 admin 角色返回 403；导入接口同样被拦。 |
| S05 | P1 | SysUserServiceImpl.java:66,77（`@DataScope(type=DEPT_AND_CHILD)`）；对照 :587-596（isDeptInScope）和 DataScopeAspect.java:113-118 | 列表查询把数据范围写死成 DEPT_AND_CHILD，不读用户配置的范围。结果是：SELF 用户的 deptIds 为空，会退化成按本部门过滤，能列出本部门所有用户；ALL 范围的非超管反而只能看到本部门。详情、写接口按用户配置判定，两边不一致。 | 改为 `@DataScope(type=DEFAULT, userField="id")`，让 SELF 只看到本人；列表和详情共用同一套判定。 | 测试矩阵：ALL / DEPT / DEPT_AND_CHILD / CUSTOM / SELF 五种范围下，list 与 getById 的可见性一致。 |
| S06 | P1 | quantum-server/src/main/resources/application-dev.yml:6-7（DB admin/***）、:19（Redis 密码）、:64（JWT secret）；application-prod.yml:9,21（内网 IP 写死） | 密钥和凭证进了仓库，git 历史里也有。 | 改为 `${DB_PASSWORD}` 等环境变量，提供 `.env.example`；**轮换**已泄露的 DB、Redis 凭证（轮换前先确认）。 | 不涉及代码逻辑；改完后启动加载测试要用环境变量。 |
| S07 | P2 | SecurityProperties.java:61（`initPassword="Init@12345"`）；SysUserServiceImpl.java:397 | 批量导入的用户统一用一个源码里公开的默认密码，也没有首登强制改密。 | 默认值改为空并在启动时校验，或者给每个用户生成随机密码并强制首登改密。 | 测试：导入后用默认密码登录要求改密。 |
| S08 | P2 | SysDictTypeServiceImpl.java:152-155；对照 :51-54、:57-59；SysDictController.java:48-58,101-119 | 「business=admin 的字典类型对非超管不可见」只在分页和列表里生效。`/type/all`、`/type/{id}` 和字典数据接口都不过滤，可以绕过。 | 把过滤抽成 `visibleTypeCondition()`，所有读接口统一套用。 | 测试：非超管调 /type/all 看不到 admin 类型。 |
| S09 | P2 | SysDictController.java:64,72,125,133 | 直接用 `@RequestBody` 绑定实体 SysDictType / SysDictData，属于批量赋值：可改 id、version、dictType 等。改 dictType 后只清新 key 的缓存，旧 key 不清（SysDictDataServiceImpl.java:82-85）。 | 新增 DictTypeCreate/Update、DictDataCreate/Update 请求 DTO 和 Convert；更新时同时清旧、新两个 dictType 的缓存。 | 测试：请求带 deleted 或 createBy 字段不生效；改 dictType 后两个缓存都失效。 |
| S10 | P2 | LogAspect.java:258-261、:290-293 | 脱敏序列化失败时回退成未脱敏的 `jsonUtil.toJson(value)` 或原值，敏感字段会进 sys_oper_log。 | 失败时写占位串 `"[UNSERIALIZABLE]"`，不回退明文。 | 测试：构造一个序列化会失败的入参，断言日志里不含密码。 |
| S11 | P2 | TraceFilter.java:51-55,69,74 | X-Trace-Id 直接取自请求头，不校验就进 MDC、日志和响应头，可做日志注入、写超长值。 | 校验 `^[A-Za-z0-9-]{8,64}$`，不合法就自己生成。 | 测试：带换行或超长的 traceId 会被替换。 |
| S12 | P2 | LoginServiceImpl.java:59-69 | 验证码缺失或错误也会调用 `recordLoginFail`，不解验证码就能把任意账号锁住（账号锁定 DoS）。 | 验证码失败不计入账号失败次数；需要的话改为按 IP 计数。 | 测试：10 次验证码错误后账号未被锁。 |
| S13 | P2 | CaptchaServiceImpl.java:94-97、:82、:105 | 验证码校验是 get 再 delete，不是原子操作，并发时同一个验证码可用两次；DEBUG 日志里打印验证码明文（dev 环境 com.alpha=DEBUG）。 | 改用已有的 `cacheClient.getAndDelete`；删掉打印验证码的日志。 | 并发测试：同一 key 两次 verify 只有一次成功。 |
| S14 | P2 | LocalFileStorageServiceImpl.java:92；RustfsFileStorageServiceImpl.java:125,180；ExcelUtil.java:120,137,185,221,232,241,283；SysUserServiceImpl.java:332-334 | 把 `e.getMessage()` 拼进 BizException 返回给前端，会泄露服务器绝对路径、S3 错误、SQL 错误。 | 对外只给通用文案，细节写日志。 | 测试：模拟 IOException，响应里不含路径。 |
| S15 | P2 | SysUserServiceImpl.java:323-334 | 导入结果用 `"<br/>"` 拼接 Excel 里的用户名和异常信息。用户名来自 Excel，不经过 JSON 的 XSS 清洗，前端如果用 v-html 渲染就是 XSS（待验证前端）。 | 返回结构化的 `List<ImportRowResult>`，前端做转义。 | 测试：用户名含 `<img>` 时原样作为文本返回。 |
| S16 | P2 | JacksonConfig.java:60-62,252-265；对照 RequestWrapperFilter.java:40-49 | 全局 JSON 反序列化对所有 String 字段做 cleanHtmlTag，包括密码，会静默改掉 `<…>`；而且 `xss-excludes` 对 JSON body 不起作用（只影响参数和请求头）。 | 改为输出编码；至少跳过密码类字段，并让 excludes 对 body 也生效。与 D10 合并处理。 | 测试：密码含 `<a>` 时注册与登录一致；排除路径的富文本保留原样。 |
| S17 | P2 | CookieUtil.java:14（Path=/）；TokenAuthenticationFilter.java:53-55 | 只要没有有效 access token 且带了 refresh cookie，任何请求都会自动续期（轮换 refresh token）。结果是 cookie 实际上能认证所有接口，CSRF 防护只靠 SameSite=Strict；并发请求下 refresh 一消费，其余请求直接 401。 | 新增显式的 `POST /auth/refresh`，cookie 的 Path 限定到该路径；过滤器不再自动续期。 | 需同步改前端；测试：普通接口不再轮换 refresh token。 |
| S18 | P2 | TokenService.java:126-142、:435-444 | 续期时先 expire 再 set：如果 get 和 set 之间恰好被吊销，set 会把已删除的 token 写回来。黑名单检查在 get 之前，挡不住这个竞争，等于冗余。 | 续期只调 `expire`（key 不存在时不会创建），不再整体 set；去掉黑名单，或者把检查挪到 set 之后。 | 并发测试：revoke 与 validate 交错后 token 不复活。 |
| S19 | P2 | RedisConfig.java:34-37 | Redisson 的 JsonJacksonCodec 默认带类型信息做多态反序列化，类型校验器可能放行任意类（待验证当前 Redisson 版本的实现）。Redis 一旦被写入（dev 密码就在仓库里）可能触发 gadget。 | 用 `BasicPolymorphicTypeValidator` 限定只允许 `com.alpha.`、`java.` 包，或改用 TypedJsonJacksonCodec。 | 回归：LoginUser、OAuth 对象在 Redis 里读写正常。 |
| S20 | P2 | AuditManagerMessage.java:32-51；application-dev.yml:39 | SQL 审计日志记录的是带参数的完整 SQL（getFullSql）。dev 下 log-all-sql=true，bcrypt 哈希、手机号、邮箱会进 sql-audit.log；prod 的慢 SQL 同样会带参数。 | 记录参数化 SQL（不带参数值）；如果要记参数，按 sensitiveFields 脱敏。 | 测试：insertUser 之后审计日志不含 password 列的值。 |
| S21 | P2 | SecurityRequestWrapper.java:41-43 | JSON 请求体不限大小，整个读进内存（multipart 上限对它无效），存在内存 DoS。 | 按 Content-Length 或读取上限（如 2MB）截断，超出返回 413。 | 测试：超大 body 返回 413。 |
| S22 | P3 | PkceUtil.java:15-18；OAuthTokenService.java:74 | code_verifier 为空时抛 IllegalArgumentException，被处理成 500，不符合 RFC 应返回的 invalid_grant。 | 在 exchange 方法里先校验，抛 BizException(PARAM_INVALID)。 | 测试：缺 verifier 返回 400。 |
| S23 | P3 | SysOperLogMapper.java:21；SysLoginLogMapper.java:21 | 有 `monitor:*:remove` 权限就能 TRUNCATE 审计表。 | 清空操作限超管，或者只允许按保留期删除。 | 需要产品确认。 |
| S24 | P3 | SysUser.java:31 | 实体的 password 字段没加 @JsonIgnore（目前没有接口直接返回实体），属于纵深防御缺口。 | 加 `@JsonIgnore`。 | 无。 |

## 二、低效代码（E）

| ID | 严重度 | 文件:行 | 问题 | 建议的重构方式 | 风险 / 需补的测试 |
|---|---|---|---|---|---|
| E01 | P2 | TokenService.java:119-142、159、415、421、419-433 | 每个请求要 exists + get + getExpire，必要时再 expire + set，共 3-5 次 Redis 往返；JWT 解析两次；每次都 new MACVerifier / MACSigner 并重新 getBytes。 | 把 JWSSigner / Verifier 做成字段（线程安全）；只解析一次，同时拿 claims 和 exp；去掉冗余黑名单（S18）。 | 已有 TokenService 测试要回归。 |
| E02 | P2 | RedisUtil.java:246-255 | 限流每个请求都先 trySetRate 再 tryAcquire，2 次往返。 | 用本地 Caffeine Set 记录已初始化的 key，只首次 setRate；或改成一个 Lua 脚本。 | 测试：限流阈值不变。 |
| E03 | P2 | TokenService.java:276-303；UserSessionInvalidationService.java:24-29；调用方 SysRoleServiceImpl.java:182-185、SysMenuServiceImpl.java:202-213 | refreshUserCache 每个 token 都调一次 loadLoginUser（约 5 条 SQL），而且在提交后的事件里同步执行（请求线程内）。改一个菜单影响 N 个用户时开销是 N×token 数×5 条 SQL。 | 每个用户只加载一次，再写到它的所有 token；监听器加 `@Async`。 | 测试：多 token 用户只查一次库。 |
| E04 | P2 | SysMenuServiceImpl.java:206-209、:467-472 | 在循环里按 roleId 查用户（N+1）。 | 新增 `SysUserRoleMapper.selectUserIdsByRoleIds(Collection)`，用 IN 查询（与 D04 合并）。 | 无。 |
| E05 | P2 | SysUserServiceImpl.java:199-209 | 删除用户时逐个 getById、逐个 deleteByUserId（N+1）。 | 改用 `listByIds` + `deleteByUserIds(IN)`。 | 测试：批量删除时数据权限仍逐个校验。 |
| E06 | P2 | SysUserServiceImpl.java:316-336（validateBean 在 :318、:350、:357、:394、:410 共调 5 次）；ExcelUtil.java:245-247；SysUserController.java:148 | 导入每行约 6 条 SQL、5 次校验，行数不设上限，整批在一个事务里。PG 下中途一条出错，后续行全部报「事务已中止」。 | 限制行数（如 1000 行）；用 IN 批量预取已有的用户名、手机、邮箱；每行只校验一次；DB 异常直接中断。 | 测试：1000 行导入时 SQL 数量有上限。 |
| E07 | P2 | SysOnlineServiceImpl.java:31-48；TokenService.java:87 | 在线列表逐个 key 查 get（N 次往返）；计数把整个集合拉回来；ONLINE_TOKENS_KEY 只在吊销或有人打开列表时清理，会无限增长。 | 改用 ZSET（score=过期时间），定期 ZREMRANGEBYSCORE；计数用 ZCARD；用 RBuckets 批量 get。 | 测试：token 过期后集合会收缩。 |
| E08 | P2 | LocalCacheClient.java:65-83、:431；application.yml:66（基线配置 cache.mode=local） | rateLimiters、setStore 等 Map 没有上限也没有后台清理；限流的 key 是 IP 或用户 + URI，持续增长会内存泄漏。 | rateLimiters 改为 Caffeine（expireAfterAccess + maximumSize）；次级存储也加上限。 | 测试：大量不同 key 后内存有界。 |
| E09 | P2 | MultiLevelCacheConfig.java:266-276 | 每次 put / evict 都 `Thread.startVirtualThread` 去发同步消息。 | 改用 `redisson.getTopic(..).publishAsync`。 | 无。 |
| E10 | P2 | 各 Controller 上 SELECT 类的 @SystemLog（如 SysUserController.java:57）；LogAspect.java:130-133；AsyncConfig.java:27-30；OperLogEventListener.java:24 与 SysOperLogServiceImpl.java:33 双重 @Async | 每次列表或详情查询都把完整结果（pageSize 最大 2000）序列化后截断，再异步插库；异步执行器是无界虚拟线程，和 20 个连接的 Hikari 抢连接；一条日志开两个虚拟线程。 | saveResult 默认改为 false，SELECT 类不落库或只记摘要；改用有界执行器（ThreadPoolTaskExecutor 加队列，或信号量）；去掉 insertOperLog 上的 @Async。 | 压测：查询接口的 P99 和连接池等待。 |
| E11 | P2 | AuditManagerMessage.java:32；MybatisFlexConfig.java:55 | 每条 SQL 都先调 getFullSql 拼出完整 SQL，即使最后不写日志；debug 日志的参数也是先算好再判断级别。 | 只在命中条件时调用 getFullSql；用 `log.isDebugEnabled()` 包住。 | 无。 |
| E12 | P3 | LogAspect.java:187-188、242-250、301、312 | 每次调用都重建敏感字段集合；递归时每层都 deepCopy（valueToTree 已经是新对象）。 | 在 @PostConstruct 里预计算；去掉 deepCopy。 | 回归脱敏测试。 |
| E13 | P3 | TreeBuilder.java:61、83、99-110 | 建树是 O(n²)：getChildList 每次全表扫描，`roots.contains` 也是线性查找。 | 用 `groupingBy(parentId)` 一次分组，复杂度 O(n)。 | 回归：环和孤儿节点的兜底测试。 |
| E14 | P3 | RustfsFileStorageServiceImpl.java:129-134；LocalFileStorageServiceImpl.java:96 | 上传完成后重新读一遍流或文件来算 MD5。 | 上传时套 DigestInputStream，边传边算。 | 测试：MD5 结果一致。 |
| E15 | P3 | SysFileServiceImpl.java:119-130、:53-61 | 删除文件 N+1，且先删存储再删库：事务回滚后会留下指向已删文件的库记录；批量上传中途失败时，前面已上传的文件不清理。 | 用 listByIds；存储删除放到 afterCommit（TransactionSynchronization）；批量失败时补偿删除已上传文件。 | 测试：第二个文件失败时数据一致。 |
| E16 | P3 | SysDictDataServiceImpl.java:34-37；SysDictController.java:101 | 方法叫 selectDictDataPage，入参也继承了 PageQuery，实际却返回全量列表。 | 改用 `page(query.getPage(), wrapper)`，返回 PageResult。 | 前端分页参数要对齐。 |
| E17 | P3 | TraceFilter.java:131；LoginServiceImpl.java:56、84、180 | 每个请求都解析 User-Agent；登录流程里同一个 UA 解析 3 次。 | 解析一次，结果放进 UserContext。 | 无。 |
| E18 | P3 | CapabilityManifestService.java:84-89 | 每次查工具都重建整个工具 Map 列表。 | 改为 static final 的不可变列表或 Map。 | 无。 |
| E19 | P3 | SysUserServiceImpl.java:293-302 | 登录时把整行用户写回（会把 version +1），与管理员同时编辑会冲突。 | 用 UpdateChain 只更新 login_ip、login_location、login_date 三列。 | 测试：登录后管理员编辑不报版本冲突。 |
| E20 | P3 | IpUtil.java:249-308 | 每个请求都重新解析可信代理的 CIDR 列表。 | 启动时预解析成「掩码 + 网络地址」结构。 | 回归 IpUtil 测试。 |
| E21 | P2 | TokenAuthenticationFilter.java:63-65 | 格式错误的 Bearer 会触发 ParseException，每次都打一条 log.error 带完整堆栈，可被用来刷日志。 | 单独 catch ParseException / JOSEException，改为 debug 或无堆栈的 warn。 | 测试：随便传一个 token 不产生 ERROR 日志。 |

## 三、重复实现（D）

| ID | 严重度 | 全部重复点（文件:行） | 问题 | 建议的重构方式 | 风险 / 需补的测试 |
|---|---|---|---|---|---|
| D01 | P2 | RateLimitFilter.java:68-73；RepeatSubmitFilter.java:99-104；AccessDeniedHandlerImpl.java:32-35；AuthenticationEntryPointImpl.java:32-35；LogoutSuccessHandlerImpl.java:47-50；OAuthBearerAuthenticationFilter.java:80-88 | 6 处手写「设置状态码 + JSON + UTF-8 + 写 Result」。 | 在 framework 模块新增 `ServletJsonWriter.write(response, status, Result)`。 | 回归各处的状态码。 |
| D02 | P3 | TokenAuthenticationFilter.java:117-123；OAuthBearerAuthenticationFilter.java:72-78；LogoutSuccessHandlerImpl.java:36-38 | 从请求头提取 Bearer token 的逻辑写了 3 遍。 | 放到 security 模块的 `BearerTokens.resolve(request)`。 | 无。 |
| D03 | P2 | LoginServiceImpl.java:86；SysUserServiceImpl.java:130、230 | `username + password` 拼起来再做 BCrypt 的自定义方案写了 3 遍。这也是用户名不能修改的原因；Size 按字符计数，非 ASCII 密码可能超过 BCrypt 72 字节上限而抛异常。 | 短期：收进 `UserPasswordCodec.encode/matches(username, raw)`。长期：迁移到纯 BCrypt，用 DelegatingPasswordEncoder 在登录时升级哈希。 | 涉及数据迁移，要有回归测试；登录、改密、重置都要测。 |
| D04 | P2 | SysMenuServiceImpl.java:203-209 与 :461-474；SysRoleServiceImpl.java:182、206、236 | 「根据角色查受影响用户」的逻辑重复。 | `SysUserRoleMapper.selectUserIdsByRoleIds` 加一个 private helper（与 E04 一起做）。 | 无。 |
| D05 | P3 | SysUserMapper.xml:11-13 ≡ SysDeptMapper.xml:70-72；SysUserServiceImpl.java:500-523 | 按 ID 查部门名称的 SQL 两份完全相同，另有一处批量回填。 | 只保留 SysDeptMapper 一份；单条查询复用批量方法。 | 无。 |
| D06 | P3 | SysDeptMapper.xml:6-15（递归 CTE）与 :33-41（ancestors LIKE，已无调用方）；SysMenuServiceImpl.java:423-459（内存遍历） | 三种「取子树」的实现。 | 部门统一用 CTE；菜单可复用 TreeBuilder 的分组结果。 | 无。 |
| D07 | P2 | DeptConvert.java:100-111；SysDeptServiceImpl.java:210-217；SysMenuServiceImpl.java:332-341；MenuConvert.java:74-79 | TreeSelect 转换写了 4 份，其中 MenuConvert 不递归 children，导致 `/system/menu/treeselect` 只返回根节点（**缺陷**）。 | 写一个通用的 `TreeSelectVO.fromTree(list, idFn, labelFn)` 递归方法；删掉两份私有副本。 | 补测试：菜单下拉树包含子节点。 |
| D08 | P3 | UserConvert.java:23-38 ≡ SysUserServiceImpl.java:434-446（toNewUser）；AuthController.java:95-102 | 请求 DTO 到实体的转换重复。 | 统一用 UserConvert（新增 `toEntity(ProfileUpdateRequest)`）。 | 无。 |
| D09 | P3 | LogAspect.java:230-250；SensitiveFieldFilter.java:37-48 | 敏感字段的小写归一化写了两份。 | 由 OperLogProperties 提供一个归一化后的 Set，两处共用。 | 无。 |
| D10 | P2 | SecurityRequestWrapper.java:122-127；JacksonConfig.java:252-265 | 两处都调 HtmlUtil.cleanHtmlTag 做 XSS 清洗，排除路径的行为也不一致。 | 收敛成一个 XssCleaner、一个开关（见 S16）。 | 无。 |
| D11 | P3 | LoginServiceImpl.java:206-219；SysLoginLogServiceImpl.java:55-77 | 登录日志实体的组装重复（service 里的那份没有调用方）。 | 保留 service 里的那份，LoginServiceImpl 改为调用它。 | 无。 |
| D12 | P3 | CacheKeyConstant.java 全文件；CommonConstants.java:102-132；RateLimitFilter.java:35；RepeatSubmitFilter.java:40；TokenService.java:38-41、467-477；OAuthTokenService.java:20-24；SysOnlineServiceImpl.java:85（重新推导 access 前缀） | Redis key 前缀散落多处，有的定义了没用，有的用了没定义。 | 每个模块一个 `*CacheKeys` 类；TokenService 对外暴露 `tokenIdOf(key)`。 | 改前缀时要兼容 Redis 里已有的 key。 |
| D13 | P3 | CacheConstants.java:45-77；LocalCacheConfig.java:46；实际缓存名在 SysDictDataServiceImpl.java:40（`"sys:dict:"`） | 按缓存名分别配置的 user / permission / dict / config 都匹配不上实际缓存名，字典缓存一直用默认配置（L1 10 分钟、L2 1 小时），而不是为 dict 设计的 30 分钟 / 24 小时。 | 定义一个 `CacheSpec` 枚举（名称 + 容量 + TTL），注解和两个 CacheManager 都引用它。 | 改了缓存名后，旧的 Redis key 需要清理。 |
| D14 | P3 | JacksonConfig.java:99-250 | 6 个日期时间序列化 / 反序列化类，加上 Long 和 BigInteger 转 String，都是手写的。 | 改用 Jackson 3 自带的 LocalDateTimeSerializer 等（传 formatter）和 ToStringSerializer。 | 回归：日期格式、Long 精度。 |
| D15 | P2 | LocalFileStorageServiceImpl.java:48-126 与 RustfsFileStorageServiceImpl.java:86-165；SysFileServiceImpl.java:78-82、140-145、157-162 | 两个存储实现里的上传流程（日期目录、存储名、组装 FileInfo、批量上传）重复；按路径查文件写了 3 遍，而且是先 list 再取 findFirst。 | 抽出 `AbstractFileStorageService` 模板方法；新增私有 `findByPath`（getOne，加 limit 1）。 | 回归上传测试。 |
| D16 | P3 | FileController.java:139-147 | 手写缓冲区循环拷贝流。 | 改用 `is.transferTo(os)`。 | 无。 |
| D17 | P3 | LoginUser.java:130-132（userId==1）；UserDetailsServiceImpl.java:81-83；SysMenuServiceImpl.java:98、122；SecurityConfig.java:186-188（roleKey "admin"）；FileController.java:109、117；CommonConstants.java:144（SUPER_ADMIN_ROLE，未使用） | 「管理员」有两套互相矛盾的定义，散落 6 处。 | 新增单一的 `AdminPolicy.isSuperAdmin(LoginUser)`；actuator 和 RequiresRole 都改为调用它（与 S04 一起做）。 | 测试：持有 admin 角色的非 1 号用户在各处的结果一致。 |
| D18 | P3 | SysUserServiceImpl.java:517；SysMenuServiceImpl.java:102、247、256、426；SysDictTypeServiceImpl.java:52、63、108、117；SysDictDataServiceImpl.java:45 | BaseEntity 已经声明 `isLogicDelete`，MyBatis-Flex 会自动追加 deleted=0，代码里又手写了一遍。 | 删掉手写条件（XML 里的保留）。 | 回归查询测试。 |
| D19 | P3 | BaseEntity.java:28、43；MybatisFlexConfig.java:92、105 | 创建、更新时间既靠 `onInsertValue="now()"` 填，又在监听器里填一遍。 | 只保留一种方式。 | 无。 |
| D20 | P3 | QuantumApplication.java:16-17；AsyncConfig.java:22；MultiLevelCacheConfig.java:37；LocalCacheConfig.java:26 | `@EnableCaching` 和 `@EnableAsync` 重复声明。 | 各保留一处。 | 无。 |
| D21 | P3 | McpProperties.java:60-76；application.yml:78-87 | MCP 默认客户端在代码和 yml 里各配置一遍。 | 代码里的默认值改为空，只在 yml 里配置。 | 无。 |
| D22 | P3 | cache/monitor/CacheInfo.java:40-47 与 :64-72 | 缓存统计 Map 的构建逻辑写了两遍。 | 提成私有方法 `statOf(mlc)`。 | 无。 |

## 四、垃圾代码（G），死代码均附 grep 证据

| ID | 严重度 | 文件:行 | 问题与 grep 证据 | 建议的重构方式 | 风险 / 需补的测试 |
|---|---|---|---|---|---|
| G01 | P2 | ExcelUtil.java:61-101、124-243、249-311、315-532；ExcelExportConfig.java（整个文件）；BatchDataListener.java（整个文件） | 约 450 行没有调用方。搜 `ExcelUtil\.\w+`，只命中 SysUserController:148（read(InputStream,Class)）和 :192（export(resp,name,clazz,data)）；搜 `ExcelExportConfig\|BatchDataListener`，只在 file 模块内部互相引用。 | 只保留上面这两个方法。 | 先确认 src/test 没有引用。 |
| G02 | P2 | JsonUtil.java:64-315 | 搜 `jsonUtil\.\w+\(`，只用到 toJson 和 getObjectMapper；toPrettyJson、toBytes、3 个 parse、parseList、parseMap、2 个 convert、parseTree、createObjectNode / ArrayNode、getString / Int / Long / Boolean、isValidJson、isJsonObject、isJsonArray 都是 0 命中。 | 删除。 | 先查测试是否引用。 |
| G03 | P2 | RedisUtil.java:259-322、333-347；CacheClient.java:41-46、54、70、74-104、113、117-137、150-152；LocalCacheClient 中对应的 hashStore、listStore、zsetStore | 锁、布隆过滤器、延迟队列（延迟队列本身也是坏的：往 zset 写、从阻塞队列读）在 `quantum-common`、`quantum-biz-system`、`quantum-mcp` 三处都是 0 命中。CacheClient 的 Hash、List、ZSet、getOrLoad、getOrDefault、sIsMember、可变参数 sRemove、单参 tryAcquire 都是 0 个生产调用方（`increment(key)` 只有 LoginServiceImpl:148 在用；deleteKeysByPattern 只有 MultiLevelCache 直接用 RedisUtil 调）。 | 缩小 CacheClient 接口，两个实现同步删减。 | LocalCacheClient 的单测要同步删减。 |
| G04 | P3 | IpUtil.java:179-189、385-387、394-403、408-413、418-424、429-431、57/73/77 | getFullRegion、isExternalIp、ipToLong、longToIp、maskIp 都是 0 命中；getIpAddress 只是 getCityInfo 的兼容包装（LoginServiceImpl:176、210 和 TraceFilter:125 在调）；静态字段 xdbData 只用来打日志。 | 删除，调用方改为直接调 getCityInfo。 | 无。 |
| G05 | P3 | CacheKeyConstant.java:24-101 | 搜 `CacheKeyConstant\.`，只用到 AUTH_CAPTCHA、AUTH_LOGIN_FAIL、SYS_DICT，其余 15 个常量 0 命中。 | 删除未用常量（与 D12 一起做）。 | 无。 |
| G06 | P3 | CommonConstants.java:43、48、75、122、127、132、144、154、164-169 | HEADER_SPAN_ID、HEADER_PARENT_SPAN_ID、MDC_SPAN_ID、REDIS_PERM_PREFIX、REDIS_RATE_LIMIT_PREFIX、REDIS_REPEAT_SUBMIT_PREFIX、SUPER_ADMIN_ROLE、STATUS_NORMAL、YES、NO 都是 0 命中。HEADER_TENANT_ID 只在 TraceFilter:115 里填 LogInfo.tenantId，项目里没有多租户。 | 删除。 | 无。 |
| G07 | P2 | SysDeptMapper.java:35、60 与 xml:33-41、70-72；SysRoleMapper.java:26 与 xml:15-21；SysRoleMenuMapper.java:25-26；SysUserRoleMapper.java:25-26；SysDictDataMapper.xml:6-8；SysOperLogMapper.java:33-34、45-47；SysLoginLogMapper.java:33-34 | 按方法名搜调用：selectDeptAndChildren、selectNameById、selectRoleKeysByUserId、deleteByMenuId 都是 0 命中；`deleteByRoleId` 的命中全部是 roleMenu / roleDept 的 mapper，没有 userRole 的；deleteByDictType 只在 Mapper 接口里声明，没有调用；deleteOperLogBefore、countOperLogByTime、deleteLoginLogByDays 都是 0 命中。其中 deleteLoginLogByDays 的 SQL 把 `'#{days} days'` 写在引号里，参数绑定不进去，本身就是坏的。 | 删除。 | 无。 |
| G08 | P2 | ISysDictDataService 及其实现 SysDictDataServiceImpl.java:31、55-70、94-101；SysDictTypeServiceImpl.java:44-48、61-65、127-131；SysRoleServiceImpl.java:56-60、72-75；SysMenuServiceImpl.java:119-126、252-264；SysDeptServiceImpl.java:57-60；SysFileServiceImpl.java:73-104；SysLoginLogServiceImpl.java:43-53、89-94、117-130；SysOperLogServiceImpl.java:53-58、82-90；FileStorageService 中的 getStorageType、uploadBatch、deleteBatch、getUrl、getPresignedUrl 和三参 upload | 搜 `\.(方法名)\(`，下列方法只剩定义本身：selectByTypeAndValue、selectDictLabel、deleteByDictType、clearCache（和 refreshCache 一模一样）、selectByDictType、selectDictTypeList、selectRoleList、selectRolesByUserId、selectPermsByUserId、hasChildMenu、isMenuUsed、selectChildDeptIds、selectByPath、listByBiz、recordLoginSuccess / Failure、countLogin*、selectLoginLogList、countOperLog、countErrorLog、selectOperLogList；FileStorageService 列出的方法在 SysFileServiceImpl 之外无人调用。SysDictDataServiceImpl 里为缓存代理做的 ObjectProvider 自注入，只服务于已经没人调用的方法。 | 删除方法、对应接口声明和自注入。 | 先确认 src/test 没有引用。 |
| G09 | P3 | SysMenuServiceImpl.java:63、332-341；SysDeptServiceImpl.java:210-217；TreeBuilder.java:112-159；UserConvert.java:116-120；TokenService.java:50-52、389-394 | 私有方法只有自身递归引用；TYPE_BUTTON 0 命中；TreeBuilder 的 Map 重载 0 命中；UserConvert.toVOList 0 命中；createToken 的 4 参重载 0 命中（LoginServiceImpl:117 调的是 5 参版本）；generateRefreshToken 的 rememberMe 参数没被使用。 | 删除。 | 无。 |
| G10 | P3 | Anonymous.java（整个文件）；biz 的 dto/response/CacheInfo.java（整个文件）；LogInfo.java:74-93；DataPermissionContext.java:50-52、97-119；PageQuery.java:44-63；PageResult.java:96-117；SecurityProperties.java:26；SystemLog.java:24；SecurityConfig.java:44 | 搜 `Anonymous`，只命中定义本身，也没有任何处理逻辑；biz 的 CacheInfo 0 引用（MonitorController 用的是 cache.monitor 包里的同名类）；putExtra、getExtra、hasPermission、getFullDeptField、getFullUserField、needFilter、isAsc、empty、of(List,…)、csrfEnabled、description 都是 0 命中；`@EnableMethodSecurity` 开着，但 `@PreAuthorize` 0 命中。PageQuery 的 orderBy 字段如果以后被直接拼进 SQL，就是注入隐患。 | 删除。 | 无。 |
| G11 | P3 | LocalCacheConfig.java:21-22；RequestWrapperFilter.java:20、24-26；TraceFilter.java:34；BaseEntity.java:35、49；GlobalExceptionHandler.java:160；SysDeptServiceImpl.java:49；log4j2-spring.xml:35-46、142；`warpper` 包名；FileProperties.java:102 与 application.yml:25 | 注释过时：引用了不存在的 LocalCacheEnvironmentPostProcessor；类名写成 QuantumRequestWrapper；注释说该过滤器是 HIGHEST_PRECEDENCE，但它其实在 Security 链里，@Order 不起作用；提到的 AuditInterceptor 不存在。log4j2 里的 JSON_TEMPLATE 未被引用；`com.alpha.web.mapper` 这个 logger 对应的包不存在；包名拼写错误（warpper）；上传上限 100MB 与 multipart 上限 50MB 不一致。 | 修正或删除注释和配置；包名改为 wrapper。 | 改包名会影响 import。 |
| G12 | P3 | SysUserServiceImpl.java:233-239、264-273；TokenService.java:216-222、367-368；TraceFilter.java:119-120；IpUtil.java:92-93、376-377；RustfsFileStorageServiceImpl.java:132-134；OperLogEventListener.java:28-32 与 SysOperLogServiceImpl.java:36-40 | 抛完异常后又写 `if (result)`；两个 catch 块完全一样；空 catch 或 ignored；MD5 计算异常被吞掉、设成 null；同一个异常被 try/catch 两层。 | 去掉冗余判断；空 catch 至少打 debug 日志；只保留一层 try/catch。 | 无。 |
| G13 | P3 | 状态值：SysUserServiceImpl.java:245、SysRoleServiceImpl.java:192；dataScope 写死 4：SysRoleServiceImpl.java:124、176、231；菜单类型和状态 "M"、"C"、1、0：SysMenuServiceImpl.java:100-102；excludeId 的 `0L`：SysUserServiceImpl.java:284、289，SysRoleServiceImpl.java:265、270，SysDeptServiceImpl.java:194，SysDictTypeServiceImpl.java:117；UserConvert.java:147-179 | 魔法值。 | 状态用 CommonConstants.STATUS_*，数据范围用 DataScopeType.CUSTOM.getCode()，菜单类型用已有的 TYPE_DIR / TYPE_MENU；mapper 的 excludeId 改为可空，用 `<if>` 判断。另外 Excel 导入时无法识别的状态标签会被静默转成「禁用」，需要处理。 | 回归测试。 |

## 附：超出四类、但把握较高的缺陷（X）

| ID | 严重度 | 文件:行 | 问题 | 建议 | 测试 |
|---|---|---|---|---|---|
| X01 | P1 | ResponseAdvice.java:28、35-44、80-87 | `basePackages="com.alpha"` 的范围包含 MCP 和 OAuth 的 Controller，跳过列表里没有 /mcp、/oauth、/.well-known。这些接口返回的 Map 会被包成 Result，破坏 JSON-RPC 和 RFC 6749 的响应格式。McpControllerTest 是直接调用 Controller，覆盖不到这条路径。 | 加一个 `@NoResultWrap` 标记注解，或在 supports() 里排除 `com.alpha.mcp` 包。 | 待验证（本机未跑）。补 MockMvc 集成测试，断言响应顶层就是 `jsonrpc` 或 `access_token`。 |
| X02 | P1 | application.yml:28-33；application-prod.yml:3-4 | 基线配置全局排除了 Redisson 自动装配，prod 设了 cache.mode=redis 却没有覆盖这个排除项。RedisConfig.redisUtil 需要 RedissonClient，prod 预计启动失败。 | 把排除项挪到 local 专用的 profile，或者真正实现注释里提到的 EnvironmentPostProcessor。 | 待验证：用 prod profile 跑一次上下文启动测试。 |
| X03 | P2 | MenuConvert.java:74-79 | 菜单下拉树丢失子节点（见 D07）。 | 同 D07。 | 同 D07。 |
| X04 | P3 | SysDictTypeServiceImpl.java:87-89 | 传入的 id 不存在时 oldDict 为 null，抛 NPE，返回 500。 | 判空后抛 BizException。 | 补单测。 |

## 建议的重构顺序（按依赖、低风险优先）

1. **先修安全缺口，单点、易验证**：S06（凭证改为环境变量并轮换，需先确认）→ S03 → S01 → S13 / S12 → S11 / S10 → X02 / X01。每条带一个改前会失败的测试。
2. **权限模型收口**：D17（统一管理员定义）→ S04（分配角色上限）→ S05（数据范围对称）→ S08 / S09 → S02（MCP 改为实时加载用户并联动吊销）。D17 是 S04 的前置。
3. **删死代码，纯删除、风险最低**：G01 → G02 → G03 → G07 → G08 → G09 → G10 → G05 / G06 → G04。每批先搜 src/test，再跑全量测试。先删能缩小后续重构的范围。
4. **合并重复实现**：D01 / D02 → D12 / D13 → D04（与 E04 一起）→ D07（同时修菜单树）→ D05 / D06 → D08 / D09 / D10（与 S16 一起）→ D15 → D14（日期格式要回归）→ 其余 P3。
5. **热路径性能，要基线**：E21 → E01 + S18（TokenService 一次改完）→ E02 → E10 → E11 → E03 → E07 → E05 / E06 → E09 / E08 → 其余 P3。先测单请求的 Redis 往返次数和查询接口 P99，改完对比。
6. **高风险、需单独立项**：D03（密码哈希迁移，涉及数据）、S17（refresh 端点改造，要和前端联动）、S19（codec 变更要回归 Redis 里的已有数据）。
7. **最后收尾**：G11 / G12 / G13 和 D18-D22。
