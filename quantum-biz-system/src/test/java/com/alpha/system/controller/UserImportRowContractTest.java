package com.alpha.system.controller;

import com.alpha.security.config.SecurityProperties;
import com.alpha.system.convert.UserConvert;
import com.alpha.system.mapper.SysDeptMapper;
import com.alpha.system.mapper.SysUserMapper;
import com.alpha.system.mapper.SysUserRoleMapper;
import com.alpha.system.security.RoleDelegationPolicy;
import com.alpha.system.service.ISysRoleService;
import com.alpha.system.service.impl.SysUserServiceImpl;
import jakarta.validation.Validation;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.ByteArrayOutputStream;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserImportRowContractTest {
    @Test
    void importReportsConversionAndValidationErrorsAtPhysicalWorksheetRows() throws Exception {
        var mapper = mock(SysUserMapper.class);
        try (var validator = Validation.buildDefaultValidatorFactory()) {
            var service = new SysUserServiceImpl(mapper, mock(SysDeptMapper.class), mock(SysUserRoleMapper.class),
                    mock(PasswordEncoder.class), new SecurityProperties(), mock(ApplicationEventPublisher.class),
                    validator.getValidator(), mock(RoleDelegationPolicy.class));
            var mvc = MockMvcBuilders.standaloneSetup(
                    new SysUserController(service, mock(ISysRoleService.class), new UserConvert())).build();

            mvc.perform(multipart("/system/user/import").file(invalidUsers()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(3))
                    .andExpect(jsonPath("$.data[0].row").value(3))
                    .andExpect(jsonPath("$.data[0].username").value("alice"))
                    .andExpect(jsonPath("$.data[0].success").value(false))
                    .andExpect(jsonPath("$.data[0].message").value(allOf(
                            containsString("角色ID列表格式不正确"), containsString("手机号格式不正确"))))
                    .andExpect(jsonPath("$.data[1].row").value(5))
                    .andExpect(jsonPath("$.data[1].message").value(containsString("单元格格式不正确")))
                    .andExpect(jsonPath("$.data[2].row").value(6))
                    .andExpect(jsonPath("$.data[2].message").value(containsString("邮箱格式不正确")));
        }
        verifyNoInteractions(mapper);
    }

    private static MockMultipartFile invalidUsers() throws Exception {
        try (var book = new XSSFWorkbook(); var bytes = new ByteArrayOutputStream()) {
            var sheet = book.createSheet();
            String[] headers = {"用户名", "部门ID", "角色ID列表", "昵称", "邮箱", "手机号", "性别", "状态"};
            var header = sheet.createRow(0);
            for (int col = 0; col < headers.length; col++) {
                header.createCell(col).setCellValue(headers[col]);
            }
            addRow(sheet.createRow(2), "alice", "10", "bad-role", "alice@example.com", "bad-phone");
            addRow(sheet.createRow(3), "david", "10", "3", "david@example.com", "13800000002");
            addRow(sheet.createRow(4), "bobby", "bad-dept", "3", "bobby@example.com", "13800000003");
            addRow(sheet.createRow(5), "clara", "10", "3", "bad-email", "13800000004");
            book.write(bytes);
            return new MockMultipartFile("file", "users.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes.toByteArray());
        }
    }

    private static void addRow(org.apache.poi.ss.usermodel.Row row, String username, String dept,
                               String roles, String email, String phone) {
        String[] values = {username, dept, roles, username, email, phone, "男", "正常"};
        for (int col = 0; col < values.length; col++) {
            row.createCell(col).setCellValue(values[col]);
        }
    }
}
