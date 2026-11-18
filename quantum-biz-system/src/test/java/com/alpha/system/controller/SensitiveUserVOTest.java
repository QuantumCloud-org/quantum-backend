package com.alpha.system.controller;

import com.alpha.framework.config.JacksonConfig;
import com.alpha.system.dto.response.UserVO;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SensitiveUserVOTest {
    @Test
    void masksPhoneAndEmailInUserResponses() {
        var user = new UserVO();
        user.setPhone("13812345678");
        user.setEmail("example@example.test");
        var mapper = new JacksonConfig().jsonMapper();
        var json = mapper.valueToTree(user);
        assertThat(json.get("phone").asString()).contains("*").isNotEqualTo(user.getPhone());
        assertThat(json.get("email").asString()).contains("*").isNotEqualTo(user.getEmail());
    }
}
