package com.alpha.system.controller;

import com.alpha.framework.entity.LoginUser;
import com.alpha.orm.enums.DataScopeType;
import com.alpha.orm.permission.DataScope;
import com.alpha.system.domain.SysDictType;
import com.alpha.system.dto.request.UserQuery;
import com.alpha.system.service.ISysDictDataService;
import com.alpha.system.service.ISysDictTypeService;
import com.alpha.system.service.impl.SysUserServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.alpha.framework.config.JacksonConfig;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthorizationAuditTest {
    @Test
    void reservesTheAdminRoleForTheSuperAdministrator() {
        assertThat(new LoginUser().setUserId(2L).setRoles(Set.of("admin")).hasRole("admin")).isFalse();
        assertThat(new LoginUser().setUserId(1L).hasRole("admin")).isTrue();
    }

    @Test
    void userQueriesUseTheActorsDataScopeAndTheUsersId() throws Exception {
        for (String method : new String[] {"selectUserPage", "selectUserList"}) {
            var scope = SysUserServiceImpl.class.getMethod(method, UserQuery.class).getAnnotation(DataScope.class);
            assertThat(scope.type()).isEqualTo(DataScopeType.DEFAULT);
            assertThat(scope.userField()).isEqualTo("id");
        }
    }

    @Test
    void dictionaryCreateIgnoresServerOwnedFields() throws Exception {
        var types = mock(ISysDictTypeService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new SysDictController(types, mock(ISysDictDataService.class)))
                .setMessageConverters(new JacksonJsonHttpMessageConverter(new JacksonConfig().jsonMapper()))
                .build();
        mvc.perform(post("/system/dict/type").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"dictName":"Test","dictType":"test_type","status":1,"id":4,"version":7,"createBy":6,"deleted":1}
                            """))
                .andExpect(status().isOk());
        var input = ArgumentCaptor.forClass(SysDictType.class);
        verify(types).insertDictType(input.capture());
        assertThat(input.getValue().getDictName()).isEqualTo("Test");
        assertThat(input.getValue().getId()).isNull();
        assertThat(input.getValue().getVersion()).isNull();
        assertThat(input.getValue().getCreateBy()).isNull();
        assertThat(input.getValue().getDeleted()).isNull();
    }
}
