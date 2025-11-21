package org.maram.bill.common.security;

import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.maram.bill.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 简化获取当前登录用户信息的辅助组件。
 */
@Component
@RequiredArgsConstructor
public class UserContext {

    private final UserService userService;

    /**
     * 获取当前认证用户的 openid。
     */
    public Optional<String> currentOpenid() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        String openid = authentication.getName();
        return StringUtils.hasText(openid) ? Optional.of(openid) : Optional.empty();
    }

    /**
     * 获取当前认证用户的 ID。
     */
    public Optional<Long> currentUserId() {
        return currentOpenid()
                .map(userService::getUserIdByOpenid)
                .filter(id -> id != null && id > 0);
    }
}
