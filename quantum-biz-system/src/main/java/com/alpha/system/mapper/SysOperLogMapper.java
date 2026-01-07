package com.alpha.system.mapper;

import com.alpha.logging.entity.SysOperLog;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

/**
 * 操作日志 Mapper
 */
@Mapper
public interface SysOperLogMapper extends BaseMapper<SysOperLog> {

    /**
     * 清空操作日志
     */
    @Delete("TRUNCATE TABLE sys_oper_log")
    void cleanOperLog();

    /**
     * 删除指定天数之前的日志
     */
    @Delete("DELETE FROM sys_oper_log WHERE oper_time < NOW() - INTERVAL '#{days} days'")
    int deleteOperLogByDays(@Param("days") int days);

    /**
     * 删除指定时间之前的日志
     */
    @Delete("DELETE FROM sys_oper_log WHERE oper_time < #{datetime}")
    int deleteOperLogBefore(@Param("datetime") LocalDateTime datetime);

    /**
     * 统计日志数量
     */
    @Select("SELECT COUNT(*) FROM sys_oper_log")
    long countOperLog();

    /**
     * 统计指定时间范围内的日志数量
     */
    @Select("SELECT COUNT(*) FROM sys_oper_log WHERE oper_time BETWEEN #{startTime} AND #{endTime}")
    long countOperLogByTime(@Param("startTime") LocalDateTime startTime,
                            @Param("endTime") LocalDateTime endTime);

    /**
     * 统计指定状态的日志数量
     */
    @Select("SELECT COUNT(*) FROM sys_oper_log WHERE status = #{status}")
    long countOperLogByStatus(@Param("status") Integer status);
}
