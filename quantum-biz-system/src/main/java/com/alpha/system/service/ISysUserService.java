package com.alpha.system.service;

import com.alpha.system.domain.SysUser;
import com.alpha.system.dto.request.UserQuery;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;

import java.util.List;

/**
 * 用户服务接口
 */
public interface ISysUserService extends IService<SysUser> {

    /**
     * 分页查询用户
     */
    Page<SysUser> selectUserPage(UserQuery query);

    /**
     * 查询用户列表
     */
    List<SysUser> selectUserList(UserQuery query);

    /**
     * 根据用户名查询用户
     */
    SysUser selectByUsername(String username);

    /**
     * 根据ID查询用户详情（包含角色）
     */
    SysUser selectUserById(Long userId);

    /**
     * 新增用户
     */
    Long insertUser(SysUser user, List<Long> roleIds);

    /**
     * 修改用户
     */
    boolean updateUser(SysUser user, List<Long> roleIds);

    /**
     * 删除用户
     */
    boolean deleteUserByIds(List<Long> userIds);

    /**
     * 重置密码
     */
    boolean resetPassword(Long userId, String password);

    /**
     * 修改用户状态
     */
    boolean updateStatus(Long userId, Integer status);

    /**
     * 检查用户名是否唯一
     */
    boolean checkUsernameUnique(String username);

    /**
     * 检查手机号是否唯一
     */
    boolean checkPhoneUnique(String phone, Long excludeId);

    /**
     * 检查邮箱是否唯一
     */
    boolean checkEmailUnique(String email, Long excludeId);

    /**
     * 更新用户登录信息
     */
    void updateLoginInfo(Long userId, String ip);

    /**
     * 导入用户
     */
    String importUsers(List<SysUser> userList, boolean updateSupport);
}