package com.alpha.system.service.impl;

import com.alpha.system.domain.SysUserRole;
import com.alpha.system.mapper.SysRoleDeptMapper;
import com.alpha.system.mapper.SysRoleMapper;
import com.alpha.system.mapper.SysRoleMenuMapper;
import com.alpha.system.mapper.SysUserRoleMapper;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SysRoleServiceImplRoleMappingTest {

    @Test
    void selectRoleIdsByUserIdsShouldBatchAndGroupRoleIds() {
        SysUserRoleMapper userRoleMapper = mock(SysUserRoleMapper.class);
        SysRoleServiceImpl service = new SysRoleServiceImpl(
                mock(SysRoleMapper.class),
                mock(SysRoleMenuMapper.class),
                mock(SysRoleDeptMapper.class),
                userRoleMapper,
                mock(ApplicationEventPublisher.class)
        );
        when(userRoleMapper.selectByUserIds(List.of(2L, 3L))).thenReturn(List.of(
                relation(2L, 5L),
                relation(2L, 6L),
                relation(3L, 7L)
        ));

        Map<Long, Set<Long>> result = service.selectRoleIdsByUserIds(Arrays.asList(2L, null, 2L, 3L));

        assertThat(result.get(2L)).containsExactly(5L, 6L);
        assertThat(result.get(3L)).containsExactly(7L);
        verify(userRoleMapper).selectByUserIds(List.of(2L, 3L));
    }

    private SysUserRole relation(Long userId, Long roleId) {
        return new SysUserRole()
                .setUserId(userId)
                .setRoleId(roleId);
    }
}
