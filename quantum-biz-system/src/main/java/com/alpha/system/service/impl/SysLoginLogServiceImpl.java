package com.alpha.system.service.impl;

import com.alpha.framework.util.IpUtil;
import com.alpha.system.domain.SysLoginLog;
import com.alpha.system.mapper.SysLoginLogMapper;
import com.alpha.system.dto.request.LoginLogQuery;
import com.alpha.system.service.ISysLoginLogService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

import static com.alpha.system.domain.table.SysLoginLogTableDef.SYS_LOGIN_LOG;

/**
 * 登录日志服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysLoginLogServiceImpl extends ServiceImpl<SysLoginLogMapper, SysLoginLog> implements ISysLoginLogService {

    private final SysLoginLogMapper loginLogMapper;

    @Async
    @Override
    public void insertLoginLog(SysLoginLog loginLog) {
        try {
            loginLogMapper.insert(loginLog);
        } catch (Exception e) {
            log.error("记录登录日志异常: {}", e.getMessage(), e);
        }
    }

    @Async
    @Override
    public void recordLoginSuccess(String username, String ip, String browser, String os) {
        saveLoginLog(username, ip, browser, os, 0, "登录成功");
    }

    @Async
    @Override
    public void recordLoginFailure(String username, String ip, String browser, String os, String message) {
        saveLoginLog(username, ip, browser, os, 1, message);
    }

    private void saveLoginLog(String username, String ip, String browser, String os,
                              Integer status, String msg) {
        try {
            SysLoginLog loginLog = new SysLoginLog();
            loginLog.setUsername(username);
            loginLog.setIpaddr(ip);
            loginLog.setLoginLocation(IpUtil.getCityInfo(ip));
            loginLog.setBrowser(browser);
            loginLog.setOs(os);
            loginLog.setStatus(status);
            loginLog.setMsg(msg);
            loginLog.setLoginTime(LocalDateTime.now());

            loginLogMapper.insert(loginLog);

            if (log.isDebugEnabled()) {
                log.debug("登录日志记录成功 | User: {} | Status: {} | IP: {}",
                        username, status == 0 ? "成功" : "失败", ip);
            }
        } catch (Exception e) {
            log.error("登录日志记录失败 | User: {} | Error: {}", username, e.getMessage(), e);
        }
    }

    @Override
    public Page<SysLoginLog> selectLoginLogPage(LoginLogQuery query) {
        QueryWrapper wrapper = buildQueryWrapper(query);
        wrapper.orderBy(SYS_LOGIN_LOG.INFO_ID.desc());
        return loginLogMapper.paginate(
                Page.of(query.getPageNum(), query.getPageSize()),
                wrapper
        );
    }

    @Override
    public List<SysLoginLog> selectLoginLogList(LoginLogQuery query) {
        QueryWrapper wrapper = buildQueryWrapper(query);
        wrapper.orderBy(SYS_LOGIN_LOG.INFO_ID.desc());
        return loginLogMapper.selectListByQuery(wrapper);
    }

    @Override
    public void deleteLoginLogByIds(List<Long> infoIds) {
        if (infoIds != null && !infoIds.isEmpty()) {
            loginLogMapper.deleteBatchByIds(infoIds);
        }
    }

    @Override
    public void cleanLoginLog() {
        loginLogMapper.cleanLoginLog();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cleanExpiredLogs(int days) {
        LocalDateTime expireTime = LocalDateTime.now().minusDays(days);
        int count = loginLogMapper.deleteByLoginTimeBefore(expireTime);
        log.info("清理过期登录日志 | 保留天数: {} | 删除数量: {}", days, count);
        return count;
    }

    @Override
    public long countLoginLog() {
        return loginLogMapper.countLoginLog();
    }

    @Override
    public long countLoginSuccess() {
        return loginLogMapper.countLoginSuccess();
    }

    @Override
    public long countLoginFail() {
        return loginLogMapper.countLoginFail();
    }

    /**
     * 构建查询条件
     */
    private QueryWrapper buildQueryWrapper(LoginLogQuery query) {
        return QueryWrapper.create()
                .where(SYS_LOGIN_LOG.USERNAME.like(query.getUsername()).when(StringUtils.hasText(query.getUsername())))
                .and(SYS_LOGIN_LOG.IPADDR.like(query.getIpaddr()).when(StringUtils.hasText(query.getIpaddr())))
                .and(SYS_LOGIN_LOG.STATUS.eq(query.getStatus()).when(query.getStatus() != null))
                .and(SYS_LOGIN_LOG.LOGIN_TIME.ge(query.getBeginTime()).when(query.getBeginTime() != null))
                .and(SYS_LOGIN_LOG.LOGIN_TIME.le(query.getEndTime()).when(query.getEndTime() != null));
    }
}