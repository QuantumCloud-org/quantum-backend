package com.alpha.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.alpha.framework.constant.CommonConstants;
import com.alpha.framework.exception.BizException;
import com.alpha.orm.enums.DataScopeType;
import com.alpha.orm.interceptor.DataPermissionInterceptor;
import com.alpha.orm.permission.DataScope;
import com.alpha.security.config.SecurityProperties;
import com.alpha.system.domain.SysUser;
import com.alpha.system.dto.request.UserQuery;
import com.alpha.system.mapper.SysDeptMapper;
import com.alpha.system.mapper.SysUserMapper;
import com.alpha.system.mapper.SysUserRoleMapper;
import com.alpha.system.service.ISysUserService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

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

    @Override
    @DataScope(type = DataScopeType.DEPT_AND_CHILD)
    public Page<SysUser> selectUserPage(UserQuery query) {
        QueryWrapper wrapper = buildQueryWrapper(query);
        DataPermissionInterceptor.applyDataScope(wrapper, "");
        return page(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
    }

    @Override
    @DataScope(type = DataScopeType.DEPT_AND_CHILD)
    public List<SysUser> selectUserList(UserQuery query) {
        QueryWrapper wrapper = buildQueryWrapper(query);
        DataPermissionInterceptor.applyDataScope(wrapper, "");
        return list(wrapper);
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

        // 更新用户
        updateById(user);

        // 更新用户角色关联
        if (roleIds != null) {
            userRoleMapper.deleteByUserId(user.getId());
            if (CollUtil.isNotEmpty(roleIds)) {
                userRoleMapper.batchInsert(user.getId(), roleIds);
            }
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

        // 删除用户角色关联
        for (Long userId : userIds) {
            userRoleMapper.deleteByUserId(userId);
        }

        // 逻辑删除用户
        return removeByIds(userIds);
    }

    @Override
    public boolean resetPassword(Long userId, String password) {
        validatePassword(password);
        SysUser user = new SysUser();
        user.setId(userId);
        user.setPassword(passwordEncoder.encode(password));
        return updateById(user);
    }

    @Override
    public boolean updateStatus(Long userId, Integer status) {
        if (status != 0 && status != 1) {
            throw new BizException("状态值无效，仅支持0(禁用)和1(正常)");
        }
        // 不能禁用管理员
        if (CommonConstants.SUPER_ADMIN_ID.equals(userId) && CommonConstants.STATUS_DISABLE.equals(status)) {
            throw new BizException("不能禁用超级管理员");
        }

        SysUser user = new SysUser();
        user.setId(userId);
        user.setStatus(status);
        return updateById(user);
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
    public Set<String> selectUserRoleKeys(Long userId) {
        return userMapper.selectUserRoleKeys(userId);
    }

    @Override
    public Set<String> selectUserPermissions(Long userId) {
        // 管理员拥有所有权限
        if (CommonConstants.SUPER_ADMIN_ID.equals(userId)) {
            return Set.of("*:*:*");
        }
        return userMapper.selectUserPermissions(userId);
    }

    @Override
    public void updateLoginInfo(Long userId, String ip) {
        SysUser user = new SysUser();
        user.setId(userId);
        user.setLoginIp(ip);
        user.setLoginDate(LocalDateTime.now());
        updateById(user);
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
                    user.setPassword(passwordEncoder.encode(RandomUtil.randomString(8)));
                    save(user);
                    successNum++;
                    successMsg.append("<br/>").append(successNum).append("、账号 ")
                            .append(user.getUsername()).append(" 导入成功");
                } else if (updateSupport) {
                    // 更新
                    user.setId(existUser.getId());
                    user.setPassword(null);
                    updateById(user);
                    successNum++;
                    successMsg.append("<br/>").append(successNum).append("、账号 ")
                            .append(user.getUsername()).append(" 更新成功");
                } else {
                    failureNum++;
                    failureMsg.append("<br/>").append(failureNum).append("、账号 ")
                            .append(user.getUsername()).append(" 已存在");
                }
            } catch (Exception e) {
                failureNum++;
                failureMsg.append("<br/>").append(failureNum).append("、账号 ")
                        .append(user.getUsername()).append(" 导入失败：").append(e.getMessage());
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
        if (StrUtil.isNotBlank(query.getPhone())) {
            wrapper.and(SYS_USER.PHONE.like(query.getPhone()));
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

    private void validatePassword(String password) {
        if (StrUtil.isBlank(password)) {
            throw new BizException("密码不能为空");
        }

        int length = password.length();
        if (length < securityProperties.getPasswordMinLength() || length > securityProperties.getPasswordMaxLength()) {
            throw new BizException(String.format("密码长度必须在%d-%d位之间",
                    securityProperties.getPasswordMinLength(), securityProperties.getPasswordMaxLength()));
        }
    }
}
