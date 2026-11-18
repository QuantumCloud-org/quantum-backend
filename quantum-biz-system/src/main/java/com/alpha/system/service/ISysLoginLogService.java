package com.alpha.system.service;

import com.alpha.system.domain.SysLoginLog;
import com.alpha.system.dto.request.LoginLogQuery;
import com.mybatisflex.core.paginate.Page;

import java.util.List;

/**
 * 登录日志服务接口
 */
public interface ISysLoginLogService {

    /**
     * 新增登录日志
     *
     * @param loginLog 登录日志对象
     */
    void insertLoginLog(SysLoginLog loginLog);

    /**
     * 分页查询登录日志
     *
     * @param query 查询条件
     * @return 分页结果
     */
    Page<SysLoginLog> selectLoginLogPage(LoginLogQuery query);

    /**
     * 批量删除登录日志
     *
     * @param infoIds 日志ID数组
     */
    void deleteLoginLogByIds(List<Long> infoIds);

    /**
     * 清空登录日志
     */
    void cleanLoginLog();

    /**
     * 清理过期日志
     *
     * @param days 保留天数
     * @return 删除数量
     */
    int cleanExpiredLogs(int days);
}