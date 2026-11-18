package com.alpha.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alpha.file.util.ExcelUtil.ReadRow;
import com.alpha.framework.constant.CommonConstants;
import com.alpha.framework.context.UserContext;
import com.alpha.framework.entity.LoginUser;
import com.alpha.framework.enums.ResultCode;
import com.alpha.framework.exception.BizException;
import com.alpha.orm.enums.DataScopeType;
import com.alpha.orm.permission.DataScope;
import com.alpha.orm.permission.SystemDataScopeContext;
import com.alpha.security.config.SecurityProperties;
import com.alpha.security.util.BcryptPasswords;
import com.alpha.system.domain.SysDept;
import com.alpha.system.domain.SysUser;
import com.alpha.system.dto.request.UserImportRequest;
import com.alpha.system.dto.response.ImportRowResult;
import com.alpha.system.security.UserImportException;
import com.alpha.system.dto.request.UserQuery;
import com.alpha.system.mapper.SysDeptMapper;
import com.alpha.system.mapper.SysUserMapper;
import com.alpha.system.mapper.SysUserRoleMapper;
import com.alpha.system.security.ForceLogoutEvent;
import com.alpha.system.security.RoleDelegationPolicy;
import com.alpha.system.security.UserCacheRefreshEvent;
import com.alpha.system.service.ISysUserService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.alpha.system.domain.table.SysDeptTableDef.SYS_DEPT;
import static com.alpha.system.domain.table.SysUserTableDef.SYS_USER;

