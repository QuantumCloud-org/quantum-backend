package com.alpha.security.config;

import com.alpha.security.filter.RateLimitFilter;
import com.alpha.security.filter.RepeatSubmitFilter;
import com.alpha.security.filter.RequestWrapperFilter;
import com.alpha.security.filter.TokenAuthenticationFilter;
import com.alpha.security.handler.AccessDeniedHandlerImpl;
import com.alpha.security.handler.AuthenticationEntryPointImpl;
import com.alpha.security.handler.LogoutSuccessHandlerImpl;
import com.alpha.security.token.TokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security 核心配置
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final SecurityProperties securityProperties;
    private final TokenService tokenService;
    private final AuthenticationEntryPointImpl authenticationEntryPoint;
    private final AccessDeniedHandlerImpl accessDeniedHandler;
    private final LogoutSuccessHandlerImpl logoutSuccessHandler;
    private final RepeatSubmitFilter repeatSubmitFilter;
    private final RateLimitFilter rateLimitFilter;

    public SecurityConfig(
            SecurityProperties securityProperties,
            TokenService tokenService,
            AuthenticationEntryPointImpl authenticationEntryPoint,
            AccessDeniedHandlerImpl accessDeniedHandler,
            LogoutSuccessHandlerImpl logoutSuccessHandler,
            RepeatSubmitFilter repeatSubmitFilter,
            RateLimitFilter rateLimitFilter) {
        this.securityProperties = securityProperties;
        this.tokenService = tokenService;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
        this.logoutSuccessHandler = logoutSuccessHandler;
        this.repeatSubmitFilter = repeatSubmitFilter;
        this.rateLimitFilter = rateLimitFilter;
    }

    /**
     * 密码编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(11);
    }

    /**
     * 认证管理器
     */
    @Bean
    public AuthenticationManager authenticationManager(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    @Bean
    public TokenAuthenticationFilter tokenAuthenticationFilter() {
        return new TokenAuthenticationFilter(tokenService, securityProperties);
    }

    @Bean
    public RequestWrapperFilter requestWrapperFilter() {
        return new RequestWrapperFilter(securityProperties);
    }

    /**
     * 安全过滤链
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // 白名单路径
        String[] whitelist = securityProperties.getWhitelist().toArray(new String[0]);

        http
                // CSRF（前后端分离不需要）
                .csrf(AbstractHttpConfigurer::disable)

                // Session（无状态）
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Headers
                .headers(headers -> headers
                        // 允许 iframe（如需要）
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)
                        // XSS 保护
                        .xssProtection(xss -> xss.headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
                        // 内容类型嗅探保护
                        .contentTypeOptions(contentType -> {
                        })
                        .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; object-src 'none'; frame-ancestors 'self'")))

                // 异常处理
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))

                // 授权配置
                .authorizeHttpRequests(auth -> auth
                        // 白名单路径
                        .requestMatchers(whitelist).permitAll()
                        // 探活端点公开
                        .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                        // 其他 actuator 端点仅管理员
                        .requestMatchers("/actuator/**").access((authentication, context) -> {
                            var auth2 = authentication.get();
                            if (auth2 == null || !auth2.isAuthenticated()) {
                                return new org.springframework.security.authorization.AuthorizationDecision(false);
                            }
                            if (auth2.getPrincipal() instanceof com.alpha.framework.entity.LoginUser loginUser) {
                                boolean isAdmin = loginUser.getRoles() != null && loginUser.getRoles().contains("admin");
                                return new org.springframework.security.authorization.AuthorizationDecision(isAdmin);
                            }
                            return new org.springframework.security.authorization.AuthorizationDecision(false);
                        })
                        // 静态资源
                        .requestMatchers(HttpMethod.GET, "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/*.html", "/*.css", "/*.js", "/favicon.ico", "/static/**", "/webjars/**", "/doc.html", "/error").permitAll()
                        // OPTIONS 预检请求
                        .requestMatchers(HttpMethod.OPTIONS).permitAll()
                        // 其他请求需要认证
                        .anyRequest().authenticated())

                // 登出
                .logout(logout -> logout
                        .logoutUrl("/auth/logout")
                        .logoutSuccessHandler(logoutSuccessHandler)
                        .clearAuthentication(true)
                        .invalidateHttpSession(true))

                // 添加自定义过滤器（执行顺序：TokenAuth → RateLimit → RequestWrapper → RepeatSubmit）
                // 1. TokenAuthenticationFilter 最先执行（认证）
                .addFilterBefore(tokenAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
                // 2. RateLimitFilter 在 TokenAuth 之后（此时已有用户信息）
                .addFilterAfter(rateLimitFilter, TokenAuthenticationFilter.class)
                // 3. RequestWrapperFilter 在 RateLimit 之后（包装请求体，供 RepeatSubmit 读取 body）
                .addFilterAfter(requestWrapperFilter(), RateLimitFilter.class)
                // 4. RepeatSubmitFilter 在 RequestWrapper 之后（此时可读取 body MD5）
                .addFilterAfter(repeatSubmitFilter, RequestWrapperFilter.class);

        log.info("【SecurityConfig】配置完成 | 白名单: {}", Arrays.toString(whitelist));

        return http.build();
    }

    /**
     * CORS 配置
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(securityProperties.getCorsAllowedOrigins());
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(Arrays.asList("Authorization", "X-Trace-Id", "X-Refresh-Token"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

}
