package com.wwzyhy.shopagent.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wwzyhy.shopagent.entity.AppUser;
import com.wwzyhy.shopagent.mapper.AppUserMapper;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

public class AppUserDetailsService implements UserDetailsService {

    private final AppUserMapper appUserMapper;

    public AppUserDetailsService(AppUserMapper appUserMapper) {
        this.appUserMapper = appUserMapper;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        QueryWrapper<AppUser> query = new QueryWrapper<>();
        query.eq("username", username);

        AppUser user = appUserMapper.selectOne(query);

        if (user == null) {
            throw new UsernameNotFoundException("用户名或密码不正确");
        }

        return User.withUsername(user.getUsername())
                .password(user.getPasswordHash())
                .roles("USER")
                .disabled(user.getStatus() != 1)
                .build();
    }
}