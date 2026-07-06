package com.alpha.system.dto.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RoleRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void createRequestShouldRejectInvalidDataScope() {
        RoleCreateRequest request = new RoleCreateRequest();
        request.setRoleName("部门管理员");
        request.setRoleKey("dept_admin");
        request.setDataScope(99);

        assertThat(validator.validate(request))
                .anySatisfy(violation -> assertThat(violation.getPropertyPath().toString()).isEqualTo("dataScope"));
    }

    @Test
    void updateRequestShouldRejectInvalidDataScope() {
        RoleUpdateRequest request = new RoleUpdateRequest();
        request.setId(2L);
        request.setVersion(1L);
        request.setRoleName("部门管理员");
        request.setRoleKey("dept_admin");
        request.setDataScope(99);

        assertThat(validator.validate(request))
                .anySatisfy(violation -> assertThat(violation.getPropertyPath().toString()).isEqualTo("dataScope"));
    }
}
