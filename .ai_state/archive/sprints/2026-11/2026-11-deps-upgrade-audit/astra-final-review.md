# 组件升级与重构整体复核

## 一、检查命令结果

| 仓库 | 命令 | 退出码 | 测试数 | 本次结果 |
|---|---|---:|---:|---|
| 后端 | `mvn clean test` | 0 | 136 | Reactor 11/11 成功。汇总本次 clean 后的 43 份 Surefire XML，失败、错误、跳过均为 0。 |
| 前端 | `bun run build` | 0 | — | TypeScript 应用、工具及测试代码类型检查通过，Vite 成功生成生产模式产物。 |
| 前端 | `bun test` | 0 | 85 | 21 个测试文件，240 次断言，0 失败。 |
| 前端 | `bun run lint` | 0 | — | ESLint 通过。 |
| 前端 | `bun run knip` | 0 | — | Knip 通过。 |

2026-11-18。后端使用 PATH 中的 Maven 3.9.14、默认 settings、指定本地仓库及 Java 25；前端使用 Bun 1.4.4。前端命令均加 Bun 全局选项 `--no-env-file`。build 和 knip 在 `dist/` 下用当前源码、配置、锁文件和原 node_modules 的隔离副本执行，避开 Vite 主动加载环境文件及路由生成器写入原源码。路由生成结果与原件逐字节一致，检查后已清理临时副本。build 验证默认 API 配置。

审查基线分别为后端 `8176e44`、前端 `f23d2ad`，与两个 HEAD 一致。按基线到当前工作树的 Git diff 及未跟踪清单复核，报告写入前，后端为 132 个跟踪文件改动、44 个未跟踪文件，前端为 147 个跟踪文件改动、37 个未跟踪文件。未将既有报告中的运行结果当成本次实测。另做一次正常字典缓存消息的离线编解码检查，结果见 R08。浏览器多标签、生产反向代理、真实 Redis 集群和 PostgreSQL 事务故障均未联调，涉及它们的判断给出成立条件。

## 二、契约对照

