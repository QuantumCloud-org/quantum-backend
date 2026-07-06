package com.alpha.security.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecurityConfigTest {

    @Test
    void corsShouldAllowOnlyConfiguredOriginsWhenCredentialsAreEnabled() {
        SecurityProperties properties = new SecurityProperties();
        properties.setCorsAllowedOrigins(List.of("https://app.example.com"));

        CorsConfigurationSource source = newConfig(properties).corsConfigurationSource();
        CorsConfiguration cors = source.getCorsConfiguration(new MockHttpServletRequest("GET", "/system/user/list"));

        assertThat(cors).isNotNull();
        assertThat(cors.getAllowCredentials()).isTrue();
        assertThat(cors.checkOrigin("https://app.example.com")).isEqualTo("https://app.example.com");
        assertThat(cors.checkOrigin("https://evil.example.net")).isNull();
    }

    @Test
    void corsShouldRejectWildcardOriginWhenCredentialsAreEnabled() {
        SecurityProperties properties = new SecurityProperties();
        properties.setCorsAllowedOrigins(List.of("*"));

        assertThatThrownBy(() -> newConfig(properties).corsConfigurationSource())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CORS")
                .hasMessageContaining("credentials");
    }

    private SecurityConfig newConfig(SecurityProperties properties) {
        return new SecurityConfig(properties, null, null, null, null, null, null);
    }
}
