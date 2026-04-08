package com.alpha.logging.service;

import com.alpha.logging.dto.LogPageQuery;
import com.alpha.logging.entity.SysOperLog;
import com.mybatisflex.core.paginate.Page;

import java.util.List;

/**
 * 操作日志 Service 接口
 */
public interface ISysOperLogService {

    /**
     * 新增操作日志
     *
     * @param operLog 操作日志对象
     */
    void insertOperLog(SysOperLog operLog);

    /**
     * 分页查询操作日志
     *
     * @param query 查询条件
     * @return 分页结果
     */
    Page<SysOperLog> selectOperLogPage(SysOperLog query, LogPageQuery pageQuery);

    /**
     * 查询操作日志列表
     *
     * @param query 查询条件
     * @return 日志列表
     */
    List<SysOperLog> selectOperLogList(SysOperLog query, LogPageQuery pageQuery);

    /**
     * 根据ID查询操作日志
     *
     * @param operId 日志ID
     * @return 操作日志
     */
    SysOperLog selectOperLogById(Long operId);

    /**
     * 批量删除操作日志
     *
     * @param operIds 日志ID数组
     */
    void deleteOperLogByIds(List<Long> operIds);

    /**
     * 清空操作日志
     */
    void cleanOperLog();

    /**
     * 删除指定天数之前的日志
     *
     * @param days 保留天数
     * @return 删除数量
     */
    int deleteOperLogByDays(int days);

    /**
     * 统计日志总数
     *
     * @return 日志总数
     */
    long countOperLog();

    /**
     * 统计异常日志数量
     *
     * @return 异常日志数量
     */
    long countErrorLog();
}