/**
 * 用户服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements ISysUserService {

    private final SysUserMapper userMapper;
    private final SysDeptMapper deptMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final SecurityProperties securityProperties;
    private final ApplicationEventPublisher eventPublisher;
    private final Validator validator;
    private final RoleDelegationPolicy roleDelegation;

    @Override
    @DataScope(type = DataScopeType.DEFAULT, userField = "id")
    public Page<SysUser> selectUserPage(UserQuery query) {
        QueryWrapper wrapper = buildQueryWrapper(query);
        restrictUserVisibility(wrapper);
        Page<SysUser> pageResult =
                page(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        fillDeptNames(pageResult.getRecords());
        return pageResult;
    }

    @Override
    @DataScope(type = DataScopeType.DEFAULT, userField = "id")
    public List<SysUser> selectUserList(UserQuery query) {
        QueryWrapper wrapper = buildQueryWrapper(query);
        restrictUserVisibility(wrapper);
        List<SysUser> users = list(wrapper.limit(10_001));
        if (users.size() > 10_000) {
            throw new BizException("导出最多 10000 行，请缩小查询范围");
        }
        fillDeptNames(users);
        return users;
    }

    @Override
    public SysUser selectByUsername(String username) {
        return userMapper.selectByUsername(username);
    }

    @Override
    public SysUser selectUserById(Long userId) {
        SysUser user = getById(userId);
        if (user != null) {
            assertTargetUserReadable(user);
        }
        return user;
    }

    @Override
    public String selectDeptNameById(Long deptId) {
        if (deptId == null) {
            return null;
        }
        return deptMapper.selectDeptNameById(deptId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long insertUser(SysUser user, List<Long> roleIds) {
        // 校验用户名
        if (!checkUsernameUnique(user.getUsername())) {
            throw new BizException("用户名已存在");
        }
        // 校验手机号
        if (StrUtil.isNotBlank(user.getPhone()) && !checkPhoneUnique(user.getPhone(), null)) {
            throw new BizException("手机号已存在");
        }
        // 校验邮箱
        if (StrUtil.isNotBlank(user.getEmail()) && !checkEmailUnique(user.getEmail(), null)) {
            throw new BizException("邮箱已存在");
        }

        return saveNewUser(user, roleIds);
    }

    private Long saveNewUser(SysUser user, List<Long> roleIds) {
        validatePassword(user.getPassword());

        // 数据权限：非管理员只能在自己可见的部门下创建用户
        assertDeptInDataScope(user.getDeptId());
        roleDelegation.checkAssignment(roleIds, user.getDeptId());

        // 加密密码
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // 保存用户
        save(user);

        // 保存用户角色关联
        if (CollUtil.isNotEmpty(roleIds)) {
            userRoleMapper.batchInsert(user.getId(), roleIds);
        }

        return user.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateUser(SysUser user, List<Long> roleIds) {
        // 校验手机号
        if (StrUtil.isNotBlank(user.getPhone()) && !checkPhoneUnique(user.getPhone(), user.getId())) {
            throw new BizException("手机号已存在");
        }
        // 校验邮箱
        if (StrUtil.isNotBlank(user.getEmail()) && !checkEmailUnique(user.getEmail(), user.getId())) {
            throw new BizException("邮箱已存在");
        }

        SysUser oldUser = getById(user.getId());
        if (oldUser == null) {
            throw new BizException("用户不存在");
        }

        boolean usernameChanged = StrUtil.isNotBlank(user.getUsername()) && !user.getUsername().equals(oldUser.getUsername());

        // 数据权限：目标用户当前部门、以及调整后的部门都必须在操作者可见范围内
        assertTargetUserWritable(oldUser);
        if (usernameChanged) {
            if (BcryptPasswords.isLegacy(oldUser.getPassword())) {
                throw new BizException("请先登录升级密码或重置密码，再修改用户名");
            }
            if (!checkUsernameUnique(user.getUsername())) {
                throw new BizException("用户名已存在");
            }
        }
        if (user.getDeptId() != null && !user.getDeptId().equals(oldUser.getDeptId())) {
            assertDeptInDataScope(user.getDeptId());
        }

        if (roleIds != null) {
            Long deptId = user.getDeptId() == null ? oldUser.getDeptId() : user.getDeptId();
            roleDelegation.checkAssignment(roleIds, deptId);
        } else if (user.getDeptId() != null && !user.getDeptId().equals(oldUser.getDeptId())) {
            roleDelegation.checkAssignment(List.copyOf(userRoleMapper.selectRoleIdsByUserId(user.getId())), user.getDeptId());
        }

        // 更新用户
        boolean updated = updateById(user);
        if (!updated) {
            throw new BizException(ResultCode.DATA_CONFLICT, "用户信息已变更，请刷新后重试");
        }

        // 更新用户角色关联
        if (roleIds != null) {
            userRoleMapper.deleteByUserId(user.getId());
            if (CollUtil.isNotEmpty(roleIds)) {
                userRoleMapper.batchInsert(user.getId(), roleIds);
            }
        }

        if (usernameChanged || (CommonConstants.STATUS_DISABLE.equals(user.getStatus())
                && !CommonConstants.STATUS_DISABLE.equals(oldUser.getStatus()))) {
            eventPublisher.publishEvent(new ForceLogoutEvent(Set.of(user.getId())));
        } else {
            eventPublisher.publishEvent(new UserCacheRefreshEvent(Set.of(user.getId())));
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteUserByIds(List<Long> userIds) {
        // 不能删除管理员
        if (userIds.contains(CommonConstants.SUPER_ADMIN_ID)) {
            throw new BizException("不能删除超级管理员");
        }

        if (userIds.isEmpty()) {
            return false;
        }
        listByIds(userIds).forEach(this::assertTargetUserWritable);
        userRoleMapper.deleteByUserIds(userIds);

        // 逻辑删除用户
        boolean result = removeByIds(userIds);
        if (result) {
            eventPublisher.publishEvent(new ForceLogoutEvent(Set.copyOf(userIds)));
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean resetPassword(Long userId, Long version, String password) {
        validatePassword(password);
        SysUser existUser = getById(userId);
        if (existUser == null) {
            throw new BizException("用户不存在");
        }
        assertTargetUserWritable(existUser);
        SysUser user = new SysUser();
        user.setId(userId);
        user.setPassword(passwordEncoder.encode(password));
        user.setVersion(version);
        boolean result = updateById(user);
        if (!result) {
            throw new BizException(ResultCode.DATA_CONFLICT, "用户信息已变更，请刷新后重试");
        }
        eventPublisher.publishEvent(new ForceLogoutEvent(Set.of(userId)));
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateStatus(Long userId, Long version, Integer status) {
        if (!CommonConstants.STATUS_DISABLE.equals(status) && !CommonConstants.STATUS_NORMAL.equals(status)) {
            throw new BizException("状态值无效，仅支持0(禁用)和1(正常)");
        }
        // 不能禁用管理员
        if (CommonConstants.SUPER_ADMIN_ID.equals(userId) && CommonConstants.STATUS_DISABLE.equals(status)) {
            throw new BizException("不能禁用超级管理员");
        }

        SysUser existUser = getById(userId);
        if (existUser == null) {
            throw new BizException("用户不存在");
        }
        assertTargetUserWritable(existUser);

        SysUser user = new SysUser();
        user.setId(userId);
        user.setStatus(status);
        user.setVersion(version);
        boolean result = updateById(user);
        if (!result) {
            throw new BizException(ResultCode.DATA_CONFLICT, "用户状态已变更，请刷新后重试");
        }
        if (CommonConstants.STATUS_DISABLE.equals(status)) {
            eventPublisher.publishEvent(new ForceLogoutEvent(Set.of(userId)));
        } else {
            eventPublisher.publishEvent(new UserCacheRefreshEvent(Set.of(userId)));
        }
        return true;
    }

    @Override
    public boolean checkUsernameUnique(String username) {
        return userMapper.checkUsernameExists(username) == 0;
    }

    @Override
    public boolean checkPhoneUnique(String phone, Long excludeId) {
        return userMapper.checkPhoneExists(phone, excludeId) == 0;
    }

    @Override
    public boolean checkEmailUnique(String email, Long excludeId) {
        return userMapper.checkEmailExists(email, excludeId) == 0;
    }

    @Override
    public void updateLoginInfo(Long userId, LoginUser loginUser) {
        userMapper.updateLoginMetadata(userId, loginUser.getLoginIp(), loginUser.getLoginLocation(), LocalDateTime.now());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<ImportRowResult> importUsers(List<ReadRow<UserImportRequest>> rows, boolean updateSupport) {
        if (CollUtil.isEmpty(rows)) {
            throw new BizException("导入用户数据不能为空");
        }
        if (rows.size() > MAX_IMPORT_ROWS) {
            throw new BizException("导入最多 " + MAX_IMPORT_ROWS + " 行");
        }
        validateImportRows(rows);
        var lookup = loadImportLookup(rows.stream().map(ReadRow::data).toList());
        var successes = new ArrayList<ImportRowResult>();
        var errors = new ArrayList<ImportRowResult>();
        for (var row : rows) {
            var input = row.data();
            try {
                String outcome = importRow(input, updateSupport, lookup);
                successes.add(new ImportRowResult(row.number(), input.getUsername(), true, outcome));
            } catch (BizException e) {
                errors.add(new ImportRowResult(row.number(), input.getUsername(), false, e.getMessage()));
            }
        }
        if (!errors.isEmpty()) {
            throw new UserImportException(errors);
        }
        return successes;
    }

    private String importRow(UserImportRequest input, boolean updateSupport, ImportLookup lookup) {
        var old = lookup.users().get(input.getUsername());
        if (old != null && !updateSupport) {
            throw new BizException("用户名已存在");
        }
        Long phoneOwner = lookup.phones().get(input.getPhone());
        Long emailOwner = lookup.emails().get(input.getEmail());
        Long oldId = old == null ? null : old.getId();
        if (phoneOwner != null && !phoneOwner.equals(oldId)) {
            throw new BizException("手机号已存在");
        }
        if (emailOwner != null && !emailOwner.equals(oldId)) {
            throw new BizException("邮箱已存在");
        }

        var row = toImportedUser(input);
        if (old == null) {
            if (CollUtil.isEmpty(input.getRoleIds())) {
                throw new BizException("请至少选择一个角色");
            }
            row.setPassword(securityProperties.getInitPassword());
            saveNewUser(row, input.getRoleIds());
        } else {
            updateImportedUser(row, old, input.getRoleIds());
            lookup.phones().remove(old.getPhone(), oldId);
            lookup.emails().remove(old.getEmail(), oldId);
        }
        lookup.users().put(row.getUsername(), row);
        lookup.phones().put(row.getPhone(), row.getId());
        lookup.emails().put(row.getEmail(), row.getId());
        return old == null ? "导入成功" : "更新成功";
    }

    private void updateImportedUser(SysUser row, SysUser old, List<Long> roleIds) {
        assertTargetUserWritable(old);
        row.setId(old.getId());
        row.setVersion(old.getVersion());
        boolean newDept = !Objects.equals(row.getDeptId(), old.getDeptId());
        if (newDept) {
            assertDeptInDataScope(row.getDeptId());
        }
        if (roleIds != null) {
            roleDelegation.checkAssignment(roleIds, row.getDeptId());
        } else if (newDept) {
            roleDelegation.checkAssignment(List.copyOf(userRoleMapper.selectRoleIdsByUserId(old.getId())), row.getDeptId());
        }
        if (!updateById(row)) {
            throw new BizException(ResultCode.DATA_CONFLICT, "用户信息已变更，请刷新后重试");
        }
        if (roleIds != null) {
            userRoleMapper.deleteByUserId(old.getId());
            if (!roleIds.isEmpty()) {
                userRoleMapper.batchInsert(old.getId(), roleIds);
            }
        }
        if (CommonConstants.STATUS_DISABLE.equals(row.getStatus()) && !CommonConstants.STATUS_DISABLE.equals(old.getStatus())) {
            eventPublisher.publishEvent(new ForceLogoutEvent(Set.of(old.getId())));
        } else {
            eventPublisher.publishEvent(new UserCacheRefreshEvent(Set.of(old.getId())));
        }
        if (row.getVersion() != null && row.getVersion().equals(old.getVersion())) {
            row.setVersion(row.getVersion() + 1);
        }
    }

    private static SysUser toImportedUser(UserImportRequest input) {
        var row = new SysUser();
        row.setUsername(input.getUsername());
        row.setNickname(input.getNickname());
        row.setEmail(input.getEmail());
        row.setPhone(input.getPhone());
        row.setSex(input.getSex());
        row.setDeptId(input.getDeptId());
        row.setStatus(input.getStatus());
        row.setRemark(input.getRemark());
        return row;
    }

    private ImportLookup loadImportLookup(List<UserImportRequest> rows) {
        var usernames = rows.stream().map(UserImportRequest::getUsername).filter(StrUtil::isNotBlank).distinct().toList();
        var phones = rows.stream().map(UserImportRequest::getPhone).filter(StrUtil::isNotBlank).distinct().toList();
        var emails = rows.stream().map(UserImportRequest::getEmail).filter(StrUtil::isNotBlank).distinct().toList();
        var query = QueryWrapper.create().where(SYS_USER.DELETED.eq(0)).and(group -> {
            group.where("1 = 0");
            if (!usernames.isEmpty()) group.or(SYS_USER.USERNAME.in(usernames));
            if (!phones.isEmpty()) group.or(SYS_USER.PHONE.in(phones));
            if (!emails.isEmpty()) group.or(SYS_USER.EMAIL.in(emails));
        });
        var lookup = new ImportLookup(new HashMap<>(), new HashMap<>(), new HashMap<>());
        for (SysUser user : userMapper.selectListByQuery(query)) {
            lookup.users().put(user.getUsername(), user);
            lookup.phones().put(user.getPhone(), user.getId());
            lookup.emails().put(user.getEmail(), user.getId());
        }
        return lookup;
    }

    private record ImportLookup(Map<String, SysUser> users, Map<String, Long> phones, Map<String, Long> emails) {
    }

    private void validateImportRows(List<ReadRow<UserImportRequest>> rows) {
        var errors = new ArrayList<ImportRowResult>();
        for (var row : rows) {
            var messages = new LinkedHashSet<String>();
            if (row.error() != null) {
                messages.add(row.error());
            }
            var input = row.data();
            if (input != null) {
                validator.validate(input).stream().map(ConstraintViolation::getMessage).forEach(messages::add);
            }
            if (!messages.isEmpty()) {
                errors.add(new ImportRowResult(row.number(), input == null ? null : input.getUsername(),
                        false, String.join("; ", messages)));
            }
        }
        if (!errors.isEmpty()) {
            throw new UserImportException(errors);
        }
    }

    /**
     * 构建查询条件
     */
    private QueryWrapper buildQueryWrapper(UserQuery query) {
        QueryWrapper wrapper = QueryWrapper.create();

        if (StrUtil.isNotBlank(query.getUsername())) {
            wrapper.and(SYS_USER.USERNAME.like(query.getUsername()));
        }
        if (StrUtil.isNotBlank(query.getNickname())) {
            wrapper.and(SYS_USER.NICKNAME.like(query.getNickname()));
        }
        if (StrUtil.isNotBlank(query.getEmail())) {
            wrapper.and(SYS_USER.EMAIL.like(query.getEmail()));
        }
        if (StrUtil.isNotBlank(query.getPhone())) {
            wrapper.and(SYS_USER.PHONE.like(query.getPhone()));
        }
        if (query.getSex() != null) {
            wrapper.and(SYS_USER.SEX.eq(query.getSex()));
        }
        if (query.getStatus() != null) {
            wrapper.and(SYS_USER.STATUS.eq(query.getStatus()));
        }
        if (query.getDeptId() != null) {
            // 查询部门及子部门的用户
            Set<Long> deptIds = deptMapper.selectChildDeptIds(query.getDeptId());
            wrapper.and(SYS_USER.DEPT_ID.in(deptIds));
        }
        if (query.getBeginTime() != null) {
            wrapper.and(SYS_USER.CREATE_TIME.ge(query.getBeginTime()));
        }
        if (query.getEndTime() != null) {
            wrapper.and(SYS_USER.CREATE_TIME.le(query.getEndTime()));
        }

        wrapper.orderBy(SYS_USER.CREATE_TIME.desc());
        return wrapper;
    }

    private void fillDeptNames(List<SysUser> users) {
        if (CollUtil.isEmpty(users)) {
            return;
        }

        Set<Long> deptIds = users.stream()
                .map(SysUser::getDeptId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (deptIds.isEmpty()) {
            return;
        }

        QueryWrapper wrapper = QueryWrapper.create()
                .select(SYS_DEPT.ID, SYS_DEPT.DEPT_NAME)
                .where(SYS_DEPT.ID.in(deptIds));

        Map<Long, String> deptNameMap = deptMapper.selectListByQuery(wrapper).stream()
                .collect(Collectors.toMap(SysDept::getId, SysDept::getDeptName, (left, right) -> left));

        users.forEach(user -> user.setDeptName(deptNameMap.get(user.getDeptId())));
    }

    private void validatePassword(String password) {
        if (StrUtil.isBlank(password)) {
            throw new BizException("密码不能为空");
        }

        if (!BcryptPasswords.withinByteLimit(password)) {
            throw new BizException("密码不能超过 " + BcryptPasswords.MAX_BYTES + " 个 UTF-8 字节");
        }
        int length = password.length();
        if (length < securityProperties.getPasswordMinLength() || length > securityProperties.getPasswordMaxLength()) {
            throw new BizException(String.format("密码长度必须在%d-%d位之间", securityProperties.getPasswordMinLength(), securityProperties.getPasswordMaxLength()));
        }
    }

    private void assertTargetUserReadable(SysUser target) {
        assertTargetUserAccessible(target, "查看");
    }

    /**
     * 写操作数据权限校验：目标用户必须在操作者可见范围内。
     * <p>
     * 查询侧已由 @DataScope 过滤，但更新/删除/重置密码/改状态按 ID 直达，
     * 必须在此单独校验，否则拥有对应功能权限的用户可跨部门操作任意账号。
     */
    private void assertTargetUserWritable(SysUser target) {
        assertTargetUserAccessible(target, "操作");
    }

    private void assertTargetUserAccessible(SysUser target, String action) {
        LoginUser current = UserContext.getUser();
        if (current == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        // 超管账号只有超管本人可以操作
        if (CommonConstants.SUPER_ADMIN_ID.equals(target.getId()) && !current.isAdmin()) {
            throw new BizException(ResultCode.ACCESS_DENIED, "无权" + action + "超级管理员账号");
        }
        if (current.isAdmin()) {
            return;
        }
        // 自己的资料始终允许读取/维护
        if (target.getId() != null && target.getId().equals(current.getUserId())) {
            return;
        }
        if (!isDeptInScope(current, target.getDeptId())) {
            throw new BizException(ResultCode.ACCESS_DENIED, "无权" + action + "该部门的用户");
        }
    }

    /**
     * 校验部门是否在操作者数据权限范围内（用于新增用户 / 调整用户部门）
     */
    private void assertDeptInDataScope(Long deptId) {
        LoginUser current = UserContext.getUser();
        if (current == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        if (current.isAdmin()) {
            return;
        }
        if (!isDeptInScope(current, deptId)) {
            throw new BizException(ResultCode.ACCESS_DENIED, "无权在该部门下操作用户");
        }
    }

    private boolean isDeptInScope(LoginUser current, Long deptId) {
        return RoleDelegationPolicy.canAccessDepartment(current, deptId);
    }

    private void restrictUserVisibility(QueryWrapper query) {
        if (SystemDataScopeContext.isActive()) {
            return;
        }
        LoginUser current = UserContext.getUser();
        if (current == null || current.getUserId() == null) {
            query.and("1 = 0");
            return;
        }
        if (current.isAdmin()) {
            return;
        }
        query.and(SYS_USER.ID.ne(CommonConstants.SUPER_ADMIN_ID));
        var self = SYS_USER.ID.eq(current.getUserId());
        switch (DataScopeType.fromCode(current.getDataScope())) {
            case ALL -> { }
            case DEPT -> query.and(current.getDeptId() == null ? self : self.or(SYS_USER.DEPT_ID.eq(current.getDeptId())));
            case DEPT_AND_CHILD, CUSTOM -> {
                var ids = current.getDeptIds();
                query.and(ids == null || ids.isEmpty() ? self : self.or(SYS_USER.DEPT_ID.in(ids)));
            }
            default -> query.and(self);
        }
    }

}
