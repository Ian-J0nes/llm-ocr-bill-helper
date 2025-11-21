package org.maram.bill.config.security;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 配置类
 * 配置 JWT 认证、权限控制等安全策略
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/user/wxlogin",
                                "/error",
                                "/error/**",  // 允许所有错误端点
                                "/ai-config/models",  // AI模型列表公开访问
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()
                        .dispatcherTypeMatchers(
                                DispatcherType.ASYNC,
                                DispatcherType.ERROR,
                                DispatcherType.FORWARD,
                                DispatcherType.INCLUDE
                        ).permitAll()  // 允许所有异步、错误、转发和包含调度
                        .anyRequest().authenticated())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .userDetailsService(userDetailsService)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                // 处理异步请求的 Security Context 传播
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> {
                            log.warn("认证失败 - URI: {}, 方法: {}, 响应已提交: {}",
                                    request.getRequestURI(),
                                    request.getMethod(),
                                    response.isCommitted());
                            // 对于已提交的响应，不再尝试发送错误
                            if (!response.isCommitted()) {
                                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            }
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            log.warn("访问被拒绝 - URI: {}, 方法: {}, 响应已提交: {}, 异常: {}",
                                    request.getRequestURI(),
                                    request.getMethod(),
                                    response.isCommitted(),
                                    accessDeniedException.getMessage());
                            // 对于已提交的响应，不再尝试发送错误
                            if (!response.isCommitted()) {
                                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            }
                        }));
        return http.build();
    }
}