| 契约 | 后端 Controller、DTO 或配置 | 前端 API 或模型 | 结果 |
|---|---|---|---|
| S17 登录 | [AuthController.java:62](../../../quantum-biz-system/src/main/java/com/alpha/system/controller/AuthController.java#L62)、[LoginResponse.java:53](../../../quantum-biz-system/src/main/java/com/alpha/system/dto/response/LoginResponse.java#L53)、[JacksonConfig.java:61](../../../quantum-common/quantum-common-framework/src/main/java/com/alpha/framework/config/JacksonConfig.java#L61) | [src/types/auth.ts:12](../../../../quantum-front-wt-upgrade/src/types/auth.ts#L12)、[src/app/auth/api.ts:45](../../../../quantum-front-wt-upgrade/src/app/auth/api.ts#L45)、[src/app/auth/session.ts:77](../../../../quantum-front-wt-upgrade/src/app/auth/session.ts#L77) | POST /auth/login。成功的 data 含字符串 userId、username、nickname、accessToken、秒数 expiresIn、日期时间 expireTime。refreshToken 在序列化前清空，仅由 HttpOnly Cookie 传送。前端从响应体取 token。 |
| S17 刷新与重放 | [SecurityConfig.java:203](../../../quantum-common/quantum-common-security/src/main/java/com/alpha/security/config/SecurityConfig.java#L203)、[AuthController.java:79](../../../quantum-biz-system/src/main/java/com/alpha/system/controller/AuthController.java#L79)、[TokenAuthenticationFilter.java:48](../../../quantum-common/quantum-common-security/src/main/java/com/alpha/security/filter/TokenAuthenticationFilter.java#L48) | [src/lib/http.ts:37](../../../../quantum-front-wt-upgrade/src/lib/http.ts#L37)、[src/lib/http.ts:97](../../../../quantum-front-wt-upgrade/src/lib/http.ts#L97)、[src/lib/http.ts:212](../../../../quantum-front-wt-upgrade/src/lib/http.ts#L212)、[src/app/auth/session.ts:25](../../../../quantum-front-wt-upgrade/src/app/auth/session.ts#L25) | POST /auth/refresh 无需 access，成功为 data.{accessToken,expiresIn}、Bearer 响应头及新 Cookie；前端只消费 body。失败 401 清 Cookie。一般请求无效 access 触发 401；同一页共享刷新，每请求最多重放一次。跨页身份与轮换见 R01、R02，异常分类见 R12。 |
| Cookie Path | [SecurityProperties.java:79](../../../quantum-common/quantum-common-security/src/main/java/com/alpha/security/config/SecurityProperties.java#L79)、[application.yml:94](../../../quantum-server/src/main/resources/application.yml#L94)、[application-prod.yml:59](../../../quantum-server/src/main/resources/application-prod.yml#L59)、[CookieUtil.java:23](../../../quantum-common/quantum-common-security/src/main/java/com/alpha/security/util/CookieUtil.java#L23)、[LogoutSuccessHandlerImpl.java:46](../../../quantum-common/quantum-common-security/src/main/java/com/alpha/security/handler/LogoutSuccessHandlerImpl.java#L46) | [src/lib/http.ts:9](../../../../quantum-front-wt-upgrade/src/lib/http.ts#L9)、[vite.config.ts:89](../../../../quantum-front-wt-upgrade/vite.config.ts#L89)、[docs/ai/convention-pack/conventions.md:82](../../../../quantum-front-wt-upgrade/docs/ai/convention-pack/conventions.md#L82) | 默认 /api/auth、prod /talent_manger/auth 均与对应浏览器前缀约定匹配。新写、失败及登出同时清理旧 Path=/。HttpOnly、SameSite=Strict，Secure 由配置决定。实际生产构建注入值、网关响应头待验证。若开发代理直接连 prod profile，其路径重写仅改请求，返回的 prod Cookie 不匹配浏览器 /api/auth；该组合需配套改写 Cookie Path。 |
| X-Device-Id | [LoginServiceImpl.java:113](../../../quantum-biz-system/src/main/java/com/alpha/system/service/impl/LoginServiceImpl.java#L113)、[AuthController.java:87](../../../quantum-biz-system/src/main/java/com/alpha/system/controller/AuthController.java#L87)、[TokenService.java:169](../../../quantum-common/quantum-common-security/src/main/java/com/alpha/security/token/TokenService.java#L169) | [src/app/auth/session.ts:75](../../../../quantum-front-wt-upgrade/src/app/auth/session.ts#L75)、[src/lib/http.ts:99](../../../../quantum-front-wt-upgrade/src/lib/http.ts#L99)、[src/lib/http.ts:191](../../../../quantum-front-wt-upgrade/src/lib/http.ts#L191)、[src/lib/http.test.ts:149](../../../../quantum-front-wt-upgrade/src/lib/http.test.ts#L149) | 登录同时发 body.deviceId 与同值请求头；后端登录用 body，刷新用头。两个 Axios 实例均 withCredentials，刷新实例移除 Authorization。启用绑定时设备、IP、UA 都需匹配。 |
| 字典类型 DTO | [SysDictController.java:69](../../../quantum-biz-system/src/main/java/com/alpha/system/controller/SysDictController.java#L69)、[DictTypeCreateRequest.java:11](../../../quantum-biz-system/src/main/java/com/alpha/system/dto/request/DictTypeCreateRequest.java#L11)、[DictTypeUpdateRequest.java:11](../../../quantum-biz-system/src/main/java/com/alpha/system/dto/request/DictTypeUpdateRequest.java#L11) | [src/features/system/dict/model.ts:109](../../../../quantum-front-wt-upgrade/src/features/system/dict/model.ts#L109)、[src/features/system/dict/api.ts:109](../../../../quantum-front-wt-upgrade/src/features/system/dict/api.ts#L109) | 新建/修改字段 dictName、dictType、business、status、remark。修改另传字符串 id/version，保留原类型编码和已有业务分组。二者可绑定后端 Long。 |
| 字典数据、分页与详情 | [DictDataCreateRequest.java:11](../../../quantum-biz-system/src/main/java/com/alpha/system/dto/request/DictDataCreateRequest.java#L11)、[DictDataUpdateRequest.java:11](../../../quantum-biz-system/src/main/java/com/alpha/system/dto/request/DictDataUpdateRequest.java#L11)、[SysDictController.java:104](../../../quantum-biz-system/src/main/java/com/alpha/system/controller/SysDictController.java#L104) | [src/features/system/dict/model.ts:123](../../../../quantum-front-wt-upgrade/src/features/system/dict/model.ts#L123)、[src/features/system/dict/api.ts:142](../../../../quantum-front-wt-upgrade/src/features/system/dict/api.ts#L142)、[src/features/system/dict/api.ts:196](../../../../quantum-front-wt-upgrade/src/features/system/dict/api.ts#L196) | dictType、dictLabel、dictValue、dictSort、cssClass、listClass、isDefault、status、remark；更新带 id/version。isDefault 用 Y/N，status 用 0/1。管理列表传 pageNum/pageSize、读 records/total；按类型选项接口返回数组。按 ID 的空结果两端兼容。跨节点缓存见 R08。 |
| 用户导入 | [SysUserController.java:143](../../../quantum-biz-system/src/main/java/com/alpha/system/controller/SysUserController.java#L143)、[ImportRowResult.java:4](../../../quantum-biz-system/src/main/java/com/alpha/system/dto/response/ImportRowResult.java#L4)、[SysUserServiceImpl.java:315](../../../quantum-biz-system/src/main/java/com/alpha/system/service/impl/SysUserServiceImpl.java#L315) | [src/features/system/user/transfer-api.ts:11](../../../../quantum-front-wt-upgrade/src/features/system/user/transfer-api.ts#L11)、[src/features/system/user/components/users-import-dialog.tsx:135](../../../../quantum-front-wt-upgrade/src/features/system/user/components/users-import-dialog.tsx#L135) | multipart 的 file、updateSupport 匹配。成功或服务层验证失败均能读取 [{row,username,success,message}]；业务码失败可伴 HTTP 200。失败整批回滚，UI 用文本显示。角色单元格格式错误与空白行见 R11。 |
| 菜单树 | [SysMenuController.java:37](../../../quantum-biz-system/src/main/java/com/alpha/system/controller/SysMenuController.java#L37)、[RouterVO.java:20](../../../quantum-biz-system/src/main/java/com/alpha/system/dto/response/RouterVO.java#L20)、[MenuConvert.java:70](../../../quantum-biz-system/src/main/java/com/alpha/system/convert/MenuConvert.java#L70)、[TreeSelectVO.java:45](../../../quantum-biz-system/src/main/java/com/alpha/system/dto/response/TreeSelectVO.java#L45) | [src/app/navigation/api.ts:20](../../../../quantum-front-wt-upgrade/src/app/navigation/api.ts#L20)、[src/features/system/menu/api.ts:43](../../../../quantum-front-wt-upgrade/src/features/system/menu/api.ts#L43)、[src/features/system/role/api.ts:125](../../../../quantum-front-wt-upgrade/src/features/system/role/api.ts#L125) | 导航读取路由树，管理列表读取实体树，角色授权读取 /system/menu/treeselect 的 id/label/children。正常多层节点匹配。树的写入链见 R07，历史环数据见 R16。 |
| 清空登录日志、操作日志 | [SysLoginLogController.java:51](../../../quantum-biz-system/src/main/java/com/alpha/system/controller/SysLoginLogController.java#L51)、[SysOperLogController.java:59](../../../quantum-biz-system/src/main/java/com/alpha/system/controller/SysOperLogController.java#L59)、[LoginUser.java:134](../../../quantum-common/quantum-common-framework/src/main/java/com/alpha/framework/entity/LoginUser.java#L134) | [src/features/monitor/log-clean.ts:6](../../../../quantum-front-wt-upgrade/src/features/monitor/log-clean.ts#L6)、[src/features/monitor/login-log/index.tsx:56](../../../../quantum-front-wt-upgrade/src/features/monitor/login-log/index.tsx#L56)、[src/features/monitor/oper-log/index.tsx:63](../../../../quantum-front-wt-upgrade/src/features/monitor/oper-log/index.tsx#L63) | 两个 DELETE /clean 的地址与 remove 权限字匹配。后端以固定用户 ID 认定超管，前端以角色名认定，两端各自的测试对同一身份给出相反结论，见 R10。 |

## 三、问题与抽查证据

下表位置指向当前工作树。P1 表示身份、撤权、数据完整性或主要操作受影响；P2 表示特定入口或条件下的功能错误；P3 表示低影响性能或文档偏差。“推断”基于正常调用路径、分支条件或事务时序。末行列出本轮必须修复的条目。

| ID | 严重度 | 仓库 | 文件:行 | 问题 | 依据 | 建议 |
|---|---|---|---|---|---|---|
| R01 | P1 | 两端 | [src/lib/http.ts:116](../../../../quantum-front-wt-upgrade/src/lib/http.ts#L116)、[src/lib/http.ts:240](../../../../quantum-front-wt-upgrade/src/lib/http.ts#L240)、[src/stores/auth-store.ts:97](../../../../quantum-front-wt-upgrade/src/stores/auth-store.ts#L97)、[AuthController.java:103](../../../quantum-biz-system/src/main/java/com/alpha/system/controller/AuthController.java#L103) | 另一标签换号后，旧页面可能以新账号身份重放写请求。 | S17 新路径。Cookie 和设备 ID 跨页共享，user、请求代次与 Query 缓存留在各页。原页面 access 到期后，refresh 仅回 token 和秒数，接收方只换 token；旧请求的本页代次仍有效。最终按另一账号的权限执行。 | 登录、登出和换号向同源标签传播身份边界；重放前核对稳定的服务端会话身份，身份改变时取消旧请求并清私有缓存。 |
| R02 | P1 | 两端 | [src/lib/http.ts:42](../../../../quantum-front-wt-upgrade/src/lib/http.ts#L42)、[src/main.tsx:37](../../../../quantum-front-wt-upgrade/src/main.tsx#L37)、[TokenService.java:197](../../../quantum-common/quantum-common-security/src/main/java/com/alpha/security/token/TokenService.java#L197)、[AuthController.java:93](../../../quantum-biz-system/src/main/java/com/alpha/system/controller/AuthController.java#L93) | 同账号两个标签同时恢复时，一次失败响应可能清掉刚轮换的 Cookie。 | 每个标签启动时有独立 Promise。同一旧 Cookie 只能消费一次；若清除 Cookie 的 401 后到，浏览器失去新 Cookie。即使顺序刷新，后端也会撤销上一标签的 access（TokenService:210），导致后续请求额外刷新。 | 协调跨页刷新和结果分发；为已过时的失败响应设计不会覆盖新会话的处理。用同一浏览器 Cookie 容器检查两种响应顺序。 |
| R03 | P1 | 后端 | [TokenService.java:186](../../../quantum-common/quantum-common-security/src/main/java/com/alpha/security/token/TokenService.java#L186)、[TokenService.java:204](../../../quantum-common/quantum-common-security/src/main/java/com/alpha/security/token/TokenService.java#L204)、[TokenService.java:252](../../../quantum-common/quantum-common-security/src/main/java/com/alpha/security/token/TokenService.java#L252)、[UserSessionInvalidationService.java:31](../../../quantum-biz-system/src/main/java/com/alpha/system/security/UserSessionInvalidationService.java#L31) | 强制下线与刷新之间存在空档，改密或强退后仍可产生有效新会话。 | 服务先载入用户、消费旧 refresh，再登记新 token；kickOut 枚举当时的索引后逐项删除。撤销若位于消费与登记之间，新会话不会出现在快照中；固定 JWT 到期和单次消费都未堵住该时序。 | 在登录、刷新和撤销之间使用同一权威用户会话版本；登记新会话时原子校验版本，补强退并发测试。 |
| R04 | P1 | 后端 | [LocalCacheClient.java:24](../../../quantum-common/quantum-common-cache/src/main/java/com/alpha/cache/util/LocalCacheClient.java#L24)、[LocalCacheClient.java:136](../../../quantum-common/quantum-common-cache/src/main/java/com/alpha/cache/util/LocalCacheClient.java#L136)、[TokenService.java:139](../../../quantum-common/quantum-common-security/src/main/java/com/alpha/security/token/TokenService.java#L139)、[TokenService.java:253](../../../quantum-common/quantum-common-security/src/main/java/com/alpha/security/token/TokenService.java#L253) | local 模式在容量压力下可能遗漏仍有效的会话。 | E08 将用户→会话集合并入 50,000 键的同一 Caffeine 缓存，各键独立淘汰。若反向索引先丢失，强退直接返回；存活的 access 仍可校验，refresh 映射仍可轮换。基线的集合独立存储。 | 将撤销依据与普通缓存淘汰分开，或让索引与会话保持一致生命周期；测试独立淘汰索引后再撤销。 |
| R05 | P1 | 后端 | [MonitorController.java:63](../../../quantum-biz-system/src/main/java/com/alpha/system/controller/MonitorController.java#L63)、[SysOnlineServiceImpl.java:43](../../../quantum-biz-system/src/main/java/com/alpha/system/service/impl/SysOnlineServiceImpl.java#L43)、[OAuthTokenService.java:150](../../../quantum-mcp/src/main/java/com/alpha/mcp/oauth/OAuthTokenService.java#L150)、[OAuthTokenService.java:225](../../../quantum-mcp/src/main/java/com/alpha/mcp/oauth/OAuthTokenService.java#L225) | 在线管理的“按用户强退”只撤销 Web 会话，启用 MCP 时原 OAuth 会话继续有效。 | S02 的 MCP 撤销版本由事务 ForceLogoutEvent 更新，监控入口直接调用 Web kickOut，未发该事件，也未改变用户启用状态。MCP access 验证及 refresh 兑换继续使用旧版本。 | 让所有按用户强退入口执行 Web 与 MCP 的统一撤销；单设备/单会话操作保留独立语义。 |
| R06 | P1 | 后端 | [SysFileServiceImpl.java:44](../../../quantum-biz-system/src/main/java/com/alpha/system/service/impl/SysFileServiceImpl.java#L44)、[SysFileServiceImpl.java:103](../../../quantum-biz-system/src/main/java/com/alpha/system/service/impl/SysFileServiceImpl.java#L103) | 上传补偿把事务结果 UNKNOWN 当作已回滚，可能删掉已登记的唯一文件。 | E15 新增 afterCompletion 以 status != COMMITTED 触发删除。若数据库提交成功但提交确认丢失，元数据可能存在，原文件却被补偿删除。正常按 ID 删除则确实在 afterCommit 后清理。 | 仅在明确 ROLLED_BACK 时直接删除；UNKNOWN 保留对象，按业务标识对账后处置。 |
| R07 | P1 | 两端 | [src/features/system/menu/model.ts:93](../../../../quantum-front-wt-upgrade/src/features/system/menu/model.ts#L93)、[src/features/system/role/model.ts:117](../../../../quantum-front-wt-upgrade/src/features/system/role/model.ts#L117)、[src/features/system/dept/model.ts:99](../../../../quantum-front-wt-upgrade/src/features/system/dept/model.ts#L99)；[MenuUpdateRequest.java:23](../../../quantum-biz-system/src/main/java/com/alpha/system/dto/request/MenuUpdateRequest.java#L23)、[RoleUpdateRequest.java:31](../../../quantum-biz-system/src/main/java/com/alpha/system/dto/request/RoleUpdateRequest.java#L31)、[DeptUpdateRequest.java:30](../../../quantum-biz-system/src/main/java/com/alpha/system/dto/request/DeptUpdateRequest.java#L30) | 菜单、角色和部门的编辑请求缺少必填 version；菜单另把 id 发成 menuId。 | 三个 PUT 均对请求体做 @Validated。按页面的 payload 保存会在服务层之前返回 400，角色菜单授权的编辑也无法进入保存后的缓存失效回调。字段错配在基线已存在。 | 三张表单从本次详情冻结 id/version，按 DTO 发回；分别覆盖正常保存和过期版本冲突。 |
| R08 | P2 | 后端 | [MultiLevelCacheConfig.java:47](../../../quantum-common/quantum-common-cache/src/main/java/com/alpha/cache/config/MultiLevelCacheConfig.java#L47)、[MultiLevelCacheConfig.java:83](../../../quantum-common/quantum-common-cache/src/main/java/com/alpha/cache/config/MultiLevelCacheConfig.java#L83)、[MultiLevelCacheConfig.java:264](../../../quantum-common/quantum-common-cache/src/main/java/com/alpha/cache/config/MultiLevelCacheConfig.java#L264)、[RedisUtil.java:183](../../../quantum-common/quantum-common-cache/src/main/java/com/alpha/cache/util/RedisUtil.java#L183)、[RedisConfig.java:43](../../../quantum-common/quantum-common-cache/src/main/java/com/alpha/cache/config/RedisConfig.java#L43) | Redis 的跨节点字典失效消息不能用当前 topic codec 往返。 | **本机离线实测**：把正常 SyncMessage 交给生产 CacheJsonCodec 编码，再用同一 codec 的 value decoder 读取，Java 退出 1，InvalidTypeIdException，缺少 @class。发布/订阅使用同一默认 topic codec；发布成功不能证明对端成功消费。旧设计已用 record，本轮 D13 将字典 L1 设为 30 分钟。 | 为此 topic 指定双方一致的定型 codec；将真实 SyncMessage 加入往返测试，补两个缓存实例之间的失效检查。 |
| R09 | P2 | 两端 | [SysUserController.java:95](../../../quantum-biz-system/src/main/java/com/alpha/system/controller/SysUserController.java#L95)、[UserConvert.java:62](../../../quantum-biz-system/src/main/java/com/alpha/system/convert/UserConvert.java#L62)、[SysUserServiceImpl.java:172](../../../quantum-biz-system/src/main/java/com/alpha/system/service/impl/SysUserServiceImpl.java#L172)、[src/features/system/user/components/users-action-dialog.tsx:311](../../../../quantum-front-wt-upgrade/src/features/system/user/components/users-action-dialog.tsx#L311) | 密码迁移后的用户改名承诺无法从真实入口完成。 | D03 的密码哈希升级有实现。更新 DTO 要求 username，但转换器没有复制该字段，业务层改名及强制下线分支不会进入。UI 编辑态也禁用了用户名。现有密码测试直接构造已改名的认证用户。 | 贯通表单、映射、唯一性校验和撤销；从 PUT 验证随后能以新名登录，旧名和旧会话失效。 |
| R10 | P2 | 两端 | [src/features/monitor/log-clean.ts:6](../../../../quantum-front-wt-upgrade/src/features/monitor/log-clean.ts#L6)、[src/app/auth/access.ts:30](../../../../quantum-front-wt-upgrade/src/app/auth/access.ts#L30)、[src/features/monitor/log-clean.test.ts:23](../../../../quantum-front-wt-upgrade/src/features/monitor/log-clean.test.ts#L23)、[AuditLogClearAuthorizationTest.java:34](../../../quantum-biz-system/src/test/java/com/alpha/system/controller/AuditLogClearAuthorizationTest.java#L34) | 两类清日志按钮显示对象与服务端实际允许对象不一致。 | 后端测试明确拒绝 ID=2 且带 admin 和 remove 的用户，允许 ID=1 无角色；前端测试却允许 roles=[admin] 且 permissions=[]。可产生多余按钮/403，也可对真正超管隐藏按钮。后端清表鉴权实际有效。 | 用 /auth/info 返回的权威能力或同一超管身份判定控制按钮，并在两端用相同身份样例测试。 |
| R11 | P2 | 后端 | [SysUserController.java:166](../../../quantum-biz-system/src/main/java/com/alpha/system/controller/SysUserController.java#L166)、[SysUserController.java:175](../../../quantum-biz-system/src/main/java/com/alpha/system/controller/SysUserController.java#L175)、[ExcelUtil.java:28](../../../quantum-common/quantum-common-file/src/main/java/com/alpha/file/util/ExcelUtil.java#L28)、[SysUserServiceImpl.java:330](../../../quantum-biz-system/src/main/java/com/alpha/system/service/impl/SysUserServiceImpl.java#L330) | 导入的单元格格式错误可能无行明细，空白行也会让后续报错指错行。 | 角色 ID 转换先于服务层汇总，遇非法格式直接返回无 data 的普通失败。读取器仅保存对象，FastExcel 1.3.0 默认跳过空行，服务用集合下标 + 2 作行号。表头后空一行时，原表第 3 行会显示为第 2 行。 | 读取时携带工作表物理行号，将转换与 Bean Validation 错误合并为统一行结果；保留整批回滚。 |
| R12 | P2 | 两端 | [TokenService.java:185](../../../quantum-common/quantum-common-security/src/main/java/com/alpha/security/token/TokenService.java#L185)、[AuthController.java:89](../../../quantum-biz-system/src/main/java/com/alpha/system/controller/AuthController.java#L89)、[src/lib/http.ts:119](../../../../quantum-front-wt-upgrade/src/lib/http.ts#L119)、[src/lib/http.ts:227](../../../../quantum-front-wt-upgrade/src/lib/http.ts#L227) | 暂时性依赖故障被当作凭证失效，浏览器丢失仍可重试的登录。 | 用户查询短暂失败时 refresh 尚未消费，服务返回 null；控制器发送 401 并清 Cookie。前端对非取消类刷新错误也重置会话。设计将失败笼统定义为 401，此处需要调整契约。 | 区分凭证拒绝和暂时服务故障，对可重试的 5xx 保留 Cookie 与会话恢复机会。 |
| R13 | P2 | 后端 | [AsyncConfig.java:30](../../../quantum-common/quantum-common-logging/src/main/java/com/alpha/logging/config/AsyncConfig.java#L30)、[SysLoginLogServiceImpl.java:32](../../../quantum-biz-system/src/main/java/com/alpha/system/service/impl/SysLoginLogServiceImpl.java#L32)、[LoginServiceImpl.java:110](../../../quantum-biz-system/src/main/java/com/alpha/system/service/impl/LoginServiceImpl.java#L110)、[LoginServiceImpl.java:202](../../../quantum-biz-system/src/main/java/com/alpha/system/service/impl/LoginServiceImpl.java#L202) | 登录审计积压可阻断密码正确的登录，且请求线程最多等待一秒。 | E10 将共享异步执行器改为有界队列。队列满时异常来自代理的提交步骤，早于任务体中的 try；成功登录先提交日志、后签发 token。此为分支与时序推断，未做负载测试。 | 明确审计不可用时的登录策略，隔离投递并告警；测试队列饱和时客户端结果及审计保留。 |
| R14 | P2 | 前端 | [src/features/errors/general-error/index.tsx:39](../../../../quantum-front-wt-upgrade/src/features/errors/general-error/index.tsx#L39)、[src/app/navigation/page-registry.tsx:16](../../../../quantum-front-wt-upgrade/src/app/navigation/page-registry.tsx#L16)、[src/lib/preload-recovery.ts:1](../../../../quantum-front-wt-upgrade/src/lib/preload-recovery.ts#L1) | 懒加载失败后，页面“重试”会复用已失败的 React.lazy 对象。 | F18 增加 router.invalidate() 与边界 reset，但模块级 lazy 对象会缓存导入的拒绝。一轮自动整页重载后若再次失败，点击就地重试仍抛出缓存异常。 | 手动重试应能重新创建 loader 或明确执行整页重载；验证网络恢复后点击能进入页面。 |
| R15 | P2 | 后端 | [SysFileServiceImpl.java:126](../../../quantum-biz-system/src/main/java/com/alpha/system/service/impl/SysFileServiceImpl.java#L126)、[SysFileServiceImpl.java:140](../../../quantum-biz-system/src/main/java/com/alpha/system/service/impl/SysFileServiceImpl.java#L140)、[deploy/init.sql:201](../../../deploy/init.sql#L201) | 按路径删除只删一行元数据，历史重复路径可留下无法下载的记录。 | 基线按路径处理匹配集合。当前 limit(1)，底层物理文件随之删除；仓内 sys_file DDL 未对 file_path 设唯一约束。仅有一行时不触发。 | 以路径处理所有匹配行并在提交后清一次对象，或先对账迁移并将唯一性固化。 |
| R16 | P2 | 后端 | [SysMenuMapper.xml:16](../../../quantum-biz-system/src/main/resources/mapper/SysMenuMapper.xml#L16)、[SysMenuServiceImpl.java:155](../../../quantum-biz-system/src/main/java/com/alpha/system/service/impl/SysMenuServiceImpl.java#L155)、[SysMenuServiceImpl.java:100](../../../quantum-biz-system/src/main/java/com/alpha/system/service/impl/SysMenuServiceImpl.java#L100) | 菜单存在祖孙环时，普通用户路由查询可能在数据库递归阶段耗尽资源或超时。 | E13 的 Java 防环只对已加载的节点生效。菜单更新仅排除父 ID 等于自身；递归 SQL 使用 UNION ALL，从已分配节点向上遇环会重复生成行。此为基线遗留，当前改动只保护了 Java 层。 | 写入时拒绝以子孙作父节点；查询对历史环做路径检测或递归去重。 |
| R17 | P2 | 后端 | [SystemDataScope.java:17](../../../quantum-common/quantum-common-orm/src/main/java/com/alpha/orm/permission/SystemDataScope.java#L17)、[DataScopeAspect.java:60](../../../quantum-common/quantum-common-orm/src/main/java/com/alpha/orm/aspect/DataScopeAspect.java#L60)、[SysUserServiceImpl.java:73](../../../quantum-biz-system/src/main/java/com/alpha/system/service/impl/SysUserServiceImpl.java#L73)、[SysUserServiceImpl.java:589](../../../quantum-biz-system/src/main/java/com/alpha/system/service/impl/SysUserServiceImpl.java#L589) | S05 的新限制也拒绝显式授权的系统级用户查询。 | SystemDataScope.execute 文档列出 selectUserList 用法，切面已为系统调用设置 ALL。新过滤器只读 UserContext；系统任务没有登录用户时强加恒假条件。仓内未找到受影响的生产任务调用。 | 让列表与导出尊重显式系统上下文，同时继续拒绝普通缺身份调用；测试两个上下文。 |
| R18 | P3 | 前端 | [.ai_state/sprints/2026-11-front-upgrade-audit/upgrade-report.md:87](../../../../quantum-front-wt-upgrade/.ai_state/sprints/2026-11-front-upgrade-audit/upgrade-report.md#L87)、[src/lib/http.test.ts:193](../../../../quantum-front-wt-upgrade/src/lib/http.test.ts#L193) | 报告仍写刷新支持响应头回退。 | 实现只读 body，专门的负向测试会拒绝只有响应头的 token。最新设计与当前实现一致。 | 修正旧报告对回退行为的描述或明确该段仅为历史快照。 |
| R19 | P3 | 后端 | [SysDictDataServiceImpl.java:93](../../../quantum-biz-system/src/main/java/com/alpha/system/service/impl/SysDictDataServiceImpl.java#L93)、[SysDictDataServiceImpl.java:123](../../../quantum-biz-system/src/main/java/com/alpha/system/service/impl/SysDictDataServiceImpl.java#L123) | 批量删除字典数据新增按行重复的类型权限查询。 | 先批取记录，随后逐条 canReadType；N 条同类型数据会额外发 N 次相同 count。未测数据库耗时。 | 在同一事务中按不同 dictType 批量校验，任一类型不允许时整批拒绝。 |
| R20 | P2 | 两端 | [SysDeptController.java:62](../../../quantum-biz-system/src/main/java/com/alpha/system/controller/SysDeptController.java#L62)、[DeptVO.java:64](../../../quantum-biz-system/src/main/java/com/alpha/system/dto/response/DeptVO.java#L64)、[src/features/system/dept/model.ts:122](../../../../quantum-front-wt-upgrade/src/features/system/dept/model.ts#L122)、[src/features/system/dept/form.ts:33](../../../../quantum-front-wt-upgrade/src/features/system/dept/form.ts#L33) | S03 生效后，部门编辑把脱敏联系方式当成可保存的原值。 | 部门详情也返回 DeptVO。只修改部门名时，脱敏手机号过不了前端格式校验；掩码邮箱可通过现有邮箱正则。即使补齐 R07 的 version，原表单仍可能发送掩码。 | 联系方式以脱敏提示显示，仅提交用户新填写的值，并在后端保留未修改字段。 |

### 合并清单抽查

表中“符合”限于列出的行为。后端逐项核对 22 个 ID，前端核对 20 个 ID。部分条目虽有通过的单测，真实入口或并发条件仍留有表中缺口。

| 后端 merged-findings ID | 当前代码或测试 | 抽查结论 |
|---|---|---|
| S01 | [SysMenuMapper.xml:11](../../../quantum-biz-system/src/main/resources/mapper/SysMenuMapper.xml#L11)、[SysMenuMapper.xml:30](../../../quantum-biz-system/src/main/resources/mapper/SysMenuMapper.xml#L30) | 符合。路由和权限 SQL 均过滤停用、已删角色，递归父节点过滤停用菜单。 |
| S02 | [OAuthTokenService.java:130](../../../quantum-mcp/src/main/java/com/alpha/mcp/oauth/OAuthTokenService.java#L130)、[OAuthTokenService.java:219](../../../quantum-mcp/src/main/java/com/alpha/mcp/oauth/OAuthTokenService.java#L219) | 部分。验证时重载用户、复核版本；监控强退漏撤销 MCP，见 R05。 |
| S03 | [Sensitive.java:20](../../../quantum-common/quantum-common-security/src/main/java/com/alpha/security/annotation/Sensitive.java#L20)、[SensitiveUserVOTest.java:17](../../../quantum-biz-system/src/test/java/com/alpha/system/controller/SensitiveUserVOTest.java#L17) | UserVO 手机、邮箱脱敏已验证；DeptVO 编辑消费方未同步，见 R20。 |
| S04 | [RoleDelegationPolicy.java:46](../../../quantum-biz-system/src/main/java/com/alpha/system/security/RoleDelegationPolicy.java#L46)、[RoleDelegationPolicy.java:119](../../../quantum-biz-system/src/main/java/com/alpha/system/security/RoleDelegationPolicy.java#L119)、[SysUserServiceImpl.java:139](../../../quantum-biz-system/src/main/java/com/alpha/system/service/impl/SysUserServiceImpl.java#L139)、[SysUserServiceImpl.java:384](../../../quantum-biz-system/src/main/java/com/alpha/system/service/impl/SysUserServiceImpl.java#L384) | 已查新增、更新、导入的委派入口及角色定义更新。非超管对受保护角色、ALL 数据域及自身不具备的权限有服务层限制。 |
| S05 | [SysUserServiceImpl.java:596](../../../quantum-biz-system/src/main/java/com/alpha/system/service/impl/SysUserServiceImpl.java#L596)、[SysUserServiceImpl.java:559](../../../quantum-biz-system/src/main/java/com/alpha/system/service/impl/SysUserServiceImpl.java#L559) | 登录用户的本人、超管、部门限制可对应到列表与详情；显式系统调用见 R17。 |
| S11 | [TraceFilter.java:42](../../../quantum-common/quantum-common-logging/src/main/java/com/alpha/logging/filter/TraceFilter.java#L42) | 符合。入站追踪 ID 有字符集及长度限制。 |
| S12 | [LoginServiceImpl.java:59](../../../quantum-biz-system/src/main/java/com/alpha/system/service/impl/LoginServiceImpl.java#L59)、[LoginServiceImpl.java:85](../../../quantum-biz-system/src/main/java/com/alpha/system/service/impl/LoginServiceImpl.java#L85) | 符合。验证码错误在记录口令失败次数前返回。 |
| S13 | [CaptchaServiceImpl.java:90](../../../quantum-common/quantum-common-security/src/main/java/com/alpha/security/service/impl/CaptchaServiceImpl.java#L90) | 符合。验证码通过 getAndDelete 消费。 |
| S15 | [SysUserServiceImpl.java:330](../../../quantum-biz-system/src/main/java/com/alpha/system/service/impl/SysUserServiceImpl.java#L330)、[SysUserController.java:171](../../../quantum-biz-system/src/main/java/com/alpha/system/controller/SysUserController.java#L171) | 部分。服务层能返回结构化错误并触发回滚；转换错误及物理行号见 R11。 |
| S17 | [AuthController.java:93](../../../quantum-biz-system/src/main/java/com/alpha/system/controller/AuthController.java#L93)、[RefreshContractTest.java:1](../../../quantum-biz-system/src/test/java/com/alpha/system/controller/RefreshContractTest.java#L1) | 单请求成功/失败及 Cookie 路径成立；正常多标签与撤销竞态见 R01–R03。 |
| S18 | [TokenService.java:133](../../../quantum-common/quantum-common-security/src/main/java/com/alpha/security/token/TokenService.java#L133)、[TokenService.java:304](../../../quantum-common/quantum-common-security/src/main/java/com/alpha/security/token/TokenService.java#L304) | 验 JWT 固定到期，刷新权限用 replaceIfPresent；确实没有在普通访问时延长 JWT。 |
| S19 | [RedisConfig.java:86](../../../quantum-common/quantum-common-cache/src/main/java/com/alpha/cache/config/RedisConfig.java#L86)、[RedisConfig.java:98](../../../quantum-common/quantum-common-cache/src/main/java/com/alpha/cache/config/RedisConfig.java#L98)、[RedisCodecRoundTripTest.java:38](../../../quantum-mcp/src/test/java/com/alpha/mcp/controller/RedisCodecRoundTripTest.java#L38) | 包名前缀准许 com.alpha.*、java.* 及规定数组；正反例实测通过。这是包级许可，测试覆盖当前 Web/MCP 对象，不证明所有存量字节或 Pub/Sub 类型兼容。正常消息的实测反例见 R08。 |
| S21 | [SecurityRequestWrapper.java:44](../../../quantum-common/quantum-common-security/src/main/java/com/alpha/security/wrapper/SecurityRequestWrapper.java#L44) | 符合。JSON 实际读取长度受限，涵盖未知 Content-Length。 |
| S22 | [OAuthTokenService.java:72](../../../quantum-mcp/src/main/java/com/alpha/mcp/oauth/OAuthTokenService.java#L72) | 符合。兑换路径先检查缺失的 verifier 再消费授权码。 |
| S23 | [AuditLogClearAuthorizationTest.java:34](../../../quantum-biz-system/src/test/java/com/alpha/system/controller/AuditLogClearAuthorizationTest.java#L34) | 服务端真实权限切面测试通过；前端表现见 R10。 |
| S24 | [SysUser.java:32](../../../quantum-biz-system/src/main/java/com/alpha/system/domain/SysUser.java#L32) | 符合。实体 password 标记 JsonIgnore。 |
| D03 | [SecurityConfig.java:95](../../../quantum-common/quantum-common-security/src/main/java/com/alpha/security/config/SecurityConfig.java#L95)、[SysUserMapper.java:44](../../../quantum-biz-system/src/main/java/com/alpha/system/mapper/SysUserMapper.java#L44)、[PasswordMigrationTest.java:46](../../../quantum-biz-system/src/test/java/com/alpha/system/controller/PasswordMigrationTest.java#L46) | 老格式登录后用旧哈希作条件，只更新密码列，新格式使用 {bcrypt}。改名的端到端承诺见 R09。 |
| D07 | [TreeSelectVO.java:51](../../../quantum-biz-system/src/main/java/com/alpha/system/dto/response/TreeSelectVO.java#L51)、[MenuConvert.java:70](../../../quantum-biz-system/src/main/java/com/alpha/system/convert/MenuConvert.java#L70) | 符合。菜单选项转换保留实际 children。 |
| E01 | [TokenService.java:60](../../../quantum-common/quantum-common-security/src/main/java/com/alpha/security/token/TokenService.java#L60)、[TokenService.java:139](../../../quantum-common/quantum-common-security/src/main/java/com/alpha/security/token/TokenService.java#L139) | 符合。验签对象复用，access 路径一次解析、一次会话查找。 |
| E09 | [MultiLevelCacheConfig.java:264](../../../quantum-common/quantum-common-cache/src/main/java/com/alpha/cache/config/MultiLevelCacheConfig.java#L264) | 发布改为异步并记录发布失败；接收端往返失败见 R08。 |
| E15 | [SysFileServiceImpl.java:98](../../../quantum-biz-system/src/main/java/com/alpha/system/service/impl/SysFileServiceImpl.java#L98)、[SysFileServiceImpl.java:116](../../../quantum-biz-system/src/main/java/com/alpha/system/service/impl/SysFileServiceImpl.java#L116) | 批删提交后清存储，已有日志记录清理失败；清单已披露无持久重试。上传补偿见 R06。 |
| E19 | [SysUserMapper.java:37](../../../quantum-biz-system/src/main/java/com/alpha/system/mapper/SysUserMapper.java#L37) | 符合。最近登录更新仅涉及 IP、地点、时间三列。 |

| 前端 merged-findings ID | 当前代码或测试 | 抽查结论 |
|---|---|---|
| F14 | [src/app/auth/session.ts:25](../../../../quantum-front-wt-upgrade/src/app/auth/session.ts#L25) | 无内存 access 的页面启动先刷新再读用户；多标签见 R02。 |
| F15 | [src/lib/http.ts:91](../../../../quantum-front-wt-upgrade/src/lib/http.ts#L91)、[src/lib/http.ts:214](../../../../quantum-front-wt-upgrade/src/lib/http.ts#L214) | 单标签共享刷新且每请求限一次重放；跨页见 R01、R02。 |
| F2 | [src/main.tsx:23](../../../../quantum-front-wt-upgrade/src/main.tsx#L23)、[src/features/settings/profile/components/profile-account-form.tsx:188](../../../../quantum-front-wt-upgrade/src/features/settings/profile/components/profile-account-form.tsx#L188) | 同页身份变更会清私有数据；跨页身份尚未同步。 |
| F16 | [src/app/navigation/use-menu-routers.ts:13](../../../../quantum-front-wt-upgrade/src/app/navigation/use-menu-routers.ts#L13) | 导航查询在用户和 access 就绪后启用，保留 mock 例外。 |
| F17 | [src/features/system/user/components/users-table.tsx:75](../../../../quantum-front-wt-upgrade/src/features/system/user/components/users-table.tsx#L75)、[src/features/system/role/components/roles-table.tsx:80](../../../../quantum-front-wt-upgrade/src/features/system/role/components/roles-table.tsx#L80) | 已查实体 ID 行选择及分页/数据变化后清选中。 |
| F3 | [src/lib/sanitize-redirect.ts:5](../../../../quantum-front-wt-upgrade/src/lib/sanitize-redirect.ts#L5) | 回跳使用统一站内目标校验。 |
| F4 | [src/stores/auth-store.ts:92](../../../../quantum-front-wt-upgrade/src/stores/auth-store.ts#L92) | 持久化迁移保留设备 ID，移除用户资料。 |
| F5 | [src/features/system/file/components/file-preview-dialog.tsx:39](../../../../quantum-front-wt-upgrade/src/features/system/file/components/file-preview-dialog.tsx#L39) | 预览按允许扩展名确定 MIME。 |
| F8 | [src/components/data-table/debounced-search-input.tsx:23](../../../../quantum-front-wt-upgrade/src/components/data-table/debounced-search-input.tsx#L23)、[src/features/system/user/api.ts:102](../../../../quantum-front-wt-upgrade/src/features/system/user/api.ts#L102) | 输入防抖，列表向 Axios 传 signal。 |
| F9 | [src/features/system/user/components/users-table.tsx:60](../../../../quantum-front-wt-upgrade/src/features/system/user/components/users-table.tsx#L60) | 用户权限列定义已使用 useMemo。 |
| F10 | [src/features/system/menu/index.tsx:157](../../../../quantum-front-wt-upgrade/src/features/system/menu/index.tsx#L157)、[src/features/system/role/components/roles-action-dialog.tsx:141](../../../../quantum-front-wt-upgrade/src/features/system/role/components/roles-action-dialog.tsx#L141) | 变更成功时有失效回调；菜单、角色编辑在真实 DTO 校验处先失败，见 R07。 |
| F18 | [src/features/errors/general-error/index.tsx:39](../../../../quantum-front-wt-upgrade/src/features/errors/general-error/index.tsx#L39) | 仅部分闭环，见 R14。 |
| F19 | [src/features/system/dept/api.ts:90](../../../../quantum-front-wt-upgrade/src/features/system/dept/api.ts#L90) | 部门树选项查询配置在用户/角色编辑处共用。 |
| R4 | [src/features/system/user/index.tsx:61](../../../../quantum-front-wt-upgrade/src/features/system/user/index.tsx#L61)、[src/features/monitor/oper-log/index.tsx:179](../../../../quantum-front-wt-upgrade/src/features/monitor/oper-log/index.tsx#L179) | 用户、角色、文件及两类日志页面对占位数据加守卫后才纠正页码。 |
| R5 | [src/features/settings/profile/components/profile-account-form.tsx:253](../../../../quantum-front-wt-upgrade/src/features/settings/profile/components/profile-account-form.tsx#L253) | 保存后以 staleTime=0 重取资料。 |
| R6 | [src/features/system/user/components/users-action-dialog.tsx:183](../../../../quantum-front-wt-upgrade/src/features/system/user/components/users-action-dialog.tsx#L183)、[src/features/system/role/components/roles-action-dialog.tsx:156](../../../../quantum-front-wt-upgrade/src/features/system/role/components/roles-action-dialog.tsx#L156) | 本次打开时等待查询结束，初始化后保留表单草稿。 |
| R7 | [src/features/monitor/online/index.tsx:148](../../../../quantum-front-wt-upgrade/src/features/monitor/online/index.tsx#L148)、[src/features/monitor/online/index.tsx:473](../../../../quantum-front-wt-upgrade/src/features/monitor/online/index.tsx#L473) | 批量强退按会话 ID 记录结果，部分失败后仍刷新并保留失败目标。 |
| O3 | [eslint.config.js:41](../../../../quantum-front-wt-upgrade/eslint.config.js#L41) | 已查基于项目类型信息的 Promise lint 规则，lint 实跑通过。 |
| O4 | [tsconfig.json:6](../../../../quantum-front-wt-upgrade/tsconfig.json#L6)、[tsconfig.test.json:1](../../../../quantum-front-wt-upgrade/tsconfig.test.json#L1) | build 会检查测试的 TS 类型。 |
| E4 | [src/components/layout/header-breadcrumb.tsx:43](../../../../quantum-front-wt-upgrade/src/components/layout/header-breadcrumb.tsx#L43)、[src/app/navigation/use-navigation.ts:10](../../../../quantum-front-wt-upgrade/src/app/navigation/use-navigation.ts#L10) | 面包屑和导航复用菜单查询。 |

**删除引用与性能范围。** 后端 6 个删除路径含旧 wrapper 包名及字典 mapper XML，核查了 Java 调用、类名字符串、mapper namespace/语句和相关非敏感配置引用；前端 19 个删除路径核查静态导入、路由注册、动态加载和模板，未发现项目内仍加载已删路径的运行入口。DB、Redis、存储的真实吞吐与 P99 待验证。除 R08、R13、R19，MCP 每次验证还会重查用户、角色/权限、部门信息，属于 S02 将撤权即时生效带来的额外查询成本，未将其定性为未经测量的延迟回归。[OAuthTokenService.java:224](../../../quantum-mcp/src/main/java/com/alpha/mcp/oauth/OAuthTokenService.java#L224)、[UserDetailsServiceImpl.java:70](../../../quantum-biz-system/src/main/java/com/alpha/system/service/impl/UserDetailsServiceImpl.java#L70)。

## 四、结论

发布前需关闭下列身份、撤权、数据完整性和已承诺功能问题。R15、R16、R17 分别依赖重复存量路径、菜单环或系统内部调用，列为条件性风险；R18、R19 为低优先级。

NEEDS-FIX（R01、R02、R03、R04、R05、R06、R07、R08、R09、R10、R11、R12、R13、R14、R20）

## 五、前端写接口与后端绑定对照（R07）

路径为浏览器 API 前缀之后的路径。JSON 中的 Long 主键、外键和版本使用字符串，列表类路径参数和角色部门参数按后端的集合绑定用逗号分隔。导出虽使用 POST，也计入对照。

| 方法与路径 | 前端调用 | 实际字段与传输位置 | 后端 DTO 或绑定 | 对照结果 |
|---|---|---|---|---|
| POST /auth/login | auth/api `loginByPassword` | JSON `username,captchaCode,captchaKey,password,rememberMe,deviceId` | `LoginRequest` | 一致 |
| POST /auth/refresh | lib/http 刷新请求 | refresh Cookie、`X-Device-Id` 请求头，无 JSON | `AuthController.refresh` | 一致 |
| POST /auth/logout | auth/api `logoutCurrentUser` | access 请求头、refresh Cookie，无 JSON | `SecurityConfig` + `LogoutSuccessHandlerImpl` | 一致 |
| PUT /auth/info | auth/api `updateProfile` | JSON `version,nickname,sex,email?,phone?,remark?` | `ProfileUpdateRequest` | 一致 |
| POST /system/menu | system/menu/api `createMenu` | JSON `parentId,menuName,orderNum,path,component,queryParam,isFrame,isCache,menuType,visible,perms,icon,status,remark` | `MenuCreateRequest` | 一致 |
| PUT /system/menu | system/menu/api `updateMenu` | 菜单创建字段 + `id,version` | `MenuUpdateRequest` | 修复 `menuId → id`，补 `version` |
| DELETE /system/menu/{menuId} | system/menu/api `deleteMenu` | 路径 `menuId` | `SysMenuController.remove(Long menuId)` | 一致 |
| POST /system/dept | system/dept/api `createDept` | JSON `parentId,deptName,orderNum,leader,phone,email,status` | `DeptCreateRequest` | 一致 |
| PUT /system/dept | system/dept/api `updateDept` | 部门创建字段 + `id,version`，`phone,email` 可省略 | `DeptUpdateRequest` | 补 `version`，只写新联系方式 |
| DELETE /system/dept/{deptId} | system/dept/api `deleteDept` | 路径 `deptId` | `SysDeptController.remove(Long deptId)` | 一致 |
| POST /system/role | system/role/api `createRole` | JSON `roleName,roleKey,orderNum,dataScope,status,remark,menuIds,deptIds` | `RoleCreateRequest` | 一致 |
| PUT /system/role | system/role/api `updateRole` | 角色创建字段 + `id,version` | `RoleUpdateRequest` | 补 `version` |
| PUT /system/role/changeStatus | system/role/api `changeRoleStatus` | 查询参数 `roleId,status` | `SysRoleController.changeStatus` | 一致 |
| PUT /system/role/dataScope | system/role/api `updateRoleDataScope` | 查询参数 `roleId,dataScope,deptIds?` | `SysRoleController.dataScope` | 一致 |
| DELETE /system/role/{roleIds} | system/role/api `deleteRoles` | 路径 `roleIds` | `SysRoleController.remove(List<Long> roleIds)` | 一致 |
| POST /system/user | system/user/api `createUser` | JSON `username,password,nickname,phone,email,sex,deptId,status,roleIds,remark` | `UserCreateRequest` | 一致 |
| PUT /system/user | system/user/api `updateUser` | 用户创建字段去掉 `password`，加 `id,version` | `UserUpdateRequest` | 字段一致，修复编辑器和 DTO 映射的改名链路 |
| PUT /system/user/changeStatus | system/user/api `changeUserStatus` | JSON `userId,version,status` | `UserStatusUpdateRequest` | 一致 |
| PUT /system/user/resetPwd | system/user/api `resetUserPassword` | JSON `userId,version,password` | `ResetPasswordRequest` | 一致 |
| DELETE /system/user/{userIds} | system/user/api `deleteUsers` | 路径 `userIds` | `SysUserController.remove(List<Long> userIds)` | 一致 |
| POST /system/user/import | system/user/transfer-api `importUsers` | multipart `file,updateSupport` | `SysUserController.importData` | 一致 |
| POST /system/user/export | system/user/transfer-api `exportUsers` | 查询参数 `username?,nickname?,email?,phone?,sex?,status?` | `UserQuery` | 一致，导出使用全部匹配行 |
| POST /system/dict/type | system/dict/api `createDictType` | JSON `dictName,dictType,business,status,remark` | `DictTypeCreateRequest` | 一致 |
| PUT /system/dict/type | system/dict/api `updateDictType` | 字典类型创建字段 + `id,version` | `DictTypeUpdateRequest` | 一致 |
| DELETE /system/dict/type/{dictIds} | system/dict/api `deleteDictTypes` | 路径 `dictIds` | `SysDictController.removeType(List<Long> dictIds)` | 一致 |
| DELETE /system/dict/type/refreshCache | system/dict/api `refreshDictCache` | 无请求体 | `SysDictController.refreshCache` | 一致 |
| POST /system/dict/data | system/dict/api `createDictData` | JSON `dictType,dictLabel,dictValue,dictSort,cssClass,listClass,isDefault,status,remark` | `DictDataCreateRequest` | 一致 |
| PUT /system/dict/data | system/dict/api `updateDictData` | 字典数据创建字段 + `id,version` | `DictDataUpdateRequest` | 一致 |
| DELETE /system/dict/data/{dictCodes} | system/dict/api `deleteDictData` | 路径 `dictCodes` | `SysDictController.removeData(List<Long> dictCodes)` | 一致 |
| POST /system/file/upload | system/file/api `uploadFile` | multipart `file,bizType?,bizId?,path?` | `FileController.upload` | 一致 |
| DELETE /system/file/{ids} | system/file/api `deleteFiles` | 路径 `ids` | `FileController.deleteByIds(List<Long> ids)` | 一致 |
| DELETE /monitor/online/{tokenId} | monitor/online/api `forceLogoutByTokenId` | 路径 `tokenId` | `MonitorController.forceLogout(String tokenId)` | 一致 |
| DELETE /monitor/login/log/{infoIds} | monitor/login-log/api `deleteLoginLogs` | 路径 `infoIds` | `SysLoginLogController.remove(List<Long> infoIds)` | 一致 |
| DELETE /monitor/login/log/clean | monitor/login-log/api `cleanLoginLog` | 无请求体 | `SysLoginLogController.clean` | 一致，显示条件由 /auth/info 提供 |
| DELETE /monitor/operlog/{operIds} | monitor/oper-log/api `deleteOperLogs` | 路径 `operIds` | `SysOperLogController.remove(List<Long> operIds)` | 一致 |
| DELETE /monitor/operlog/clean | monitor/oper-log/api `cleanOperLog` | 无请求体 | `SysOperLogController.clean` | 一致，显示条件由 /auth/info 提供 |

## 修复处置

| ID | 处置 | 改动文件 | 回归测试 |
|---|---|---|---|
| R01 | 已修。登录与刷新返回稳定会话 ID。跨标签换号取消旧请求并清私有缓存，旧页登出保留不同会话的 Cookie。 | [AuthController.java:109](../../../quantum-biz-system/src/main/java/com/alpha/system/controller/AuthController.java#L109)、[TokenService.java:190](../../../quantum-common/quantum-common-security/src/main/java/com/alpha/security/token/TokenService.java#L190)、[lib/http.ts:145](../../../../quantum-front-wt-upgrade/src/lib/http.ts#L145)、[lib/auth-session-channel.ts:49](../../../../quantum-front-wt-upgrade/src/lib/auth-session-channel.ts#L49) | 后端 `RefreshContractTest.loginAndRefreshReturnTheSameSessionIdentityAndNewLoginChangesIt`、`logoutFromAnOldPagePreservesTheDifferentSessionsCookie`。前端 `a write cannot replay with another server session and its private cache is cleared`、`an in-flight write aborts at the adapter with a caller signal present=%s`。 |
| R02 | 已修。原子轮换 refresh。旧轮换返回 409 并保留 Cookie。前端跨页加锁并复用同会话结果。 | [AuthController.java:91](../../../quantum-biz-system/src/main/java/com/alpha/system/controller/AuthController.java#L91)、[TokenService.java:143](../../../quantum-common/quantum-common-security/src/main/java/com/alpha/security/token/TokenService.java#L143)、[RedisUtil.java:67](../../../quantum-common/quantum-common-cache/src/main/java/com/alpha/cache/util/RedisUtil.java#L67)、[LocalCacheClient.java:84](../../../quantum-common/quantum-common-cache/src/main/java/com/alpha/cache/util/LocalCacheClient.java#L84)、[lib/auth-session-channel.ts:81](../../../../quantum-front-wt-upgrade/src/lib/auth-session-channel.ts#L81)、[lib/http.ts:120](../../../../quantum-front-wt-upgrade/src/lib/http.ts#L120) | 后端 `RefreshContractTest.obsoleteRefreshCannotOverwriteRotatedCookieInEitherResponseOrder`、`expiredSupersededRefreshLeavesWinningCookieIntact`。前端 `startup waits for a sibling cookie rotation before submitting its refresh`、`startup consumes a fresh sibling result without reusing its consumed cookie`、`a sibling rotation is retryable when the obsolete response arrives first=%s`。 |
| R03 | 已修。强退和改密撤销同一用户的 Web、MCP 会话代际。签发和验证核对代际，吊销前开始的刷新无法留下有效会话。 | [SessionGeneration.java:30](../../../quantum-common/quantum-common-security/src/main/java/com/alpha/security/token/SessionGeneration.java#L30)、[TokenService.java:232](../../../quantum-common/quantum-common-security/src/main/java/com/alpha/security/token/TokenService.java#L232)、[LoginServiceImpl.java:87](../../../quantum-biz-system/src/main/java/com/alpha/system/service/impl/LoginServiceImpl.java#L87)、[UserSessionInvalidationService.java:34](../../../quantum-biz-system/src/main/java/com/alpha/system/security/UserSessionInvalidationService.java#L34)、[OAuthTokenService.java:148](../../../quantum-mcp/src/main/java/com/alpha/mcp/oauth/OAuthTokenService.java#L148) | `TokenServiceTest.forceLogoutDuringRegistrationRejectsAnyRefreshFromTheOldGeneration`、`OAuthTokenServiceTest.forceLogoutInvalidatesExistingAccessRefreshAndAuthorizationCodes`。 |
| R04 | 已修。本地会话代际与通用缓存淘汰分离，用户令牌索引丢失后仍可吊销。 | [LocalCacheClient.java:52](../../../quantum-common/quantum-common-cache/src/main/java/com/alpha/cache/util/LocalCacheClient.java#L52)、[TokenService.java:232](../../../quantum-common/quantum-common-security/src/main/java/com/alpha/security/token/TokenService.java#L232) | `TokenServiceTest.revokedEvenWhenUserIndexWasEvicted`。 |
| R05 | 已修。按用户强退通过统一事件撤销 Web 和 MCP。按会话强退只影响目标 Web 会话。 | [SysOnlineServiceImpl.java:46](../../../quantum-biz-system/src/main/java/com/alpha/system/service/impl/SysOnlineServiceImpl.java#L46)、[UserSessionInvalidationService.java:34](../../../quantum-biz-system/src/main/java/com/alpha/system/security/UserSessionInvalidationService.java#L34)、[OAuthTokenService.java:148](../../../quantum-mcp/src/main/java/com/alpha/mcp/oauth/OAuthTokenService.java#L148) | `OAuthTokenServiceTest.monitorForceLogoutRevokesWebAndMcpSessionsButSingleSessionActionIsWebOnly`。 |
| R06 | 已修。明确回滚时才删除上传对象。UNKNOWN 保留对象并记录对账所需路径和业务标识。 | [SysFileServiceImpl.java:47](../../../quantum-biz-system/src/main/java/com/alpha/system/service/impl/SysFileServiceImpl.java#L47)、[SysFileServiceImpl.java:49](../../../quantum-biz-system/src/main/java/com/alpha/system/service/impl/SysFileServiceImpl.java#L49) | `SysFileServiceImplTest.keepsUploadedObjectWhenCommitOutcomeIsUnknown`、`compensatesOnlyAnExplicitUploadRollback`。 |
| R07 | 已修。菜单、角色、部门从详情冻结 id/version，菜单以 id 提交。三个后端入口均以 409 表达乐观锁冲突。 | [SysMenuServiceImpl.java:189](../../../quantum-biz-system/src/main/java/com/alpha/system/service/impl/SysMenuServiceImpl.java#L189)、[SysRoleServiceImpl.java:158](../../../quantum-biz-system/src/main/java/com/alpha/system/service/impl/SysRoleServiceImpl.java#L158)、[SysDeptServiceImpl.java:145](../../../quantum-biz-system/src/main/java/com/alpha/system/service/impl/SysDeptServiceImpl.java#L145)、[features/system/menu/model.ts:96](../../../../quantum-front-wt-upgrade/src/features/system/menu/model.ts#L96)、[features/system/role/components/roles-action-dialog.tsx:93](../../../../quantum-front-wt-upgrade/src/features/system/role/components/roles-action-dialog.tsx#L93)、[features/system/dept/model.ts:102](../../../../quantum-front-wt-upgrade/src/features/system/dept/model.ts#L102) | 后端 `VersionedSystemEditsTest.menuEditAcceptsStringIdAndVersionAndRejectsStaleSave`、`roleEditAcceptsStringIdAndVersionAndRejectsStaleSave`、`departmentEditAcceptsStringIdAndVersionAndRejectsStaleSave`。前端 `menu edit sends the detail id and version and surfaces stale-version conflicts`、`role edit sends the version from the selected role and surfaces stale-version conflicts`、`department edit sends the detail version and omits untouched masked contacts`。 |
| R08 | 已修。字典缓存失效 topic 发布、订阅使用相同的定型 codec。 | [MultiLevelCacheConfig.java:41](../../../quantum-common/quantum-common-cache/src/main/java/com/alpha/cache/config/MultiLevelCacheConfig.java#L41)、[MultiLevelCacheConfig.java:69](../../../quantum-common/quantum-common-cache/src/main/java/com/alpha/cache/config/MultiLevelCacheConfig.java#L69)、[CacheModeConfigurationTest.java:35](../../../quantum-common/quantum-common-cache/src/test/java/com/alpha/cache/config/CacheModeConfigurationTest.java#L35) | `MultiLevelCacheSyncTest.transfersDictionaryEvictionAndClearBetweenCachesThroughTheTopicCodec`、`CacheModeConfigurationTest.redisModeCanBuildItsCacheWithTheStarterConfiguration`。 |
| R09 | 已修。表单允许修改用户名，后端执行原有唯一性校验并撤销旧会话。超管批量保护绑定用户 ID。 | [UserConvert.java:57](../../../quantum-biz-system/src/main/java/com/alpha/system/convert/UserConvert.java#L57)、[features/system/user/components/users-action-dialog.tsx:302](../../../../quantum-front-wt-upgrade/src/features/system/user/components/users-action-dialog.tsx#L302)、[features/system/user/user-guards.ts:5](../../../../quantum-front-wt-upgrade/src/features/system/user/user-guards.ts#L5) | 后端 `PasswordMigrationTest.userEditRenamesTheAuthenticatedAccountAndRevokesTheOldSessions`。前端 `renders an editable username and sends the replacement in the user update`、`a rename keeps the super administrator out of bulk user changes`。 |
| R10 | 已修。清日志按钮使用 /auth/info 返回的 canClearAuditLogs，与清表鉴权共用超管判定。 | [AuthController.java:124](../../../quantum-biz-system/src/main/java/com/alpha/system/controller/AuthController.java#L124)、[app/auth/api.ts:22](../../../../quantum-front-wt-upgrade/src/app/auth/api.ts#L22)、[features/monitor/log-clean.ts:6](../../../../quantum-front-wt-upgrade/src/features/monitor/log-clean.ts#L6) | 后端 `AuditLogClearAuthorizationTest.authInfoAdvertisesOnlyTheAuthorityUsedByTheAuditControllers`、`onlySuperAdministratorCanClearEitherAuditTable`。前端 `both clean actions use server authority even when role and permissions disagree`。 |
| R11 | 已修。Excel 导入保留物理行号，合并格式和校验错误，以逐行结果返回并保持整批回滚。 | [ExcelUtil.java:27](../../../quantum-common/quantum-common-file/src/main/java/com/alpha/file/util/ExcelUtil.java#L27)、[SysUserController.java:149](../../../quantum-biz-system/src/main/java/com/alpha/system/controller/SysUserController.java#L149)、[SysUserController.java:184](../../../quantum-biz-system/src/main/java/com/alpha/system/controller/SysUserController.java#L184)、[SysUserServiceImpl.java:318](../../../quantum-biz-system/src/main/java/com/alpha/system/service/impl/SysUserServiceImpl.java#L318) | `UserImportRowContractTest.importReportsConversionAndValidationErrorsAtPhysicalWorksheetRows`。 |
| R12 | 已修。确认凭证失效返回 401 并清会话。依赖故障返回 503，前端保留会话并允许重试。 | [AuthController.java:95](../../../quantum-biz-system/src/main/java/com/alpha/system/controller/AuthController.java#L95)、[TokenService.java:131](../../../quantum-common/quantum-common-security/src/main/java/com/alpha/security/token/TokenService.java#L131)、[lib/http.ts:259](../../../../quantum-front-wt-upgrade/src/lib/http.ts#L259)、[app/auth/session.ts:42](../../../../quantum-front-wt-upgrade/src/app/auth/session.ts#L42) | 后端 `RefreshContractTest.userStoreFailurePreservesCookieAndRefreshCanBeRetried`、`TokenServiceTest.temporaryRegistrationFailureLeavesPreviousRefreshUsable`。前端 `a temporary refresh failure %s preserves the session and allows another request to retry`、`startup retries a temporary refresh error in the same session generation`、`a temporary failure refreshing for logout preserves the session for retry`。 |
| R13 | 已修。登录审计独立有界排队，队列饱和时告警并同步保留审计记录。 | [AsyncConfig.java:41](../../../quantum-common/quantum-common-logging/src/main/java/com/alpha/logging/config/AsyncConfig.java#L41)、[SysLoginLogServiceImpl.java:40](../../../quantum-biz-system/src/main/java/com/alpha/system/service/impl/SysLoginLogServiceImpl.java#L40) | `LoginAuditSaturationTest.saturatedAuditQueuePreservesSuccessfulLoginAndItsAuditRecord`。 |
| R14 | 已修。自动恢复用过后，手动重试重新加载文档并重新执行懒加载。 | [features/errors/general-error/index.tsx:37](../../../../quantum-front-wt-upgrade/src/features/errors/general-error/index.tsx#L37)、[lib/preload-recovery.ts:20](../../../../quantum-front-wt-upgrade/src/lib/preload-recovery.ts#L20) | `manual retry reloads after the automatic recovery was already used`。 |
| R15 | 已修。按路径删除全部匹配元数据，提交后清理一次物理对象。 | [SysFileServiceImpl.java:128](../../../quantum-biz-system/src/main/java/com/alpha/system/service/impl/SysFileServiceImpl.java#L128) | `SysFileServiceImplTest.deletesAllMetadataForADuplicatePathAndCleansTheObjectOnceAfterCommit`。 |
| R16 | 已修。改父节点锁定并检查祖先链，拒绝子孙作为父节点。递归 SQL 去重，处理历史环。 | [SysMenuServiceImpl.java:388](../../../quantum-biz-system/src/main/java/com/alpha/system/service/impl/SysMenuServiceImpl.java#L388)、[SysMenuMapper.xml:16](../../../quantum-biz-system/src/main/resources/mapper/SysMenuMapper.xml#L16) | `SysMenuHierarchyTest.cannotReparentMenuBelowItsGrandchild`、`locksEachAncestorBeforeReparenting`、`canRepairHistoricalCycleByMovingAMenuToRoot`、`SysMenuMapperContractTest.recursionDeduplicatesAlreadyVisitedMenuRows`。 |
| R17 | 已修。列表和导出识别显式系统数据域，普通缺身份调用继续拒绝。 | [SysUserServiceImpl.java:599](../../../quantum-biz-system/src/main/java/com/alpha/system/service/impl/SysUserServiceImpl.java#L599) | `SysUserServiceImplSecurityTest.systemDataScopeKeepsListAndExportOpenOnlyInsideTheExplicitScope`。 |
| R18 | 已修。升级报告勘误，明确由响应体读取 accessToken。 | [.ai_state/sprints/2026-11-front-upgrade-audit/upgrade-report.md:87](../../../../quantum-front-wt-upgrade/.ai_state/sprints/2026-11-front-upgrade-audit/upgrade-report.md#L87) | `a refresh response without a body token fails even when a header is present`。 |
| R19 | 已修。批量删除按不同字典类型一次校验，任一类型不可见时整批拒绝。 | [SysDictDataServiceImpl.java:95](../../../quantum-biz-system/src/main/java/com/alpha/system/service/impl/SysDictDataServiceImpl.java#L95) | `SysDictDataVisibilityTest.batchDeleteChecksDistinctTypesTogetherBeforeDeleting`、`batchDeleteDeniesAllRowsWhenAnyTypeIsInvisible`。 |
| R20 | 已修。部门联系方式掩码仅作提示。编辑只提交新填值，后端保留省略字段和旧客户端回送的当前邮箱掩码。 | [SysDeptServiceImpl.java:133](../../../quantum-biz-system/src/main/java/com/alpha/system/service/impl/SysDeptServiceImpl.java#L133)、[features/system/dept/model.ts:102](../../../../quantum-front-wt-upgrade/src/features/system/dept/model.ts#L102)、[features/system/dept/index.tsx:634](../../../../quantum-front-wt-upgrade/src/features/system/dept/index.tsx#L634) | 后端 `VersionedSystemEditsTest.departmentEditPreservesOmittedContactsAndIgnoresDisplayedEmailMask`。前端 `department edit sends the detail version and omits untouched masked contacts`、`department edit only submits newly entered contact values`。 |

### 检查结果

| Worktree | 命令 | 退出码 | 结果 |
|---|---|---:|---|
| 后端 | `mvn -Dmaven.repo.local=/Users/mi_manchi/workspace/Tools/apache-maven-3.9.14/repository clean test` | 0 | Maven Reactor 11/11 成功，49 份 Surefire XML 共 164 项，失败、错误、跳过均为 0 |
| 前端 | `bun --no-env-file run build` | 0 | TypeScript 与 Vite 生产构建通过 |
| 前端 | `bun --no-env-file test` | 0 | 25 个文件，105 项测试，323 次断言，0 失败 |
| 前端 | `bun --no-env-file run lint` | 0 | ESLint 通过 |
| 前端 | `bun --no-env-file run knip` | 0 | Knip 通过 |

行为改动用改前失败的用例复核。R18 为文档勘误，沿用既有响应体回归。

构建和 Knip 在排除环境文件的前端隔离副本运行。Athena 的证据快照在共享 Git 对象库写入处被沙箱拒绝，退出码来自直接运行 Maven 和 Bun。Playwright 的 Chromium 启动在 macOS Mach Port 注册处被拒绝，双标签用模拟的 Web Locks、BroadcastChannel 和共享 Cookie 验证。Redis 同步使用生产 codec 和模拟转发，真实 Redis 和 PostgreSQL 待验证。

切换到新会话代际后，既有 Web 会话需重新登录，既有 MCP 会话需重新授权。登录审计队列饱和时，同步保留审计记录会增加一次数据库往返。

### 改动文件清单

路径相对各自 worktree，范围为本轮修复写入的文件。

#### 后端

```text
.ai_state/sprints/2026-11-deps-upgrade-audit/astra-final-review.md
.ai_state/sprints/2026-11-deps-upgrade-audit/design.md
quantum-biz-system/src/main/java/com/alpha/system/controller/AuthController.java
quantum-biz-system/src/main/java/com/alpha/system/controller/SysUserController.java
quantum-biz-system/src/main/java/com/alpha/system/convert/UserConvert.java
quantum-biz-system/src/main/java/com/alpha/system/dto/response/LoginResponse.java
quantum-biz-system/src/main/java/com/alpha/system/dto/response/UserVO.java
quantum-biz-system/src/main/java/com/alpha/system/mapper/SysUserMapper.java
quantum-biz-system/src/main/java/com/alpha/system/security/UserSessionInvalidationService.java
quantum-biz-system/src/main/java/com/alpha/system/service/ISysUserService.java
quantum-biz-system/src/main/java/com/alpha/system/service/impl/LoginServiceImpl.java
quantum-biz-system/src/main/java/com/alpha/system/service/impl/SysDeptServiceImpl.java
quantum-biz-system/src/main/java/com/alpha/system/service/impl/SysDictDataServiceImpl.java
quantum-biz-system/src/main/java/com/alpha/system/service/impl/SysFileServiceImpl.java
quantum-biz-system/src/main/java/com/alpha/system/service/impl/SysLoginLogServiceImpl.java
quantum-biz-system/src/main/java/com/alpha/system/service/impl/SysMenuServiceImpl.java
quantum-biz-system/src/main/java/com/alpha/system/service/impl/SysOnlineServiceImpl.java
quantum-biz-system/src/main/java/com/alpha/system/service/impl/SysRoleServiceImpl.java
quantum-biz-system/src/main/java/com/alpha/system/service/impl/SysUserServiceImpl.java
quantum-biz-system/src/main/resources/mapper/SysMenuMapper.xml
quantum-biz-system/src/test/java/com/alpha/system/controller/AuditLogClearAuthorizationTest.java
quantum-biz-system/src/test/java/com/alpha/system/controller/PasswordMigrationTest.java
quantum-biz-system/src/test/java/com/alpha/system/controller/RefreshContractTest.java
quantum-biz-system/src/test/java/com/alpha/system/controller/UserImportRowContractTest.java
quantum-biz-system/src/test/java/com/alpha/system/controller/VersionedSystemEditsTest.java
quantum-biz-system/src/test/java/com/alpha/system/mapper/SysMenuMapperContractTest.java
quantum-biz-system/src/test/java/com/alpha/system/service/impl/LoginAuditSaturationTest.java
quantum-biz-system/src/test/java/com/alpha/system/service/impl/LoginServiceImplTest.java
quantum-biz-system/src/test/java/com/alpha/system/service/impl/SysDictDataVisibilityTest.java
quantum-biz-system/src/test/java/com/alpha/system/service/impl/SysFileServiceImplTest.java
quantum-biz-system/src/test/java/com/alpha/system/service/impl/SysMenuHierarchyTest.java
quantum-biz-system/src/test/java/com/alpha/system/service/impl/SysUserServiceImplSecurityTest.java
quantum-common/quantum-common-cache/src/main/java/com/alpha/cache/config/MultiLevelCacheConfig.java
quantum-common/quantum-common-cache/src/main/java/com/alpha/cache/constant/CacheKeyConstant.java
quantum-common/quantum-common-cache/src/main/java/com/alpha/cache/util/CacheClient.java
quantum-common/quantum-common-cache/src/main/java/com/alpha/cache/util/LocalCacheClient.java
quantum-common/quantum-common-cache/src/main/java/com/alpha/cache/util/RedisUtil.java
quantum-common/quantum-common-cache/src/test/java/com/alpha/cache/config/CacheModeConfigurationTest.java
quantum-common/quantum-common-cache/src/test/java/com/alpha/cache/config/MultiLevelCacheSyncTest.java
quantum-common/quantum-common-file/src/main/java/com/alpha/file/util/ExcelUtil.java
quantum-common/quantum-common-framework/src/main/java/com/alpha/framework/entity/LoginUser.java
quantum-common/quantum-common-logging/src/main/java/com/alpha/logging/config/AsyncConfig.java
quantum-common/quantum-common-security/src/main/java/com/alpha/security/handler/LogoutSuccessHandlerImpl.java
quantum-common/quantum-common-security/src/main/java/com/alpha/security/token/SessionGeneration.java
quantum-common/quantum-common-security/src/main/java/com/alpha/security/token/TokenInfo.java
quantum-common/quantum-common-security/src/main/java/com/alpha/security/token/TokenService.java
quantum-common/quantum-common-security/src/test/java/com/alpha/security/token/TokenServiceTest.java
quantum-mcp/src/main/java/com/alpha/mcp/oauth/OAuthTokenService.java
quantum-mcp/src/test/java/com/alpha/mcp/oauth/OAuthTokenServiceTest.java
quantum-mcp/src/test/java/com/alpha/mcp/security/OAuthBearerAuthenticationFilterTest.java
```

#### 前端

```text
.ai_state/sprints/2026-11-front-upgrade-audit/design.md
.ai_state/sprints/2026-11-front-upgrade-audit/upgrade-report.md
src/app/auth/access.ts
src/app/auth/api.test.ts
src/app/auth/api.ts
src/app/auth/session.test.ts
src/app/auth/session.ts
src/features/errors/general-error/index.tsx
src/features/monitor/log-clean.test.ts
src/features/monitor/log-clean.ts
src/features/monitor/login-log/index.tsx
src/features/monitor/oper-log/index.tsx
src/features/system/dept/api.ts
src/features/system/dept/form.ts
src/features/system/dept/index.tsx
src/features/system/dept/model.test.ts
src/features/system/dept/model.ts
src/features/system/menu/api.ts
src/features/system/menu/form.ts
src/features/system/menu/index.tsx
src/features/system/menu/model.ts
src/features/system/role/api.ts
src/features/system/role/components/roles-action-dialog-form.ts
src/features/system/role/components/roles-action-dialog.tsx
src/features/system/role/model.ts
src/features/system/user/components/user-username-field.test.tsx
src/features/system/user/components/user-username-field.tsx
src/features/system/user/components/users-action-dialog.tsx
src/features/system/user/user-guards.test.ts
src/features/system/user/user-guards.ts
src/features/system/versioned-edits.test.ts
src/lib/auth-session-channel.ts
src/lib/http.test.ts
src/lib/http.ts
src/lib/preload-recovery.test.ts
src/lib/preload-recovery.ts
src/lib/query-keys.ts
src/main.tsx
src/routes/_authenticated/route.tsx
src/stores/auth-store.ts
src/types/auth.ts
```

## 独立 review 修复

| ID | 修复 | 改动文件 | 回归测试 |
|---|---|---|---|
| B1 | OAuth 刷新先校验绑定及当前用户，再原子取走旧令牌。用户库临时失败后可用同一令牌重试，同客户端其他授权仍有效。 | [OAuthTokenService.java:93](../../../quantum-mcp/src/main/java/com/alpha/mcp/oauth/OAuthTokenService.java#L93)、[OAuthTokenServiceTest.java:131](../../../quantum-mcp/src/test/java/com/alpha/mcp/oauth/OAuthTokenServiceTest.java#L131) | `temporaryUserLookupFailureLeavesRefreshUsableWithoutRevokingOtherGrants`、`refreshTokenRotatesAndReuseRevokesClientTokenChain` |
| B2 | 元数据提交后，用独立只读事务按路径查询有效引用。确认无引用才清理存储对象。 | [SysFileServiceImpl.java:113](../../../quantum-biz-system/src/main/java/com/alpha/system/service/impl/SysFileServiceImpl.java#L113)、[SysFileServiceImplTest.java:69](../../../quantum-biz-system/src/test/java/com/alpha/system/service/impl/SysFileServiceImplTest.java#L69) | `survivingMetadataCanDownloadAfterDeletingAnotherRowForTheSamePath`、`deletesAllMetadataForADuplicatePathAndCleansTheObjectOnceAfterCommit` |
| B3 | 代际竞态返回 HTTP 409「会话切换中，请重试」。令牌签发成功后才清失败计数、更新最近登录记录并记录成功审计。 | [LoginServiceImpl.java:109](../../../quantum-biz-system/src/main/java/com/alpha/system/service/impl/LoginServiceImpl.java#L109)、[TokenService.java:80](../../../quantum-common/quantum-common-security/src/main/java/com/alpha/security/token/TokenService.java#L80)、[ResultCode.java:54](../../../quantum-common/quantum-common-framework/src/main/java/com/alpha/framework/enums/ResultCode.java#L54)、[LoginServiceImplTest.java:64](../../../quantum-biz-system/src/test/java/com/alpha/system/service/impl/LoginServiceImplTest.java#L64) | `missingGenerationAfterValidCredentialsDoesNotCountTowardAccountLockout`、`rejectedTokenIssuanceDoesNotClearFailureCountOrRecordSuccessfulLogin` |
| B4 | Redis 类型白名单允许 `com.alpha.`、`java.util.`、`java.time.`、`java.lang.` 及这些包下的对象数组和基本类型数组。 | [RedisConfig.java:83](../../../quantum-common/quantum-common-cache/src/main/java/com/alpha/cache/config/RedisConfig.java#L83)、[RedisCodecRoundTripTest.java:48](../../../quantum-mcp/src/test/java/com/alpha/mcp/controller/RedisCodecRoundTripTest.java#L48) | `preservesWebAndMcpCacheEntries`、`rejectsNetworkTypeIds`、`rejectsNetworkArrayTypeIds`、`acceptsPrimitiveArrayTypeIds` |
| F1 | 关闭的会话触发受保护路由跳转 `/sign-in`，携带净化后的 `redirect`，零刷新请求。 | [session.ts:30](../../../../quantum-front-wt-upgrade/src/app/auth/session.ts#L30)、[http.ts:79](../../../../quantum-front-wt-upgrade/src/lib/http.ts#L79)、[route.tsx:38](../../../../quantum-front-wt-upgrade/src/routes/_authenticated/route.tsx#L38)、[session.test.ts:136](../../../../quantum-front-wt-upgrade/src/app/auth/session.test.ts#L136) | `a protected route redirects to sign-in after logout without refreshing` |
| F2 | 刷新遇到 409 时再次读取同会话的其他标签页结果。没有结果时重试一次，第二次失败交给原失败处理。 | [http.ts:125](../../../../quantum-front-wt-upgrade/src/lib/http.ts#L125)、[http.test.ts:175](../../../../quantum-front-wt-upgrade/src/lib/http.test.ts#L175)、[session.test.ts:456](../../../../quantum-front-wt-upgrade/src/app/auth/session.test.ts#L456) | `a refresh conflict retries once when the obsolete response arrives first=%s`、`startup consumes a fresh sibling result with refresh conflict=%s` |

六条回归均在修复前复现失败，修复后通过。

| 工作区 | 命令 | 退出码 | 结果 |
|---|---|---:|---|
| 后端 | `mvn clean test` | 0 | 11 个 Reactor 模块通过，171 项测试 |
| 前端 | `bun --no-env-file run build` | 0 | TypeScript 与 Vite 生产构建通过 |
| 前端 | `bun --no-env-file test` | 0 | 25 个文件，107 项测试 |
| 前端 | `bun --no-env-file run lint` | 0 | ESLint 通过 |
| 前端 | `bun --no-env-file run knip` | 0 | Knip 通过 |

前端构建和 Knip 在工作区内排除 `.env*` 的临时副本运行。Athena 最终测试快照写入共享 Git 对象库时被沙箱拒绝，两个测试退出码来自直接运行 Maven 和 Bun。
