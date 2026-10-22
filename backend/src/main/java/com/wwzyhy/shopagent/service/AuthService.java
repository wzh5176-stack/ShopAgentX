package com.wwzyhy.shopagent.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wwzyhy.shopagent.dto.RegisterRequest;
import com.wwzyhy.shopagent.dto.UserResponse;
import com.wwzyhy.shopagent.entity.AppUser;
import com.wwzyhy.shopagent.exception.UsernameAlreadyExistsException;
import com.wwzyhy.shopagent.mapper.AppUserMapper;
import java.nio.charset.StandardCharsets;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AppUserMapper appUserMapper;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            AppUserMapper appUserMapper,
            PasswordEncoder passwordEncoder) {
        this.appUserMapper = appUserMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String username = request.username();
        String password = request.password();

        if ("shopagent_admin".equals(username)) {
            throw new IllegalArgumentException("该用户名不能用于注册");
        }

        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("密码的UTF-8编码不能超过72字节");
        }

        QueryWrapper<AppUser> query = new QueryWrapper<>();
        query.eq("username", username);

        if (appUserMapper.selectCount(query) > 0) {
            throw new UsernameAlreadyExistsException();
        }

        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setStatus(1);

        int rows;
        try {
            rows = appUserMapper.insert(user);
        } catch (DuplicateKeyException exception) {
            throw new UsernameAlreadyExistsException();
        }

        if (rows != 1 || user.getId() == null) {
            throw new IllegalStateException("新增用户失败");
        }

        return new UserResponse(user.getId(), user.getUsername());
    }
}