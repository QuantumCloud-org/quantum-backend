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
import org.springframework.boot.web.servlet.FilterRegistrationBean;
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
        return new BCryptPasswordEncoder();
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

    /*
     * 以下四个 FilterRegistrationBean 用于禁用 Servlet 容器对 Filter Bean 的自动注册。
     *
     * 背景：Spring Boot 会把上下文中所有 Filter 类型的 Bean 自动注册为容器级过滤器，
     * 而这四个过滤器同时又被 addFilterBefore/After 加入了 Security 过滤链。
     * 由于它们都继承 OncePerRequestFilter（有 ALREADY_FILTERED 去重），实际会在
     * Servlet 层（认证之前、请求包装之前）先执行一次，链内的那次被跳过，导致：
     *  - RateLimitFilter 拿不到用户信息，限流永远按 IP 计；
     *  - RepeatSubmitFilter 拿不到 SecurityRequestWrapper，body MD5 从未参与 Key，
     *    同一 IP 5 秒内两个不同 POST 会被误判为重复提交。
     * 禁用自动注册后，它们只在 Security 链内按声明顺序执行一次。
     */
    @Bean
    public FilterRegistrationBean<TokenAuthenticationFilter> tokenAuthenticationFilterRegistration(TokenAuthenticationFilter filter) {
        FilterRegistrationBean<TokenAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<RequestWrapperFilter> requestWrapperFilterRegistration(RequestWrapperFilter filter) {
        FilterRegistrationBean<RequestWrapperFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration() {
        FilterRegistrationBean<RateLimitFilter> registration = new FilterRegistrationBean<>(rateLimitFilter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<RepeatSubmitFilter> repeatSubmitFilterRegistration() {
        FilterRegistrationBean<RepeatSubmitFilter> registration = new FilterRegistrationBean<>(repeatSubmitFilter);
        registration.setEnabled(false);
        return registration;
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
        config.setExposedHeaders(Arrays.asList("Authorization", "X-Trace-Id"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

}
