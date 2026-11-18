package com.alpha.system.mapper;

import com.alpha.logging.entity.SysOperLog;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;


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
    @Delete("DELETE FROM sys_oper_log WHERE oper_time < NOW() - make_interval(days => #{days})")
    int deleteOperLogByDays(@Param("days") int days);
}
