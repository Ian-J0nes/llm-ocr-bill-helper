package org.maram.bill.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 认证过滤器.
 * 这个过滤器在每个请求中只执行一次，负责从请求头中提取、验证JWT，并建立Spring Security的认证上下文。
 * 它继承了 OncePerRequestFilter 以确保在任何情况下（例如内部转发）都只被调用一次。
 */
@Component
@RequiredArgsConstructor // 使用Lombok为所有final字段生成构造函数，方便依赖注入
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtTokenProvider jwtTokenProvider; // 注入JWT工具类
    private final UserDetailsService userDetailsService; // 注入Spring Security的用户服务

    /**
     * 过滤器的核心逻辑。
     * @param request  HTTP请求
     * @param response HTTP响应
     * @param filterChain 过滤器链，用于将请求传递给下一个过滤器
     * @throws ServletException 如果发生Servlet相关错误
     * @throws IOException 如果发生I/O错误
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // 1. 从请求头中提取JWT
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String openid;

        // 如果请求头为空，或者不以 "Bearer " 开头，则直接放行，让后续的过滤器处理（可能会被拒绝）
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7); // 提取 "Bearer " 后面的Token部分

        // 2. 从JWT中解析出openid
        try {
            openid = jwtTokenProvider.getOpenidFromJWT(jwt);
        } catch (Exception e) {
            logger.warn("无效的JWT Token: {}", e.getMessage());
            // 如果解析失败，不设置安全上下文，直接放行，后续将被Spring Security的授权机制拦截
            filterChain.doFilter(request, response);
            return;
        }


        // 3. 验证Token并设置安全上下文
        // SecurityContextHolder.getContext().getAuthentication() == null 确保了只有在当前上下文没有认证信息时才执行
        if (StringUtils.hasText(openid) && SecurityContextHolder.getContext().getAuthentication() == null) {
            // 通过openid加载用户详细信息（这需要一个实现了UserDetailsService的Bean）
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(openid);

            // 验证Token是否有效（例如，签名和过期时间）
            if (jwtTokenProvider.validateToken(jwt, userDetails)) {
                // 如果Token有效，创建一个代表当前用户的认证凭证
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails, // principal: 核心用户信息
                        null,        // credentials: 对于JWT认证，我们不需要密码
                        userDetails.getAuthorities() // authorities: 用户的权限集合
                );

                // 将请求的详细信息（如IP地址、Session ID）设置到认证凭证中
                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                // 将认证凭证设置到安全上下文中，表示当前用户已通过认证
                SecurityContextHolder.getContext().setAuthentication(authToken);
//                logger.info("用户 '{}' 认证成功, 权限: {}. 已设置安全上下文", openid, authToken.getAuthorities());
            } else {
                logger.warn("JWT Token验证失败，用户 '{}'", openid);
            }
        }

        // 4. 将请求传递给下一个过滤器
        filterChain.doFilter(request, response);
    }
}
