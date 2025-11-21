package org.maram.bill.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.maram.bill.config.security.JwtTokenProvider;
import org.maram.bill.entity.User;
import org.maram.bill.integration.WxAuthClient;
import org.maram.bill.mapper.UserMapper;
import org.maram.bill.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 用户服务实现类
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private static final String ERROR_WX_LOGIN_FAILED = "微信登录失败：无法获取 openid";
    private static final String USERNAME_PREFIX = "wx_user_";
    private static final String DEFAULT_ROLE = "ROLE_USER";
    private static final String DEFAULT_STATUS = "ACTIVE";
    private static final int USERNAME_RANDOM_LENGTH = 12;

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;
    private final WxAuthClient wxAuthClient;

    @Override
    public Map<String, Object> wxLogin(String code) {
        String openid = wxAuthClient.exchangeCodeForOpenid(code);
        if (!StringUtils.hasText(openid)) {
            throw new IllegalStateException(ERROR_WX_LOGIN_FAILED);
        }

        User user = baseMapper.findByOpenid(openid).orElseGet(() -> createUser(openid));

        UserDetails userDetails = userDetailsService.loadUserByUsername(openid);
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);

        updateUserLoginTime(user);
        log.info("微信用户登录成功: openid={}, username={}", openid, user.getUsername());

        String token = jwtTokenProvider.generateToken(userDetails);
        return buildLoginResponse(user, token);
    }

    private User createUser(String openid) {
        String newUsername = generateUsername();
        User newUser = User.builder()
                .openid(openid)
                .username(newUsername)
                .role(DEFAULT_ROLE)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .status(DEFAULT_STATUS)
                .build();
        baseMapper.insert(newUser);
        log.info("新微信用户注册成功: openid={}, username={}", openid, newUsername);
        return newUser;
    }

    private String generateUsername() {
        return USERNAME_PREFIX + UUID.randomUUID().toString().replace("-", "").substring(0, USERNAME_RANDOM_LENGTH);
    }

    private void updateUserLoginTime(User user) {
        user.setLastLoginAt(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        baseMapper.updateById(user);
    }

    private Map<String, Object> buildLoginResponse(User user, String token) {
        Map<String, Object> result = new HashMap<>();
        result.put("userId", user.getId());
        result.put("openid", user.getOpenid());
        result.put("nickName", user.getUsername());
        result.put("token", token);
        return result;
    }

    @Override
    public User getById(Long id) {
        return baseMapper.selectById(id);
    }

    @Override
    public User getByUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return null;
        }
        return baseMapper.selectOne(new QueryWrapper<User>().eq("username", username));
    }

    @Override
    public User getByOpenid(String openid) {
        return StringUtils.hasText(openid) 
            ? baseMapper.findByOpenid(openid).orElse(null) 
            : null;
    }

    @Override
    public java.util.List<User> list() {
        return baseMapper.selectList(null);
    }

    @Override
    public boolean updateUserById(User user) {
        if (user.getId() == null) {
            return false;
        }
        
        User existingUser = baseMapper.selectById(user.getId());
        if (existingUser == null) {
            return false;
        }
        
        BeanUtils.copyProperties(user, existingUser, "id", "openid", "createTime");
        existingUser.setUpdateTime(LocalDateTime.now());
        return baseMapper.updateById(existingUser) > 0;
    }

    @Override
    public boolean removeById(Long id) {
        return baseMapper.deleteById(id) > 0;
    }

    @Override
    public Page<User> page(Page<User> page, QueryWrapper<User> queryWrapper) {
        return baseMapper.selectPage(page, queryWrapper);
    }

    @Override
    public Long getUserIdByOpenid(String openid) {
        return baseMapper.findByOpenid(openid).map(User::getId).orElse(null);
    }

    @Override
    public boolean updateAiConfig(String openid, String aiModel, Double aiTemperature) {
        return baseMapper.updateAiConfig(openid, aiModel, aiTemperature) > 0;
    }

    @Override
    public String getOpenidByUserId(Long userId) {
        if (userId == null) {
            return null;
        }
        User user = baseMapper.selectById(userId);
        return user != null ? user.getOpenid() : null;
    }
}
