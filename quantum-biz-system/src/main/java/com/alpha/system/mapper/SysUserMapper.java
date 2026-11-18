package com.alpha.system.mapper;

import com.alpha.system.domain.SysUser;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.time.LocalDateTime;

/**
 * 用户 Mapper
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 根据用户名查询用户
     */
    SysUser selectByUsername(@Param("username") String username);

    @Select("SELECT id FROM sys_user WHERE username = #{username} AND deleted = 0")
    Long selectIdByUsername(@Param("username") String username);


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
    @Update("""
            UPDATE sys_user SET login_ip = #{ip}, login_location = #{location}, login_date = #{loginDate}
            WHERE id = #{userId} AND deleted = 0
            """)
    int updateLoginMetadata(@Param("userId") Long userId, @Param("ip") String ip,
                            @Param("location") String location, @Param("loginDate") LocalDateTime loginDate);

    @Update("""
            UPDATE sys_user SET password = #{newPassword}
            WHERE id = #{userId} AND password = #{oldPassword} AND status = 1 AND deleted = 0
            """)
    int upgradePassword(@Param("userId") Long userId, @Param("oldPassword") String oldPassword,
                        @Param("newPassword") String newPassword);
}
