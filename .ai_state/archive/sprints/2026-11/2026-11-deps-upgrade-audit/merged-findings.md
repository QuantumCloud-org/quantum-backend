# 第二轮合并审查清单

两份输入共 107 条，按具体缺陷合并为 81 行。定位列沿用两份报告的基线行号。跨条目覆盖的第一轮发现按子问题并入相应行。生产环境、线上变更和容量测量待单独验证。

| ID | 来源 | 类别 | 严重度 | 文件:行 | 处置 |
|---|---|---|---|---|---|
| S01 | Opus S01 | 安全 | P1 | quantum-biz-system/src/main/resources/mapper/SysMenuMapper.xml:25-34、:6-22 | 已修。菜单和权限 SQL 仅纳入启用、未删除的角色，递归菜单过滤停用祖先。SysMenuMapperContractTest 对两条 MyBatis BoundSql 回归。 |
| S02 | Opus S02 · 首轮 A02 | 安全 | P1 | OAuthTokenService.java:43,78-79,99-104,119-121,168,176；UserSessionInvalidationService.java:31-36 | 已修。授权码与令牌存用户 ID 和会话版本，兑换、续期、验证从 DB 读取当前用户。强制下线撤销旧版本。权限变更在下次 MCP 请求生效。旧格式 MCP 会话需重新授权。 |
| S03 | Opus S03 | 安全 | P1 | Sensitive.java:17-20；UserVO.java:44,50；DeptVO.java:64,70 | 已修。Sensitive 加 JacksonAnnotationsInside，SensitiveUserVOTest 断言手机号和邮箱脱敏。/auth/info 复用 UserVO，跟随脱敏。 |
| S04 | Opus S04 · 首轮 A01 | 安全 | P1 | SysUserServiceImpl.java:136-138,178-183,384-388；SecurityConfig.java:186-188；FileController.java:109,117 | 已修。用户新增、编辑、导入限制角色委派，角色定义修改检查菜单权限、部门及数据域。非超管禁止委派 admin、ALL 或范围外权限。 |
| S05 | Opus S05 | 安全 | P1 | SysUserServiceImpl.java:66,77；对照 :587-596和 DataScopeAspect.java:113-118 | 已修。用户列表使用 DEFAULT/id，与详情的本人、超管和部门可见性一致。AuthorizationAuditTest 验证列表入口配置，原数据域用例保留。 |
| S06 | Opus S06 · 首轮 A03 | 安全 | P1 | quantum-server/src/main/resources/application-dev.yml:6-7、:19、:64；application-prod.yml:9,21 | 按用户裁定保留 profile 文件。上线前由部署方处理凭证轮换及配置外部化。 |
| S07 | Opus S07 · 首轮 A04 | 安全 | P1 | SecurityProperties.java:61；SysUserServiceImpl.java:397 | 待办。按用户裁定保留默认初始密码。首次改密或独立激活流程需配套上线。 |
| S08 | Opus S08 | 安全 | P2 | SysDictTypeServiceImpl.java:152-155；对照 :51-54、:57-59；SysDictController.java:48-58,101-119 | 已修。字典类型全量、分页、详情及数据列表、详情、按类型查询共用可见性约束。缓存读取前校验字典类型权限。 |
| S09 | Opus S09 · Opus X04 | 安全 | P2 | SysDictController.java:64,72,125,133 | 已修。四个写接口只绑定请求 DTO，更新要求乐观锁版本。移动字典数据在提交后清除旧、新缓存键，缺失记录返回 404。AuthorizationAuditTest、SysDictDataVisibilityTest。 |
| S10 | Opus S10 · 首轮 A07 | 安全 | P2 | LogAspect.java:258-261、:290-293 | 已修（首轮）。LogAspect 脱敏失败使用占位或省略响应。LogAspectTest。 |
| S11 | Opus S11 | 安全 | P2 | TraceFilter.java:51-55,69,74 | 已修。TraceFilter 仅接受 8–64 位字母、数字、连字符，其余生成新追踪 ID。TraceFilterTest。 |
| S12 | Opus S12 | 安全 | P2 | LoginServiceImpl.java:59-69 | 已修。缺失或错误验证码不增加账户登录失败次数。LoginServiceImplTest。 |
| S13 | Opus S13 | 安全 | P2 | CaptchaServiceImpl.java:94-97、:82、:105 | 已修。验证码通过 getAndDelete 一次性消费，移除明文日志。CaptchaServiceImplTest 及 LocalCacheClient 原子消费测试。 |
| S14 | Opus S14 · 首轮 A09 · 首轮 A17 | 安全 | P2 | LocalFileStorageServiceImpl.java:92；RustfsFileStorageServiceImpl.java:125,180；ExcelUtil.java:120,137,185,221,232,241,283；SysUserServiceImpl.java:332-334 | 已修。上传沿用首轮通用错误。下载、Excel 导出及导入数据库异常使用通用文案，导入仅返回验证失败字段。FileStorageErrorTest。 |
| S15 | Opus S15 · 首轮 A17 | 安全 | P2 | SysUserServiceImpl.java:323-334 | 已修。Excel 导入按行返回 JSON 对象，移除 HTML 拼接。失败行列表对应整批事务回滚。 |
| S16 | Opus S16 · Opus D10 | 安全 | P2 | JacksonConfig.java:60-62,252-265；对照 RequestWrapperFilter.java:40-49 | 已修。清洗与排除路径由请求包装过滤器统一决定。JSON 和参数共用 XssCleaner，密码字段及排除路径保持原值。RequestWrapperFilterTest。 |
| S17 | Opus S17 · 首轮 A12 | 安全 | P2 | CookieUtil.java:14；TokenAuthenticationFilter.java:53-55 | 已修。显式 POST /auth/refresh 轮换令牌，返回新令牌及有效秒数。登录、刷新清除根路径 Cookie，失败和登出清除两处。普通请求只用访问令牌。RefreshContractTest、TokenAuthenticationFilterContractTest、CookieUtilTest。 |
| S18 | Opus S18 | 安全 | P2 | TokenService.java:126-142、:435-444 | 已修。访问令牌按 JWT 固定过期时间验证，删除滑动写回及黑名单；权限刷新仅替换仍存在的会话键。TokenServiceTest。 |
| S19 | Opus S19 · 首轮 A13 | 安全 | P2 | RedisConfig.java:34-37 | 已修。Redisson 保持原类型元数据格式，按类名拒绝 com.alpha. 和 java. 之外的非原始类型。RedisCodecRoundTripTest 覆盖 Web/MCP 对象及外部类型拒绝。 |
| S20 | Opus S20 · Opus E11 · 首轮 A08 | 安全 | P2 | AuditManagerMessage.java:32-51；application-dev.yml:39 | 已修。首轮改为参数化 SQL，本轮仅在慢 SQL/审计或启用 DEBUG 时读取模板。AuditManagerMessageTest。 |
| S21 | Opus S21 · 首轮 A18 | 安全 | P2 | SecurityRequestWrapper.java:41-43 | 已修。JSON 请求缓存上限 2 MiB，未知 Content-Length 同样限读并返回 413。RequestWrapperFilterTest。 |
| S22 | Opus S22 | 安全 | P3 | PkceUtil.java:15-18；OAuthTokenService.java:74 | 已修。缺失 PKCE verifier 返回 HTTP 400 与 invalid_grant，服务层在消费授权码前检查。OAuthTokenServiceTest。 |
| S23 | Opus S23 | 安全 | P3 | SysOperLogMapper.java:21；SysLoginLogMapper.java:21 | 已修。两张日志表的 /clean 均要求超管。AuditLogClearAuthorizationTest 使用真实权限切面验证普通角色与超管。 |
| S24 | Opus S24 | 安全 | P3 | SysUser.java:31 | 已修。SysUser.password 加 JsonIgnore。 |
| E01 | Opus E01 · 首轮 A06 | 性能 | P2 | TokenService.java:119-142、159、415、421、419-433 | 已修。复用 HS512 签名/验签对象，访问令牌解析一次并只进行一次会话缓存读取。TokenServiceTest 断言调用数。 |
| E02 | Opus E02 | 性能 | P2 | RedisUtil.java:246-255 | 已修。Redis 限流器初始化做有界本地记忆，热路径只取配额；Redis 淘汰或重启后恢复初始化。RedisRateLimiterTest。 |
| E03 | Opus E03 | 性能 | P2 | TokenService.java:276-303；UserSessionInvalidationService.java:24-29；调用方 SysRoleServiceImpl.java:182-185、SysMenuServiceImpl.java:202-213 | 已修查询放大。批取会话后每名用户加载一次权限，存在才替换。权限变更仍在提交后同步传播，避免异步排队延迟撤权。TokenServiceTest。 |
| E04 | Opus E04 · Opus D04 · 首轮 A19 | 性能 | P2 | SysMenuServiceImpl.java:206-209、:467-472 | 已修。菜单、角色统一走按角色集合查受影响用户的 IN 查询。 |
| E05 | Opus E05 | 性能 | P2 | SysUserServiceImpl.java:199-209 | 已修。批量查用户、逐个授权，再批量删角色关联。SysUserServiceImplSecurityTest 断言越权目标使整批停止。 |
| E06 | Opus E06 · 首轮 A21 | 性能 | P2 | SysUserServiceImpl.java:316-336；ExcelUtil.java:245-247；SysUserController.java:148 | 已修。FastExcel 读取与服务均限制 1,000 行；一次批量查用户名、手机号、邮箱，每行一次 Bean Validation。DB 错误立即中断回滚；导出上限 10,000 行。真实大批 SQL 容量待测。 |
| E07 | Opus E07 · 首轮 A19 | 性能 | P2 | SysOnlineServiceImpl.java:31-48；TokenService.java:87 | 已修。在线索引迁至 ZSET，按分数清理，列表批量取会话，计数用 ZCARD。周期及读路径迁移旧索引。 |
| E08 | Opus E08 · 首轮 A20 | 性能 | P2 | LocalCacheClient.java:65-83、:431；application.yml:66 | 已修。Caffeine 统一缓存 KV、计数与集合并按键限量。限流器独立限量且过期清理。在线索引成员按令牌过期时间周期清理。 |
| E09 | Opus E09 | 性能 | P2 | MultiLevelCacheConfig.java:266-276 | 已修。Redis L1 失效广播使用 publishAsync，记录发布异常。 |
| E10 | Opus E10 · 首轮 A18 · 首轮 A22 | 性能 | P2 | 各 Controller 上 SELECT 类的 @SystemLog；LogAspect.java:130-133；AsyncConfig.java:27-30；OperLogEventListener.java:24 与 SysOperLogServiceImpl.java:33 双重 @Async | 已修。操作日志默认省略响应，去掉重复异步，审计执行器 2–4 工作线程、队列 256，拥塞等待最多 1 秒。S3 请求总超时 2 分钟、单次尝试 45 秒。实际 P99 和容量待测。 |
| E12 | Opus E12 · Opus D09 | 性能 | P3 | LogAspect.java:187-188、242-250、301、312 | 已修。敏感字段启动时归一化，两处使用同一个集合，直接修改新建 JSON 树。 |
| E13 | Opus E13 | 性能 | P3 | TreeBuilder.java:61、83、99-110 | 已修。每个节点只访问一次，父子分组后构树；环路断开，孤儿成为根节点。 |
| E14 | Opus E14 | 性能 | P3 | RustfsFileStorageServiceImpl.java:129-134；LocalFileStorageServiceImpl.java:96 | 已修。本地与 S3 计算上传流的 MD5，移除事后重读。FileStorageErrorTest 含 S3 上传流摘要回归。 |
| E15 | Opus E15 · 首轮 A23 | 性能 | P2 | SysFileServiceImpl.java:119-130、:53-61 | 已修。文件批量查询。上传回滚补偿，删除在 DB 提交后执行存储清理。存储清理失败留诊断记录，线上对账及重试待部署方。 |
| E16 | Opus E16 | 性能 | P3 | SysDictDataServiceImpl.java:34-37；SysDictController.java:101 | 已修。字典数据管理列表实际分页，响应变为 PageResult。 |
| E17 | Opus E17 | 性能 | P3 | TraceFilter.java:131；LoginServiceImpl.java:56、84、180 | 已修。请求上下文解析一次 User-Agent，登录及访问日志复用。 |
| E18 | Opus E18 | 性能 | P3 | CapabilityManifestService.java:84-89 | 已修。工具定义使用静态不可变列表，查找不再重建清单。 |
| E19 | Opus E19 | 性能 | P3 | SysUserServiceImpl.java:293-302 | 已修。最近登录信息仅更新 IP、地点和时间三列。 |
| E20 | Opus E20 | 性能 | P3 | IpUtil.java:249-308 | 已修。启动配置绑定时解析受信 CIDR，请求只匹配字节地址，使用 IP 字面量。 |
| E21 | Opus E21 | 性能 | P2 | TokenAuthenticationFilter.java:63-65 | 已修。无法解析或验签失败的访问令牌走 DEBUG，无异常堆栈。 |
| D01 | Opus D01 | 重复 | P2 | RateLimitFilter.java:68-73；RepeatSubmitFilter.java:99-104；AccessDeniedHandlerImpl.java:32-35；AuthenticationEntryPointImpl.java:32-35；LogoutSuccessHandlerImpl.java:47-50；OAuthBearerAuthenticationFilter.java:80-88 | 已修。ServletJsonWriter 统一写入 HTTP 状态、UTF-8 JSON 和 Result。 |
| D02 | Opus D02 | 重复 | P3 | TokenAuthenticationFilter.java:117-123；OAuthBearerAuthenticationFilter.java:72-78；LogoutSuccessHandlerImpl.java:36-38 | 已修。Web 登录态、登出及 MCP 使用 BearerTokens 提取请求头。 |
| D03 | Opus D03 | 重复 | P2 | LoginServiceImpl.java:86；SysUserServiceImpl.java:130、230 | 已修。DelegatingPasswordEncoder 写入 {bcrypt}；旧格式按用户名加口令验证后条件更新 password 列。认证与写入检查 72 UTF-8 字节。PasswordMigrationTest。 |
| D05 | Opus D05 | 重复 | P3 | SysUserMapper.xml:11-13 ≡ SysDeptMapper.xml:70-72；SysUserServiceImpl.java:500-523 | 已修。单条部门名称查询归属 SysDeptMapper，列表仍批量回填。 |
| D06 | Opus D06 | 重复 | P3 | SysDeptMapper.xml:6-15与 :33-41；SysMenuServiceImpl.java:423-459 | 已修。部门删除无调用方的祖先查询。菜单子树遍历复用父子分组，处理循环关系。 |
| D07 | Opus D07 · Opus X03 | 重复 | P2 | DeptConvert.java:100-111；SysDeptServiceImpl.java:210-217；SysMenuServiceImpl.java:332-341；MenuConvert.java:74-79 | 已修。部门和菜单共用 TreeSelectVO.fromTree；MenuConvertTest 验证子节点。 |
| D08 | Opus D08 | 重复 | P3 | UserConvert.java:23-38 ≡ SysUserServiceImpl.java:434-446；AuthController.java:95-102 | 已修。删重复的新建用户转换，个人资料更新请求由 UserConvert 转换。 |
| D11 | Opus D11 | 重复 | P3 | LoginServiceImpl.java:206-219；SysLoginLogServiceImpl.java:55-77 | 已修。登录日志实体组装保留在实际调用的 LoginServiceImpl。 |
| D12 | Opus D12 · Opus G05 · Opus G06 | 重复 | P3 | CacheKeyConstant.java 全文件；CommonConstants.java:102-132；RateLimitFilter.java:35；RepeatSubmitFilter.java:40；TokenService.java:38-41、467-477；OAuthTokenService.java:20-24；SysOnlineServiceImpl.java:85 | 已修。删除未使用缓存键，在线会话使用 TokenService 解码访问键，现有令牌键保持可读。 |
| D13 | Opus D13 | 重复 | P3 | CacheConstants.java:45-77；LocalCacheConfig.java:46；实际缓存名在 SysDictDataServiceImpl.java:40 | 已修。字典按实际缓存名匹配 L1 30 分钟、L2 24 小时，缓存键沿用。 |
| D14 | Opus D14 | 重复 | P3 | JacksonConfig.java:99-250 | 已修。日期时间改用 Jackson 3 标准转换器，Long/BigInteger 使用 ToStringSerializer。 |
| D15 | Opus D15 | 重复 | P2 | LocalFileStorageServiceImpl.java:48-126 与 RustfsFileStorageServiceImpl.java:86-165；SysFileServiceImpl.java:78-82、140-145、157-162 | 已修。上传校验、命名和元数据走共享模板，路径端点只查一条记录。 |
| D16 | Opus D16 | 重复 | P3 | FileController.java:139-147 | 已修。下载流使用 transferTo。 |
| D17 | Opus D17 | 重复 | P3 | LoginUser.java:130-132；UserDetailsServiceImpl.java:81-83；SysMenuServiceImpl.java:98、122；SecurityConfig.java:186-188；FileController.java:109、117；CommonConstants.java:144 | 已修。超级管理员以账号 ID 为准，actuator、保留 admin 角色和文件路径接口统一判定。 |
| D18 | Opus D18 | 重复 | P3 | SysUserServiceImpl.java:517；SysMenuServiceImpl.java:102、247、256、426；SysDictTypeServiceImpl.java:52、63、108、117；SysDictDataServiceImpl.java:45 | 已处理。移除普通实体查询的重复逻辑删除条件。字典类型子查询及手写 SQL 保留显式条件。 |
| D19 | Opus D19 | 重复 | P3 | BaseEntity.java:28、43；MybatisFlexConfig.java:92、105 | 已修。数据库填充创建、更新时间，实体监听器仅填操作人。 |
| D20 | Opus D20 | 重复 | P3 | QuantumApplication.java:16-17；AsyncConfig.java:22；MultiLevelCacheConfig.java:37；LocalCacheConfig.java:26 | 已修。EnableCaching、EnableAsync 各一处。 |
| D21 | Opus D21 | 重复 | P3 | McpProperties.java:60-76；application.yml:78-87 | 已修。MCP 默认客户端只由 application.yml 提供，测试显式配置。 |
| D22 | Opus D22 | 重复 | P3 | cache/monitor/CacheInfo.java:40-47 与 :64-72 | 已修。CacheInfo 共用指标组装代码，未知缓存仍不创建。 |
| G01 | Opus G01 | 清理 | P2 | ExcelUtil.java:61-101、124-243、249-311、315-532；ExcelExportConfig.java；BatchDataListener.java | 已清理。删除未调用的 Excel 重载、配置和监听器，保留用户导入、导出入口。 |
| G02 | Opus G02 | 清理 | P2 | JsonUtil.java:64-315 | 已清理。JsonUtil 只保留生产调用的 toJson 和 getObjectMapper。 |
| G03 | Opus G03 | 清理 | P2 | RedisUtil.java:259-322、333-347；CacheClient.java:41-46、54、70、74-104、113、117-137、150-152；LocalCacheClient 中对应的 hashStore、listStore、zsetStore | 已清理。移除 Hash、List、锁、布隆和延迟队列等无生产调用实现，保留 E07 使用的有序集操作。同步移除专测已删接口的测试。 |
| G04 | Opus G04 | 清理 | P3 | IpUtil.java:179-189、385-387、394-403、408-413、418-424、429-431、57/73/77 | 已清理。删除无调用方的 IP 兼容方法，归属地调用直接使用 getCityInfo。 |
| G07 | Opus G07 | 清理 | P2 | SysDeptMapper.java:35、60 与 xml:33-41、70-72；SysRoleMapper.java:26 与 xml:15-21；SysRoleMenuMapper.java:25-26；SysUserRoleMapper.java:25-26；SysDictDataMapper.xml:6-8；SysOperLogMapper.java:33-34、45-47；SysLoginLogMapper.java:33-34 | 已清理。删除无调用方的 mapper 方法及对应 XML，保留审计保留期清理。 |
| G08 | Opus G08 | 清理 | P2 | ISysDictDataService 及其实现 SysDictDataServiceImpl.java:31、55-70、94-101；SysDictTypeServiceImpl.java:44-48、61-65、127-131；SysRoleServiceImpl.java:56-60、72-75；SysMenuServiceImpl.java:119-126、252-264；SysDeptServiceImpl.java:57-60；SysFileServiceImpl.java:73-104；SysLoginLogServiceImpl.java:43-53、89-94、117-130；SysOperLogServiceImpl.java:53-58、82-90；FileStorageService 中的 getStorageType、uploadBatch、deleteBatch、getUrl、getPresignedUrl 和三参 upload | 已清理。删除无调用的服务方法、接口声明及字典自注入。 |
| G09 | Opus G09 | 清理 | P3 | SysMenuServiceImpl.java:63、332-341；SysDeptServiceImpl.java:210-217；TreeBuilder.java:112-159；UserConvert.java:116-120；TokenService.java:50-52、389-394 | 已清理。删未使用的树方法和构造重载。刷新直接传递 rememberMe，保留会话持久标志。 |
| G10 | Opus G10 | 清理 | P3 | Anonymous.java；biz 的 dto/response/CacheInfo.java；LogInfo.java:74-93；DataPermissionContext.java:50-52、97-119；PageQuery.java:44-63；PageResult.java:96-117；SecurityProperties.java:26；SystemLog.java:24；SecurityConfig.java:44 | 已清理。删除未用注解、DTO 和辅助方法，移除无消费者的 CSRF 属性及方法鉴权启用注解。 |
| G11 | Opus G11 | 清理 | P3 | LocalCacheConfig.java:21-22；RequestWrapperFilter.java:20、24-26；TraceFilter.java:34；BaseEntity.java:35、49；GlobalExceptionHandler.java:160；SysDeptServiceImpl.java:49；log4j2-spring.xml:35-46、142；`warpper` 包名；FileProperties.java:102 与 application.yml:25 | 已清理。warpper 改为 wrapper，全部 src/main 与 src/test import 同步。移除过时注释和 Log4j 项目，文件默认限额与 multipart 统一到 50 MB。 |
| G12 | Opus G12 | 清理 | P3 | SysUserServiceImpl.java:233-239、264-273；TokenService.java:216-222、367-368；TraceFilter.java:119-120；IpUtil.java:92-93、376-377；RustfsFileStorageServiceImpl.java:132-134；OperLogEventListener.java:28-32 与 SysOperLogServiceImpl.java:36-40 | 已清理。合并异常分支，去掉重复成功判断及操作日志的双重异常捕获。 |
| G13 | Opus G13 | 清理 | P3 | 状态值：SysUserServiceImpl.java:245、SysRoleServiceImpl.java:192；dataScope 写死 4：SysRoleServiceImpl.java:124、176、231；菜单类型和状态 "M"、"C"、1、0：SysMenuServiceImpl.java:100-102；excludeId 的 `0L`：SysUserServiceImpl.java:284、289，SysRoleServiceImpl.java:265、270，SysDeptServiceImpl.java:194，SysDictTypeServiceImpl.java:117；UserConvert.java:147-179 | 已处理。状态、数据域和菜单采用已有常量，唯一性查询按空值决定 excludeId 条件，未知状态标签走字段校验。 |
| X01 | Opus X01 | 缺陷 | P1 | ResponseAdvice.java:28、35-44、80-87 | 已修。MCP、OAuth 与发现控制器的协议响应不做 Result 包装。McpResponseContractTest 通过 MVC 验证 JSON-RPC 根结构。 |
| X02 | Opus X02 | 缺陷 | P1 | application.yml:28-33；application-prod.yml:3-4 | 已修。Redis starter 只由 redis 模式条件装配。CacheModeConfigurationTest 验证 local 不建 Redisson、redis 装配；真实部署连接待验证。 |
| A05 | 首轮 A05 | 安全 | P2 | [OAuthBearerAuthenticationFilter.java:73](../../../quantum-mcp/src/main/java/com/alpha/mcp/security/OAuthBearerAuthenticationFilter.java#L73) · [McpSecurityConfig.java:34](../../../quantum-mcp/src/main/java/com/alpha/mcp/config/McpSecurityConfig.java#L34) | 已修（首轮）。MCP 链与过滤器共享路径匹配，按 context 相对路径判断。OAuthBearerAuthenticationFilterTest。 |
| A10 | 首轮 A10 | 性能 | P2 | [CacheInfo.java:61](../../../quantum-common/quantum-common-cache/src/main/java/com/alpha/cache/monitor/CacheInfo.java#L61) | 已修（首轮）。CacheInfo 先确认名称存在。CacheInfoTest。 |
| A11 | 首轮 A11 | 缺陷 | P3 | [FileUtils.java:123](../../../quantum-common/quantum-common-file/src/main/java/com/alpha/file/util/FileUtils.java#L123) | 已修（首轮）。FileUtils 使用 readNBytes 读取签名长度。FileUtilsTest。 |
| A14 | 首轮 A14 | 安全 | P2 | [AsyncConfig.java:44](../../../quantum-common/quantum-common-logging/src/main/java/com/alpha/logging/config/AsyncConfig.java#L44) · [MybatisFlexConfig.java:75](../../../quantum-common/quantum-common-orm/src/main/java/com/alpha/orm/config/MybatisFlexConfig.java#L75) · [StartupDependencyVerifier.java:55](../../../quantum-server/src/main/java/com/alpha/server/startup/StartupDependencyVerifier.java#L55) | 已修发现项。异步错误不打印参数，MyBatis 原生日志关闭，应用审计保留 SQL 模板，启动自检不输出 JDBC URL。 |
| A15 | 首轮 A15 | 安全 | P2 | [SysFileServiceImpl.java:65](../../../quantum-biz-system/src/main/java/com/alpha/system/service/impl/SysFileServiceImpl.java#L65) · [FileController.java:81](../../../quantum-biz-system/src/main/java/com/alpha/system/controller/FileController.java#L81) | 待定 P2。当前文件接口采用全局文件权限。业务附件的所属对象权限及既有共享文件兼容规则未定义，贸然改成上传人私有会破坏授权访问。需先定资源级策略。 |
| A16 | 首轮 A16 | 安全 | P2 | [FileUtils.java:161](../../../quantum-common/quantum-common-file/src/main/java/com/alpha/file/util/FileUtils.java#L161) · [RustfsFileStorageServiceImpl.java:119](../../../quantum-common/quantum-common-file/src/main/java/com/alpha/file/service/impl/RustfsFileStorageServiceImpl.java#L119) · [RustfsFileStorageServiceImpl.java:297](../../../quantum-common/quantum-common-file/src/main/java/com/alpha/file/service/impl/RustfsFileStorageServiceImpl.java#L297) | 已缩小风险。拒绝本地路径已有符号链接，RustFS 通过 URI 构造并按已验证扩展名设置类型，上传默认下载。存储目录写权限、旧对象元数据和桶 ACL 待部署核实。 |

## 验证范围与部署待办

- 本地验证覆盖 MyBatis 映射、Spring MVC、鉴权切面、Caffeine 及 Redisson codec。Redisson 连接和 S3 调用使用本地上下文或 mock。线上 Redis、PostgreSQL、RustFS、浏览器 Origin 和接口 P99 待验证。
- 访问令牌缓存调用计数，改前为黑名单存在性、访问值、TTL 共三次，测试确认改后一次读取。按用户刷新权限，多会话只加载一次用户。Redis 实际 RTT、压测吞吐和内存容量未测。
- 定向红阶段覆盖菜单角色过滤、敏感字段、验证码及失败计数、Trace ID、MCP 响应、数据域、字典 DTO、JWT 热路径、刷新端点、密码格式、Redis 类型及审计表权限。其余条目以对应回归或源码检查验证，部分行为用例未单独执行红阶段。
- 上线联动包括前端部署、MCP 旧会话重新授权、共享 Redis 客户端序列化兼容确认、历史对象元数据和存储清理对账，以及 S06 凭证轮换评估。代码执行范围限本工作区和本地测试。
- 未闭环项为 A15 文件资源级授权和用户裁定延期的 S07 默认初始密码。E03 同步传播权限有意保留，仍需实测大批权限改动。S06 按用户裁定跳过。

## 改动文件清单

以下为当前 worktree 相对 HEAD 的全部改动，包含首轮已留在工作区的依赖和修复。`M` 为修改，`D` 为删除，`??` 为新增。wrapper 重命名在工作区状态中显示为删除与新增。

```text
?? .ai_state/requirements/deps-upgrade-and-audit-2026-11.md
?? .ai_state/sprints/2026-11-deps-upgrade-audit/audit-report.md
?? .ai_state/sprints/2026-11-deps-upgrade-audit/design.md
?? .ai_state/sprints/2026-11-deps-upgrade-audit/log.md
?? .ai_state/sprints/2026-11-deps-upgrade-audit/merged-findings.md
?? .ai_state/sprints/2026-11-deps-upgrade-audit/opus-audit.md
?? .ai_state/sprints/2026-11-deps-upgrade-audit/upgrade-report.md
 M pom.xml
 M quantum-biz-system/src/main/java/com/alpha/system/controller/AuthController.java
 M quantum-biz-system/src/main/java/com/alpha/system/controller/FileController.java
 M quantum-biz-system/src/main/java/com/alpha/system/controller/SysDictController.java
 M quantum-biz-system/src/main/java/com/alpha/system/controller/SysLoginLogController.java
 M quantum-biz-system/src/main/java/com/alpha/system/controller/SysOperLogController.java
 M quantum-biz-system/src/main/java/com/alpha/system/controller/SysUserController.java
 M quantum-biz-system/src/main/java/com/alpha/system/convert/DeptConvert.java
?? quantum-biz-system/src/main/java/com/alpha/system/convert/DictConvert.java
 M quantum-biz-system/src/main/java/com/alpha/system/convert/MenuConvert.java
 M quantum-biz-system/src/main/java/com/alpha/system/convert/UserConvert.java
 M quantum-biz-system/src/main/java/com/alpha/system/domain/SysUser.java
?? quantum-biz-system/src/main/java/com/alpha/system/dto/request/DictDataCreateRequest.java
?? quantum-biz-system/src/main/java/com/alpha/system/dto/request/DictDataUpdateRequest.java
?? quantum-biz-system/src/main/java/com/alpha/system/dto/request/DictTypeCreateRequest.java
?? quantum-biz-system/src/main/java/com/alpha/system/dto/request/DictTypeUpdateRequest.java
 M quantum-biz-system/src/main/java/com/alpha/system/dto/request/UserImportRequest.java
 D quantum-biz-system/src/main/java/com/alpha/system/dto/response/CacheInfo.java
?? quantum-biz-system/src/main/java/com/alpha/system/dto/response/ImportRowResult.java
 M quantum-biz-system/src/main/java/com/alpha/system/dto/response/LoginResponse.java
 M quantum-biz-system/src/main/java/com/alpha/system/dto/response/TreeSelectVO.java
 M quantum-biz-system/src/main/java/com/alpha/system/mapper/SysDeptMapper.java
 M quantum-biz-system/src/main/java/com/alpha/system/mapper/SysLoginLogMapper.java
 M quantum-biz-system/src/main/java/com/alpha/system/mapper/SysOperLogMapper.java
 M quantum-biz-system/src/main/java/com/alpha/system/mapper/SysRoleMapper.java
 M quantum-biz-system/src/main/java/com/alpha/system/mapper/SysRoleMenuMapper.java
 M quantum-biz-system/src/main/java/com/alpha/system/mapper/SysUserMapper.java
 M quantum-biz-system/src/main/java/com/alpha/system/mapper/SysUserRoleMapper.java
?? quantum-biz-system/src/main/java/com/alpha/system/security/RoleDelegationPolicy.java
?? quantum-biz-system/src/main/java/com/alpha/system/security/UserImportException.java
 M quantum-biz-system/src/main/java/com/alpha/system/service/ISysDeptService.java
 M quantum-biz-system/src/main/java/com/alpha/system/service/ISysDictDataService.java
 M quantum-biz-system/src/main/java/com/alpha/system/service/ISysDictTypeService.java
 M quantum-biz-system/src/main/java/com/alpha/system/service/ISysFileService.java
 M quantum-biz-system/src/main/java/com/alpha/system/service/ISysLoginLogService.java
 M quantum-biz-system/src/main/java/com/alpha/system/service/ISysMenuService.java
 M quantum-biz-system/src/main/java/com/alpha/system/service/ISysRoleService.java
 M quantum-biz-system/src/main/java/com/alpha/system/service/ISysUserService.java
 M quantum-biz-system/src/main/java/com/alpha/system/service/impl/LoginServiceImpl.java
 M quantum-biz-system/src/main/java/com/alpha/system/service/impl/SysDeptServiceImpl.java
 M quantum-biz-system/src/main/java/com/alpha/system/service/impl/SysDictDataServiceImpl.java
 M quantum-biz-system/src/main/java/com/alpha/system/service/impl/SysDictTypeServiceImpl.java
 M quantum-biz-system/src/main/java/com/alpha/system/service/impl/SysFileServiceImpl.java
 M quantum-biz-system/src/main/java/com/alpha/system/service/impl/SysLoginLogServiceImpl.java
 M quantum-biz-system/src/main/java/com/alpha/system/service/impl/SysMenuServiceImpl.java
 M quantum-biz-system/src/main/java/com/alpha/system/service/impl/SysOnlineServiceImpl.java
 M quantum-biz-system/src/main/java/com/alpha/system/service/impl/SysOperLogServiceImpl.java
 M quantum-biz-system/src/main/java/com/alpha/system/service/impl/SysRoleServiceImpl.java
 M quantum-biz-system/src/main/java/com/alpha/system/service/impl/SysUserServiceImpl.java
 M quantum-biz-system/src/main/java/com/alpha/system/service/impl/UserDetailsServiceImpl.java
 M quantum-biz-system/src/main/java/com/alpha/system/support/TreeBuilder.java
 M quantum-biz-system/src/main/resources/mapper/SysDeptMapper.xml
 D quantum-biz-system/src/main/resources/mapper/SysDictDataMapper.xml
 M quantum-biz-system/src/main/resources/mapper/SysMenuMapper.xml
 M quantum-biz-system/src/main/resources/mapper/SysRoleMapper.xml
 M quantum-biz-system/src/main/resources/mapper/SysUserMapper.xml
?? quantum-biz-system/src/test/java/com/alpha/system/controller/AuditLogClearAuthorizationTest.java
?? quantum-biz-system/src/test/java/com/alpha/system/controller/AuthorizationAuditTest.java
?? quantum-biz-system/src/test/java/com/alpha/system/controller/PasswordMigrationTest.java
?? quantum-biz-system/src/test/java/com/alpha/system/controller/RefreshContractTest.java
?? quantum-biz-system/src/test/java/com/alpha/system/controller/SensitiveUserVOTest.java
?? quantum-biz-system/src/test/java/com/alpha/system/convert/MenuConvertTest.java
?? quantum-biz-system/src/test/java/com/alpha/system/mapper/SysMenuMapperContractTest.java
?? quantum-biz-system/src/test/java/com/alpha/system/service/impl/LoginServiceImplTest.java
?? quantum-biz-system/src/test/java/com/alpha/system/service/impl/SysDictDataVisibilityTest.java
 M quantum-biz-system/src/test/java/com/alpha/system/service/impl/SysRoleServiceImplRoleMappingTest.java
 M quantum-biz-system/src/test/java/com/alpha/system/service/impl/SysUserServiceImplSecurityTest.java
 M quantum-biz-system/src/test/java/com/alpha/system/service/impl/UserDetailsServiceImplDataScopeTest.java
 M quantum-common/quantum-common-cache/src/main/java/com/alpha/cache/config/LocalCacheConfig.java
 M quantum-common/quantum-common-cache/src/main/java/com/alpha/cache/config/MultiLevelCacheConfig.java
 M quantum-common/quantum-common-cache/src/main/java/com/alpha/cache/config/RedisConfig.java
 M quantum-common/quantum-common-cache/src/main/java/com/alpha/cache/constant/CacheConstants.java
 M quantum-common/quantum-common-cache/src/main/java/com/alpha/cache/constant/CacheKeyConstant.java
 M quantum-common/quantum-common-cache/src/main/java/com/alpha/cache/monitor/CacheInfo.java
 M quantum-common/quantum-common-cache/src/main/java/com/alpha/cache/util/CacheClient.java
 M quantum-common/quantum-common-cache/src/main/java/com/alpha/cache/util/LocalCacheClient.java
 M quantum-common/quantum-common-cache/src/main/java/com/alpha/cache/util/RedisUtil.java
?? quantum-common/quantum-common-cache/src/test/java/com/alpha/cache/config/CacheModeConfigurationTest.java
?? quantum-common/quantum-common-cache/src/test/java/com/alpha/cache/monitor/CacheInfoTest.java
 M quantum-common/quantum-common-cache/src/test/java/com/alpha/cache/util/LocalCacheClientTest.java
?? quantum-common/quantum-common-cache/src/test/java/com/alpha/cache/util/RedisRateLimiterTest.java
 D quantum-common/quantum-common-file/src/main/java/com/alpha/file/config/ExcelExportConfig.java
 M quantum-common/quantum-common-file/src/main/java/com/alpha/file/config/FileProperties.java
 D quantum-common/quantum-common-file/src/main/java/com/alpha/file/listener/BatchDataListener.java
 M quantum-common/quantum-common-file/src/main/java/com/alpha/file/service/FileStorageService.java
?? quantum-common/quantum-common-file/src/main/java/com/alpha/file/service/impl/AbstractFileStorageService.java
 M quantum-common/quantum-common-file/src/main/java/com/alpha/file/service/impl/LocalFileStorageServiceImpl.java
 M quantum-common/quantum-common-file/src/main/java/com/alpha/file/service/impl/RustfsFileStorageServiceImpl.java
 M quantum-common/quantum-common-file/src/main/java/com/alpha/file/util/ExcelUtil.java
 M quantum-common/quantum-common-file/src/main/java/com/alpha/file/util/FileUtils.java
?? quantum-common/quantum-common-file/src/test/java/com/alpha/file/service/impl/FileStorageErrorTest.java
 M quantum-common/quantum-common-file/src/test/java/com/alpha/file/util/FileUtilsTest.java
 M quantum-common/quantum-common-framework/pom.xml
?? quantum-common/quantum-common-framework/src/main/java/com/alpha/framework/advice/NoResultWrap.java
 M quantum-common/quantum-common-framework/src/main/java/com/alpha/framework/advice/ResponseAdvice.java
 M quantum-common/quantum-common-framework/src/main/java/com/alpha/framework/config/JacksonConfig.java
 M quantum-common/quantum-common-framework/src/main/java/com/alpha/framework/config/TrustedProxyProperties.java
 M quantum-common/quantum-common-framework/src/main/java/com/alpha/framework/constant/CommonConstants.java
 M quantum-common/quantum-common-framework/src/main/java/com/alpha/framework/context/UserContext.java
 M quantum-common/quantum-common-framework/src/main/java/com/alpha/framework/entity/LoginUser.java
 M quantum-common/quantum-common-framework/src/main/java/com/alpha/framework/enums/ResultCode.java
 M quantum-common/quantum-common-framework/src/main/java/com/alpha/framework/exception/GlobalExceptionHandler.java
 M quantum-common/quantum-common-framework/src/main/java/com/alpha/framework/util/IpUtil.java
 M quantum-common/quantum-common-framework/src/main/java/com/alpha/framework/util/JsonUtil.java
?? quantum-common/quantum-common-framework/src/main/java/com/alpha/framework/util/ServletJsonWriter.java
?? quantum-common/quantum-common-framework/src/main/java/com/alpha/framework/util/XssCleaner.java
 M quantum-common/quantum-common-logging/src/main/java/com/alpha/logging/annotation/SystemLog.java
 M quantum-common/quantum-common-logging/src/main/java/com/alpha/logging/aspect/LogAspect.java
 M quantum-common/quantum-common-logging/src/main/java/com/alpha/logging/config/AsyncConfig.java
 M quantum-common/quantum-common-logging/src/main/java/com/alpha/logging/entity/LogInfo.java
 M quantum-common/quantum-common-logging/src/main/java/com/alpha/logging/event/OperLogEventListener.java
 M quantum-common/quantum-common-logging/src/main/java/com/alpha/logging/filter/SensitiveFieldFilter.java
 M quantum-common/quantum-common-logging/src/main/java/com/alpha/logging/filter/TraceFilter.java
 M quantum-common/quantum-common-logging/src/main/java/com/alpha/logging/properties/OperLogProperties.java
 M quantum-common/quantum-common-logging/src/main/java/com/alpha/logging/service/ISysOperLogService.java
?? quantum-common/quantum-common-logging/src/test/java/com/alpha/logging/aspect/LogAspectTest.java
?? quantum-common/quantum-common-logging/src/test/java/com/alpha/logging/filter/TraceFilterTest.java
 M quantum-common/quantum-common-orm/src/main/java/com/alpha/orm/config/AuditManagerMessage.java
 M quantum-common/quantum-common-orm/src/main/java/com/alpha/orm/config/MybatisFlexConfig.java
 M quantum-common/quantum-common-orm/src/main/java/com/alpha/orm/context/DataPermissionContext.java
 M quantum-common/quantum-common-orm/src/main/java/com/alpha/orm/entity/BaseEntity.java
 M quantum-common/quantum-common-orm/src/main/java/com/alpha/orm/entity/PageQuery.java
 M quantum-common/quantum-common-orm/src/main/java/com/alpha/orm/entity/PageResult.java
?? quantum-common/quantum-common-orm/src/test/java/com/alpha/orm/config/AuditManagerMessageTest.java
 D quantum-common/quantum-common-security/src/main/java/com/alpha/security/annotation/Anonymous.java
 M quantum-common/quantum-common-security/src/main/java/com/alpha/security/annotation/Sensitive.java
 M quantum-common/quantum-common-security/src/main/java/com/alpha/security/config/SecurityConfig.java
 M quantum-common/quantum-common-security/src/main/java/com/alpha/security/config/SecurityProperties.java
 M quantum-common/quantum-common-security/src/main/java/com/alpha/security/filter/RateLimitFilter.java
 M quantum-common/quantum-common-security/src/main/java/com/alpha/security/filter/RepeatSubmitFilter.java
 M quantum-common/quantum-common-security/src/main/java/com/alpha/security/filter/RequestWrapperFilter.java
 M quantum-common/quantum-common-security/src/main/java/com/alpha/security/filter/TokenAuthenticationFilter.java
 M quantum-common/quantum-common-security/src/main/java/com/alpha/security/handler/AccessDeniedHandlerImpl.java
 M quantum-common/quantum-common-security/src/main/java/com/alpha/security/handler/AuthenticationEntryPointImpl.java
 M quantum-common/quantum-common-security/src/main/java/com/alpha/security/handler/LogoutSuccessHandlerImpl.java
 M quantum-common/quantum-common-security/src/main/java/com/alpha/security/service/impl/CaptchaServiceImpl.java
 M quantum-common/quantum-common-security/src/main/java/com/alpha/security/token/TokenInfo.java
 M quantum-common/quantum-common-security/src/main/java/com/alpha/security/token/TokenProperties.java
 M quantum-common/quantum-common-security/src/main/java/com/alpha/security/token/TokenService.java
?? quantum-common/quantum-common-security/src/main/java/com/alpha/security/util/BcryptPasswords.java
?? quantum-common/quantum-common-security/src/main/java/com/alpha/security/util/BearerTokens.java
 M quantum-common/quantum-common-security/src/main/java/com/alpha/security/util/CookieUtil.java
 D quantum-common/quantum-common-security/src/main/java/com/alpha/security/warpper/SecurityRequestWrapper.java
?? quantum-common/quantum-common-security/src/main/java/com/alpha/security/wrapper/SecurityRequestWrapper.java
?? quantum-common/quantum-common-security/src/test/java/com/alpha/security/filter/RequestWrapperFilterTest.java
?? quantum-common/quantum-common-security/src/test/java/com/alpha/security/filter/TokenAuthenticationFilterContractTest.java
?? quantum-common/quantum-common-security/src/test/java/com/alpha/security/service/impl/CaptchaServiceImplTest.java
?? quantum-common/quantum-common-security/src/test/java/com/alpha/security/token/TokenServiceTest.java
 M quantum-common/quantum-common-security/src/test/java/com/alpha/security/util/CookieUtilTest.java
 M quantum-mcp/src/main/java/com/alpha/mcp/config/McpProperties.java
 M quantum-mcp/src/main/java/com/alpha/mcp/config/McpSecurityConfig.java
 M quantum-mcp/src/main/java/com/alpha/mcp/controller/McpController.java
 M quantum-mcp/src/main/java/com/alpha/mcp/controller/OAuthController.java
 M quantum-mcp/src/main/java/com/alpha/mcp/controller/OAuthMetadataController.java
 M quantum-mcp/src/main/java/com/alpha/mcp/manifest/CapabilityManifestService.java
 M quantum-mcp/src/main/java/com/alpha/mcp/oauth/OAuthAccessToken.java
 M quantum-mcp/src/main/java/com/alpha/mcp/oauth/OAuthAuthorizationCode.java
 M quantum-mcp/src/main/java/com/alpha/mcp/oauth/OAuthRefreshToken.java
 M quantum-mcp/src/main/java/com/alpha/mcp/oauth/OAuthTokenService.java
 M quantum-mcp/src/main/java/com/alpha/mcp/security/OAuthBearerAuthenticationFilter.java
?? quantum-mcp/src/test/java/com/alpha/mcp/controller/McpResponseContractTest.java
?? quantum-mcp/src/test/java/com/alpha/mcp/controller/RedisCodecRoundTripTest.java
 M quantum-mcp/src/test/java/com/alpha/mcp/oauth/OAuthTokenServiceTest.java
 M quantum-mcp/src/test/java/com/alpha/mcp/security/OAuthBearerAuthenticationFilterTest.java
 M quantum-server/src/main/java/com/alpha/server/QuantumApplication.java
 M quantum-server/src/main/java/com/alpha/server/startup/StartupDependencyVerifier.java
 M quantum-server/src/main/resources/application.yml
 M quantum-server/src/main/resources/log4j2-spring.xml
```

## 最终验证

`mvn clean test` 退出码 **0**，Reactor **11/11**。
共 **136** 个测试，失败、错误、跳过各 **0**。

`athena run --covers AC2,AC5,AC6 -- mvn clean test` 在构建前尝试建立源码树快照时，Git 对象库拒绝写入，错误为 `unable to create temporary file: Operation not permitted`。以下为在指定 worktree 直接执行相同 Maven 命令的结果。Athena 验收记录和独立 review 留在主 sprint 的收尾阶段。

```text
[INFO] Reactor Summary for quantum-backend 1.0.0:
[INFO] 
[INFO] quantum-backend .................................... SUCCESS [  0.049 s]
[INFO] quantum-common ..................................... SUCCESS [  0.002 s]
[INFO] quantum-common-framework ........................... SUCCESS [  1.565 s]
[INFO] quantum-common-cache ............................... SUCCESS [  3.205 s]
[INFO] quantum-common-security ............................ SUCCESS [  2.331 s]
[INFO] quantum-common-orm ................................. SUCCESS [  1.437 s]
[INFO] quantum-common-logging ............................. SUCCESS [  1.424 s]
[INFO] quantum-common-file ................................ SUCCESS [  1.394 s]
[INFO] quantum-biz-system ................................. SUCCESS [  4.225 s]
[INFO] quantum-mcp ........................................ SUCCESS [  1.964 s]
[INFO] quantum-server ..................................... SUCCESS [  0.190 s]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
```

## 前端接口同步清单

### Web 会话刷新 S17

浏览器路径为 `POST /api/auth/refresh`，网关转发到后端 `POST /auth/refresh`。请求无 body，无需 access token。跨源的同站请求使用 `credentials: "include"` 或 Axios `withCredentials: true`。启用设备绑定的部署应沿用登录时的 `X-Device-Id`。

```http
POST /api/auth/refresh HTTP/1.1
Cookie: refresh_token=<current-refresh-token>
X-Device-Id: <existing-device-id>
```

成功响应。示例使用默认 30 分钟 access token 和勾选记住我后的 7 天 refresh Cookie。

```http
HTTP/1.1 200 OK
Content-Type: application/json
Cache-Control: no-store
Authorization: Bearer <new-access-token>
Set-Cookie: refresh_token=<new-refresh-token>; Path=/api/auth; Max-Age=604800; Secure; HttpOnly; SameSite=Strict
Set-Cookie: refresh_token=; Path=/; Max-Age=0; Secure; HttpOnly; SameSite=Strict

{"code":200,"message":"操作成功","data":{"accessToken":"<new-access-token>","expiresIn":1800},"timestamp":"1794974400000"}
```

失败示例。两条 Cookie 都应由浏览器处理。

```http
HTTP/1.1 401 Unauthorized
Content-Type: application/json
Cache-Control: no-store
Set-Cookie: refresh_token=; Path=/api/auth; Max-Age=0; Secure; HttpOnly; SameSite=Strict
Set-Cookie: refresh_token=; Path=/; Max-Age=0; Secure; HttpOnly; SameSite=Strict

{"code":401,"message":"未认证","timestamp":"1794974400000"}
```

响应会按 Cookie 标准附加 `Expires`，由配置决定 `Secure`。未选择记住我时，新 Cookie 为会话 Cookie。登录也会发放新 Path 的 Cookie 并清理旧 Path，`data` 新增秒数 `expiresIn`，原有 `expireTime` 仍可读取。登出清除两处 Cookie。浏览器的共享 Path 由 `security.refresh-cookie-path` 设置，默认 `/api/auth`，需要覆盖浏览器侧的登录、刷新和登出路由。

前端启动时缺少 access token，先尝试一次刷新。除登录与刷新请求外，接口收到 401 时，共享一个刷新 Promise。刷新成功后更新 access token，再为每个原请求最多重放一次。刷新失败后执行会话 reset 并跳转登录页。

### 字典 S08、S09、E16

表内是后端路径，浏览器请求照常经过 `/api`。`id` 和 `version` 取自详情或列表响应，沿用现有 Long 字符串表示，例如 `"id":"21"`、`"version":"3"`。

| 接口 | 请求 JSON |
|---|---|
| `POST /system/dict/type` | 必填 `dictName`、`dictType`。可选 `business`、`status`、`remark`。 |
| `PUT /system/dict/type` | 创建字段，加必填的 `id`、`version`。已有字典类型的 `dictType` 保持原值。 |
| `POST /system/dict/data` | 必填 `dictType`、`dictLabel`、`dictValue`。可选 `dictSort`、`cssClass`、`listClass`、`isDefault`、`status`、`remark`。 |
| `PUT /system/dict/data` | 创建字段，加必填的 `id`、`version`。允许移动到有权限的字典类型。 |

提交时只发送表中字段。状态值为 0 或 1，`isDefault` 为 `"Y"` 或 `"N"`。非超管无法读取或写入管理员字典。按 ID 读取不可见记录时按空结果处理。管理端 `GET /system/dict/data/list` 接收 `pageNum`、`pageSize`，从 `data.records` 取本页数据，`data.total` 取总数。按类型读取选项的 `GET /system/dict/data/type/{dictType}` 返回数组。

### 用户、菜单与审计

- `POST /system/user/import` 的 `data` 是 `[{row,username,success,message}]`。用文本渲染消息。验证失败时整批回滚，返回失败行，行号含表头。导入最多 1,000 行，导出最多 10,000 行。
- `GET /auth/info` 返回的手机号和邮箱已脱敏。编辑个人信息时，仅提交实际填写的联系方式。
- 已经升级或重置密码的用户可改用户名。改名会结束现有会话，下次使用新用户名登录。变更前仍使用旧格式的用户可先正常登录一次完成自动升级。认证及密码写入以 72 UTF-8 字节为 BCrypt 硬上限，表单继续遵循现有长度规则。
- `GET /system/menu/treeselect` 的 `children` 包含实际子菜单。
- `DELETE /monitor/login/log/clean`、`DELETE /monitor/operlog/clean` 的清空操作只向超管开放。
