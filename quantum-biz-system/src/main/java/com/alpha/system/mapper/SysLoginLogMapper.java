package com.alpha.system.mapper;

import com.alpha.system.domain.SysLoginLog;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

/**
 * 登录日志 Mapper
 */
@Mapper
public interface SysLoginLogMapper extends BaseMapper<SysLoginLog> {

    /**
     * 清空登录日志
     */
    @Delete("TRUNCATE TABLE sys_login_log")
    void cleanLoginLog();

    /**
     * 删除指定时间之前的日志
     */
    @Delete("DELETE FROM sys_login_log WHERE login_time < #{expireTime}")
    int deleteByLoginTimeBefore(@Param("expireTime") LocalDateTime expireTime);

    /**
     * 删除指定天数之前的日志
     */
    @Delete("DELETE FROM sys_login_log WHERE login_time < NOW() - INTERVAL '#{days} days'")
    int deleteLoginLogByDays(@Param("days") int days);

    /**
     * 统计日志数量
     */
    @Select("SELECT COUNT(*) FROM sys_login_log")
    long countLoginLog();

    /**
     * 统计登录成功数量
     */
    @Select("SELECT COUNT(*) FROM sys_login_log WHERE status = 0")
    long countLoginSuccess();

    /**
     * 统计登录失败数量
     */
    @Select("SELECT COUNT(*) FROM sys_login_log WHERE status = 1")
    long countLoginFail();
}