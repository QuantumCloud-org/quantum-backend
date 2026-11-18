package com.alpha.system.service.impl;

import com.alpha.cache.constant.CacheKeyConstant;
import com.alpha.framework.exception.BizException;
import com.alpha.system.domain.SysDictData;
import com.alpha.system.mapper.SysDictTypeMapper;
import org.junit.jupiter.api.Test;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SysDictDataVisibilityTest {

    private final SysDictTypeMapper types = mock(SysDictTypeMapper.class);
    private final ConcurrentMapCacheManager caches = new ConcurrentMapCacheManager(CacheKeyConstant.SYS_DICT);
    private final SysDictDataServiceImpl service = spy(new SysDictDataServiceImpl(types, caches));

    @Test
    void deniedDictionaryTypeCannotBeReadFromSharedCache() {
        var cache = caches.getCache(CacheKeyConstant.SYS_DICT);
        cache.put("admin-type", List.of(new SysDictData()));
        when(types.selectCountByQuery(any())).thenReturn(0L);

        assertThat(service.selectDictDataByType("admin-type")).isEmpty();
    }

    @Test
    void batchDeleteChecksDistinctTypesTogetherBeforeDeleting() {
        var ids = List.of(1L, 2L, 3L);
        doReturn(List.of(data("external"), data("external"), data("shared"))).when(service).listByIds(ids);
        doReturn(true).when(service).removeByIds(ids);
        when(types.selectCountByQuery(any())).thenReturn(1L);
        when(types.selectListByQueryAs(any(), eq(String.class))).thenReturn(List.of("external", "shared"));

        assertThat(service.deleteDictDataByIds(ids)).isTrue();

        verify(types).selectListByQueryAs(any(), eq(String.class));
        verify(types, never()).selectCountByQuery(any());
        verify(service).removeByIds(ids);
    }

    @Test
    void batchDeleteDeniesAllRowsWhenAnyTypeIsInvisible() {
        var ids = List.of(1L, 2L, 3L);
        doReturn(List.of(data("external"), data("external"), data("internal"))).when(service).listByIds(ids);
        doReturn(true).when(service).removeByIds(ids);
        when(types.selectCountByQuery(any())).thenReturn(1L);
        when(types.selectListByQueryAs(any(), eq(String.class))).thenReturn(List.of("external"));

        assertThatThrownBy(() -> service.deleteDictDataByIds(ids))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("无权操作");
        verify(service, never()).removeByIds(any());
    }

    private static SysDictData data(String type) {
        var data = new SysDictData();
        data.setDictType(type);
        return data;
    }

    @Test
    void movingDictionaryDataEvictsBothKeysAfterCommit() {
        var cache = caches.getCache(CacheKeyConstant.SYS_DICT);
        cache.put("old-type", List.of());
        cache.put("new-type", List.of());
        cache.put("unrelated", List.of());
        var stored = new SysDictData();
        stored.setId(2L);
        stored.setDictType("old-type");
        var update = new SysDictData();
        update.setId(2L);
        update.setDictType("new-type");
        doReturn(stored).when(service).getById(2L);
        doReturn(true).when(service).updateById(update);
        when(types.selectCountByQuery(any())).thenReturn(1L);

        TransactionSynchronizationManager.initSynchronization();
        try {
            assertThat(service.updateDictData(update)).isTrue();
            assertThat(cache.get("old-type")).isNotNull();
            TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
            assertThat(cache.get("old-type")).isNull();
            assertThat(cache.get("new-type")).isNull();
            assertThat(cache.get("unrelated")).isNotNull();
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }
}
