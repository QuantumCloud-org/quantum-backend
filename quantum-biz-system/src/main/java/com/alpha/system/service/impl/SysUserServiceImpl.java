package com.alpha.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alpha.framework.constant.CommonConstants;
import com.alpha.framework.entity.LoginUser;
import com.alpha.framework.enums.ResultCode;
import com.alpha.framework.exception.BizException;
import com.alpha.orm.enums.DataScopeType;
import com.alpha.orm.interceptor.DataPermissionInterceptor;
import com.alpha.orm.permission.DataScope;
import com.alpha.security.config.SecurityProperties;
import com.alpha.system.domain.SysDept;
import com.alpha.system.domain.SysUser;
import com.alpha.system.dto.request.UserQuery;
import com.alpha.system.mapper.SysDeptMapper;
import com.alpha.system.mapper.SysUserMapper;
import com.alpha.system.mapper.SysUserRoleMapper;
import com.alpha.system.security.ForceLogoutEvent;
import com.alpha.system.security.UserCacheRefreshEvent;
import com.alpha.system.service.ISysUserService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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

    private final static String first_pass = "123456";
    private final SysUserMapper userMapper;
    private final SysDeptMapper deptMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final SecurityProperties securityProperties;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @DataScope(type = DataScopeType.DEPT_AND_CHILD)
    public Page<SysUser> selectUserPage(UserQuery query) {
        QueryWrapper wrapper = buildQueryWrapper(query);
        DataPermissionInterceptor.applyDataScope(wrapper, "");
        Page<SysUser> pageResult =
                page(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        fillDeptNames(pageResult.getRecords());
        return pageResult;
    }

    @Override
    @DataScope(type = DataScopeType.DEPT_AND_CHILD)
    public List<SysUser> selectUserList(UserQuery query) {
        QueryWrapper wrapper = buildQueryWrapper(query);
        DataPermissionInterceptor.applyDataScope(wrapper, "");
        List<SysUser> users = list(wrapper);
        fillDeptNames(users);
        return users;
    }

    @Override
    public SysUser selectByUsername(String username) {
        return userMapper.selectByUsername(username);
    }

    @Override
    public SysUser selectUserById(Long userId) {
        return getById(userId);
    }

    @Override
    public String selectDeptNameById(Long deptId) {
        if (deptId == null) {
            return null;
        }
        return userMapper.selectDeptNameById(deptId);
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

        validatePassword(user.getPassword());

        // 加密密码
        user.setPassword(passwordEncoder.encode(user.getUsername() + user.getPassword()));

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

        // 资料/角色/部门变更时刷新缓存
        eventPublisher.publishEvent(new UserCacheRefreshEvent(Set.of(user.getId())));
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteUserByIds(List<Long> userIds) {
        // 不能删除管理员
        if (userIds.contains(CommonConstants.SUPER_ADMIN_ID)) {
            throw new BizException("不能删除超级管理员");
        }

        // 删除用户角色关联
        for (Long userId : userIds) {
            userRoleMapper.deleteByUserId(userId);
        }

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
        SysUser user = new SysUser();
        user.setId(userId);
        user.setPassword(passwordEncoder.encode(existUser.getUsername() + password));
        user.setVersion(version);
        boolean result = updateById(user);
        if (!result) {
            throw new BizException(ResultCode.DATA_CONFLICT, "用户信息已变更，请刷新后重试");
        }
        if (result) {
            eventPublisher.publishEvent(new ForceLogoutEvent(Set.of(userId)));
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateStatus(Long userId, Long version, Integer status) {
        if (status != 0 && status != 1) {
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

        SysUser user = new SysUser();
        user.setId(userId);
        user.setStatus(status);
        user.setVersion(version);
        boolean result = updateById(user);
        if (!result) {
            throw new BizException(ResultCode.DATA_CONFLICT, "用户状态已变更，请刷新后重试");
        }
        if (result) {
            if (CommonConstants.STATUS_DISABLE.equals(status)) {
                eventPublisher.publishEvent(new ForceLogoutEvent(Set.of(userId)));
            } else {
                eventPublisher.publishEvent(new UserCacheRefreshEvent(Set.of(userId)));
            }
        }
        return result;
    }

    @Override
    public boolean checkUsernameUnique(String username) {
        return userMapper.checkUsernameExists(username) == 0;
    }

    @Override
    public boolean checkPhoneUnique(String phone, Long excludeId) {
        return userMapper.checkPhoneExists(phone, excludeId != null ? excludeId : 0L) == 0;
    }

    @Override
    public boolean checkEmailUnique(String email, Long excludeId) {
        return userMapper.checkEmailExists(email, excludeId != null ? excludeId : 0L) == 0;
    }

    @Override
    public void updateLoginInfo(Long userId, LoginUser loginUser) {
        SysUser existUser = getById(userId);
        if (existUser == null) {
            return;
        }
        existUser.setLoginIp(loginUser.getLoginIp());
        existUser.setLoginLocation(loginUser.getLoginLocation());
        existUser.setLoginDate(LocalDateTime.now());
        updateById(existUser);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String importUsers(List<SysUser> userList, boolean updateSupport) {
        if (CollUtil.isEmpty(userList)) {
            throw new BizException("导入用户数据不能为空");
        }

        int successNum = 0;
        int failureNum = 0;
        StringBuilder successMsg = new StringBuilder();
        StringBuilder failureMsg = new StringBuilder();

        for (SysUser user : userList) {
            try {
                SysUser existUser = selectByUsername(user.getUsername());
                if (existUser == null) {
                    // 新增
                    user.setPassword(passwordEncoder.encode(user.getUsername() + first_pass));
                    save(user);
                    successNum++;
                    successMsg.append("<br/>").append(successNum).append("、账号 ").append(user.getUsername()).append(" 导入成功");
                } else if (updateSupport) {
                    // 更新
                    user.setId(existUser.getId());
                    user.setPassword(null);
                    user.setVersion(existUser.getVersion());
                    updateById(user);
                    successNum++;
                    successMsg.append("<br/>").append(successNum).append("、账号 ").append(user.getUsername()).append(" 更新成功");
                } else {
                    failureNum++;
                    failureMsg.append("<br/>").append(failureNum).append("、账号 ").append(user.getUsername()).append(" 已存在");
                }
            } catch (Exception e) {
                failureNum++;
                failureMsg.append("<br/>").append(failureNum).append("、账号 ").append(user.getUsername()).append(" 导入失败：").append(e.getMessage());
            }
        }

        if (failureNum > 0) {
            failureMsg.insert(0, "很抱歉，导入失败！共 " + failureNum + " 条数据格式不正确，错误如下：");
            throw new BizException(failureMsg.toString());
        } else {
            successMsg.insert(0, "恭喜您，数据已全部导入成功！共 " + successNum + " 条，数据如下：");
        }

        return successMsg.toString();
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
                .where(SYS_DEPT.ID.in(deptIds))
                .and(SYS_DEPT.DELETED.eq(0));

        Map<Long, String> deptNameMap = deptMapper.selectListByQuery(wrapper).stream()
                .collect(Collectors.toMap(SysDept::getId, SysDept::getDeptName, (left, right) -> left));

        users.forEach(user -> user.setDeptName(deptNameMap.get(user.getDeptId())));
    }

    private void validatePassword(String password) {
        if (StrUtil.isBlank(password)) {
            throw new BizException("密码不能为空");
        }

        int length = password.length();
        if (length < securityProperties.getPasswordMinLength() || length > securityProperties.getPasswordMaxLength()) {
            throw new BizException(String.format("密码长度必须在%d-%d位之间", securityProperties.getPasswordMinLength(), securityProperties.getPasswordMaxLength()));
        }
    }

}
