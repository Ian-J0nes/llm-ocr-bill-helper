package org.maram.bill.config.security;

import java.util.Collections;

import lombok.RequiredArgsConstructor;
import org.maram.bill.mapper.UserMapper;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class DatabaseUserDetailsService implements UserDetailsService {

    private final UserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String openid) throws UsernameNotFoundException {
        return userMapper.findByOpenid(openid)
                .map(user -> {
                    String role = StringUtils.hasText(user.getRole()) ? user.getRole() : "ROLE_USER";
                    return new User(
                            user.getOpenid(),
                            "",
                            Collections.singletonList(new SimpleGrantedAuthority(role))
                    );
                })
                .orElseThrow(() -> new UsernameNotFoundException("未找到用户: " + openid));
    }
}
