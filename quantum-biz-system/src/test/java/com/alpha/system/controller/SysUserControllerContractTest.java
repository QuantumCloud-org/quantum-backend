package com.alpha.system.controller;

import com.alpha.system.convert.UserConvert;
import com.alpha.system.service.ISysRoleService;
import com.alpha.system.service.ISysUserService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SysUserControllerContractTest {

    @Test
    void importEndpointShouldReturnBusinessErrorWhenFileIsEmpty() throws Exception {
        SysUserController controller = new SysUserController(
                mock(ISysUserService.class),
                mock(ISysRoleService.class),
                new UserConvert()
        );
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        MockMultipartFile emptyFile = new MockMultipartFile("file", "users.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[0]);

        mockMvc.perform(multipart("/system/user/import").file(emptyFile))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("请选择要导入的Excel文件"));
    }
}
