package com.alpha.system.service.impl;

import com.alpha.logging.entity.SysOperLog;
import com.alpha.logging.service.ISysOperLogService;
import com.alpha.orm.entity.PageQuery;
import com.alpha.system.mapper.SysOperLogMapper;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

import static com.alpha.logging.entity.table.SysOperLogTableDef.SYS_OPER_LOG;

/**
 * 操作日志 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysOperLogServiceImpl extends ServiceImpl<SysOperLogMapper, SysOperLog> implements ISysOperLogService {

    private final SysOperLogMapper operLogMapper;

    /**
     * 异步新增操作日志
     */
    @Async
    @Override
    public void insertOperLog(SysOperLog operLog) {
        try {
            operLogMapper.insert(operLog);
        } catch (Exception e) {
            log.error("记录操作日志异常: {}", e.getMessage(), e);
        }
    }

    @Override
    public Page<SysOperLog> selectOperLogPage(SysOperLog query, PageQuery pageQuery) {
        QueryWrapper wrapper = buildQueryWrapper(query);
        wrapper.orderBy(SYS_OPER_LOG.OPER_ID.desc());
        return operLogMapper.paginate(
                Page.of(pageQuery.getPageNum(), pageQuery.getPageSize()),
                wrapper
        );
    }

    @Override
    public List<SysOperLog> selectOperLogList(SysOperLog query, PageQuery pageQuery) {
        QueryWrapper wrapper = buildQueryWrapper(query);
        wrapper.orderBy(SYS_OPER_LOG.OPER_ID.desc());
        return operLogMapper.selectListByQuery(wrapper);
    }

    @Override
    public SysOperLog selectOperLogById(Long operId) {
        return operLogMapper.selectOneById(operId);
    }

    @Override
    public void deleteOperLogByIds(List<Long> operIds) {
        if (operIds != null && !operIds.isEmpty()) {
            operLogMapper.deleteBatchByIds(operIds);
        }
    }

    @Override
    public void cleanOperLog() {
        operLogMapper.cleanOperLog();
    }

    @Override
    public int deleteOperLogByDays(int days) {
        return operLogMapper.deleteOperLogByDays(days);
    }

    @Override
    public long countOperLog() {
        return operLogMapper.countOperLog();
    }

    @Override
    public long countErrorLog() {
        return operLogMapper.countOperLogByStatus(1); // 1 = 异常
    }

    /**
     * 构建查询条件
     */
    private QueryWrapper buildQueryWrapper(SysOperLog query) {
        return QueryWrapper.create()
                .where(SYS_OPER_LOG.TITLE.like(query.getTitle()).when(StringUtils.hasText(query.getTitle())))
                .and(SYS_OPER_LOG.BUSINESS_TYPE.eq(query.getBusinessType()).when(query.getBusinessType() != null))
                .and(SYS_OPER_LOG.OPER_NAME.like(query.getOperName()).when(StringUtils.hasText(query.getOperName())))
                .and(SYS_OPER_LOG.STATUS.eq(query.getStatus()).when(query.getStatus() != null))
                .and(SYS_OPER_LOG.OPER_TIME.ge(query.getOperTime()).when(query.getOperTime() != null))
                .and(SYS_OPER_LOG.OPER_TIME.le(query.getOperTime()).when(query.getOperTime() != null));
    }
}