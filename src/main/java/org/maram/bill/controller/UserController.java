package org.maram.bill.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.maram.bill.common.security.UserContext;
import org.maram.bill.common.utils.Result;
import org.maram.bill.common.utils.ResultCode;
import org.maram.bill.entity.User;
import org.maram.bill.service.UserService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制器，处理用户相关的HTTP请求。
 * 包括微信登录、用户信息管理等。
 */
@RestController
@RequestMapping("/user")
@Slf4j
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserContext userContext;

    /**
     * 处理微信用户登录请求。
     * 成功登录后返回用户信息及JWT。
     *
     * @param payload 包含微信登录 code 的请求体
     * @return 包含登录信息（含JWT）的 {@link Result} 对象
     */
    @PostMapping("/wxlogin")
    public Result<Map<String, Object>> wxLogin(@RequestBody Map<String, String> payload) {
        String code = payload == null ? null : payload.get("code");
        log.info("微信用户登录请求，code: {}", code);

        if (code == null || code.trim().isEmpty()) {
            return Result.badRequest("code 不能为空");
        }

        try {
            Map<String, Object> loginInfo = userService.wxLogin(code);
            return Result.success(loginInfo);
        } catch (Exception e) {
            log.error("微信登录时发生未知错误, code: {}: {}", code, e.getMessage(), e);
            return Result.error("操作失败: " + e.getMessage());
        }
    }

    /**
     * 根据用户ID获取用户信息。
     * 此接口需要认证。
     *
     * @param id 用户ID
     * @return 包含 {@link User} (不含敏感信息) 的 {@link Result} 对象
     */
    @GetMapping("/{id}")
    public Result<User> getById(@PathVariable Long id) {
        log.info("请求获取用户ID: {} 的信息", id);
        return withCurrentUser(current -> {
            if (!isSelfOrAdmin(current, id)) {
                log.warn("用户 [{}] 无权访问用户ID [{}]", current.getId(), id);
                return Result.error(ResultCode.FORBIDDEN);
            }
            User user = userService.getById(id);
            if (user == null) {
                return Result.error(ResultCode.USER_NOT_FOUND);
            }
            return Result.success(user);
        });
    }

    /**
     * 根据用户名获取用户信息。
     * 此接口需要认证。
     *
     * @param username 用户名
     * @return 包含 {@link User} (不含敏感信息) 的 {@link Result} 对象
     */
    @GetMapping("/username/{username}")
    public Result<User> getByUsername(@PathVariable String username) {
        log.info("请求获取用户名: {} 的信息", username);
        return withCurrentUser(current -> {
            User user = userService.getByUsername(username);
            if (user == null) {
                return Result.error(ResultCode.USER_NOT_FOUND);
            }
            if (!isAdmin(current) && !user.getId().equals(current.getId())) {
                log.warn("用户 [{}] 无权访问用户名 [{}]", current.getId(), username);
                return Result.error(ResultCode.FORBIDDEN);
            }
            return Result.success(user);
        });
    }

    /**
     * 获取当前登录用户的信息。
     * 此接口需要认证。
     *
     * @return 包含当前登录 {@link User} (不含敏感信息) 的 {@link Result} 对象
     */
    @GetMapping("/me")
    public Result<User> getCurrentUser() {
        return withCurrentUser(user -> Result.success(user));
    }

    /**
     * 获取所有用户列表或分页结果，仅管理员可用
     */
    @GetMapping
    public Result<?> listAllUsers(
            @RequestParam(value = "current", required = false) Long current,
            @RequestParam(value = "size", required = false) Long size) {
        return withAdmin(admin -> {
            if (current == null && size == null) {
                return Result.success(userService.list());
            }
            long pageNum = (current == null || current <= 0) ? 1 : current;
            long pageSize = (size == null || size <= 0) ? 10 : size;
            log.info("管理员 [{}] 分页查询用户：第{}页，每页{}条", admin.getId(), pageNum, pageSize);
            Page<User> pageParam = new Page<>(pageNum, pageSize);
            Page<User> userPage = userService.page(pageParam, null);
            return Result.success(userPage);
        });
    }

    /**
     * 更新指定ID的用户信息。
     * 此接口需要认证。
     *
     * @param user 包含待更新信息的用户DTO，必须包含用户ID
     * @return 包含操作是否成功 (boolean) 的 {@link Result} 对象
     */
    @PutMapping("/{id}")
    public Result<Boolean> updateUserById(@PathVariable Long id, @RequestBody User user) {
        if (user == null) {
            return Result.badRequest("输入参数无效");
        }
        user.setId(id);
        return withCurrentUser(current -> {
            if (user.getId() == null) {
                return Result.badRequest("用户ID不能为空");
            }
            if (!isSelfOrAdmin(current, user.getId())) {
                log.warn("用户 [{}] 无权更新用户ID [{}]", current.getId(), user.getId());
                return Result.error(ResultCode.FORBIDDEN);
            }

            log.info("请求更新用户ID: {} 的信息", user.getId());
            boolean result = userService.updateUserById(user);
            if (result) {
                return Result.success(true);
            }
            return Result.error("用户更新失败");
        });
    }

    /**
     * 根据用户ID删除用户（逻辑删除）。
     * 此接口需要认证，并且可能需要管理员权限。
     *
     * @param id 待删除用户的ID
     * @return 包含操作是否成功 (boolean) 的 {@link Result} 对象
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> deleteUserById(@PathVariable Long id) {
        log.info("请求删除用户ID: {}", id);
        return withCurrentUser(current -> {
            if (!isSelfOrAdmin(current, id)) {
                log.warn("用户 [{}] 无权删除用户ID [{}]", current.getId(), id);
                return Result.error(ResultCode.FORBIDDEN);
            }

            User target = userService.getById(id);
            if (target == null) {
                return Result.error(ResultCode.USER_NOT_FOUND);
            }

            boolean result = userService.removeById(id);
            if (result) {
                return Result.success(true);
            }
            return Result.error("用户删除失败");
        });
    }

    private <T> Result<T> withCurrentUser(Function<User, Result<T>> action) {
        String openid = userContext.currentOpenid().orElse(null);
        if (!StringUtils.hasText(openid)) {
            log.warn("无法获取当前用户OpenID");
            return Result.error(ResultCode.UNAUTHORIZED);
        }
        User current = userService.getByOpenid(openid);
        if (current == null) {
            log.warn("根据OpenID [{}] 未找到用户", openid);
            return Result.error(ResultCode.USER_NOT_FOUND);
        }
        return action.apply(current);
    }

    private <T> Result<T> withAdmin(Function<User, Result<T>> action) {
        return withCurrentUser(current -> {
            if (!isAdmin(current)) {
                log.warn("用户 [{}] 非管理员，无权执行操作", current.getId());
                return Result.error(ResultCode.FORBIDDEN);
            }
            return action.apply(current);
        });
    }

    private boolean isSelfOrAdmin(User current, Long targetId) {
        return targetId != null && (targetId.equals(current.getId()) || isAdmin(current));
    }

    private boolean isAdmin(User user) {
        String role = user.getRole();
        if (!StringUtils.hasText(role)) {
            return false;
        }
        String normalized = role.toUpperCase();
        return "ROLE_ADMIN".equals(normalized) || "ADMIN".equals(normalized);
    }
}
