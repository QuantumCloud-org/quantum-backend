package com.alpha.system.service.impl;

import com.alpha.system.domain.SysLoginLog;
import com.alpha.system.mapper.SysLoginLogMapper;
import com.alpha.system.dto.request.LoginLogQuery;
import com.alpha.system.service.ISysLoginLogService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
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
public class SysLoginLogServiceImpl extends ServiceImpl<SysLoginLogMapper, SysLoginLog> implements ISysLoginLogService {

    private final SysLoginLogMapper loginLogMapper;
    private final TaskExecutor loginAuditExecutor;

    public SysLoginLogServiceImpl(SysLoginLogMapper loginLogMapper,
                                  @Qualifier("loginAuditExecutor") TaskExecutor loginAuditExecutor) {
        this.loginLogMapper = loginLogMapper;
        this.loginAuditExecutor = loginAuditExecutor;
    }

    @Override
    public void insertLoginLog(SysLoginLog loginLog) {
        try {
            loginAuditExecutor.execute(() -> persistLoginLog(loginLog));
        } catch (TaskRejectedException e) {
            log.warn("登录审计队列不可用，改为同步落库");
            persistLoginLog(loginLog);
        }
    }

    private void persistLoginLog(SysLoginLog loginLog) {
        try {
            loginLogMapper.insert(loginLog);
        } catch (Exception e) {
            log.error("记录登录日志异常: {}", e.getMessage(), e);
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
