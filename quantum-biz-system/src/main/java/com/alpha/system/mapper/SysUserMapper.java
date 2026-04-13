package com.alpha.system.mapper;

import com.alpha.system.domain.SysUser;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户 Mapper
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 根据用户名查询用户
     */
    SysUser selectByUsername(@Param("username") String username);

    /**
     * 查询部门名称
     */
    String selectDeptNameById(@Param("deptId") Long deptId);

    /**
     * 检查用户名是否存在
     */
    int checkUsernameExists(@Param("username") String username);

    /**
     * 检查手机号是否存在
     */
    int checkPhoneExists(@Param("phone") String phone, @Param("excludeId") Long excludeId);

    /**
     * 检查邮箱是否存在
     */
    int checkEmailExists(@Param("email") String email, @Param("excludeId") Long excludeId);
}
