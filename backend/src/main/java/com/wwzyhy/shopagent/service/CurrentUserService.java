package com.wwzyhy.shopagent.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wwzyhy.shopagent.entity.AppUser;
import com.wwzyhy.shopagent.mapper.AppUserMapper;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    private final AppUserMapper appUserMapper;

    public CurrentUserService(AppUserMapper appUserMapper) {
        this.appUserMapper = appUserMapper;
    }

    public Long requireUserId(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new AuthenticationCredentialsNotFoundException("请先登录");
        }

        String username = authentication.getName();

        QueryWrapper<AppUser> query = new QueryWrapper<>();
        query.select("id", "status");
        query.eq("username", username);

        AppUser user = appUserMapper.selectOne(query);

        if (user == null
                || user.getId() == null
                || !Integer.valueOf(1).equals(user.getStatus())) {
            throw new AuthenticationCredentialsNotFoundException("当前账号不可用");
        }

        return user.getId();
    }
}