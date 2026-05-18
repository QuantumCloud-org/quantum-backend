package com.alpha.system.service.impl;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SysDeptServiceImplTest {

    @Test
    void normalizeUpdateParentIdShouldMapTopLevelSelfReferenceToRoot() {
        assertThat(SysDeptServiceImpl.normalizeUpdateParentId(100L, 100L, 0L, "0"))
                .isEqualTo(0L);
        assertThat(SysDeptServiceImpl.normalizeUpdateParentId(100L, 100L, 100L, "0"))
                .isEqualTo(0L);
    }

    @Test
    void normalizeUpdateParentIdShouldKeepNonRootSelfReferenceForValidation() {
        assertThat(SysDeptServiceImpl.normalizeUpdateParentId(100L, 100L, 99L, "0,99"))
                .isEqualTo(100L);
    }

    @Test
    void normalizeUpdateParentIdShouldMapBlankParentToRoot() {
        assertThat(SysDeptServiceImpl.normalizeUpdateParentId(100L, null, 0L, "0"))
                .isEqualTo(0L);
    }

    @Test
    void shouldRejectSelfParentShouldAllowRootDeptKeepingRootParent() {
        assertThat(SysDeptServiceImpl.shouldRejectSelfParent(0L, 0L, 0L, "0"))
                .isFalse();
    }

    @Test
    void shouldRejectSelfParentShouldRejectNonTopLevelDept() {
        assertThat(SysDeptServiceImpl.shouldRejectSelfParent(100L, 100L, 99L, "0,99"))
                .isTrue();
    }
}
