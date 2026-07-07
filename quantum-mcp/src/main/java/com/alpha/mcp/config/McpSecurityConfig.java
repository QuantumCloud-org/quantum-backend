package com.alpha.mcp.config;

import com.alpha.mcp.security.OAuthBearerAuthenticationFilter;
import com.alpha.security.filter.TokenAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "ai.mcp", name = "enabled", havingValue = "true")
public class McpSecurityConfig {

    private final OAuthBearerAuthenticationFilter oAuthBearerAuthenticationFilter;
    private final TokenAuthenticationFilter tokenAuthenticationFilter;

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain mcpSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher(
                        "/mcp/**",
                        "/.well-known/oauth-protected-resource",
                        "/.well-known/oauth-authorization-server",
                        "/oauth/authorize",
                        "/oauth/token",
                        "/oauth/revoke")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET,
                                "/.well-known/oauth-protected-resource",
                                "/.well-known/oauth-authorization-server").permitAll()
                        .requestMatchers(HttpMethod.POST, "/oauth/token", "/oauth/revoke").permitAll()
                        .requestMatchers(HttpMethod.GET, "/oauth/authorize").authenticated()
                        .requestMatchers("/mcp/**").authenticated()
                        .anyRequest().denyAll())
                .addFilterBefore(tokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(oAuthBearerAuthenticationFilter, TokenAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public FilterRegistrationBean<OAuthBearerAuthenticationFilter> oAuthBearerAuthenticationFilterRegistration(
            OAuthBearerAuthenticationFilter filter) {
        FilterRegistrationBean<OAuthBearerAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
