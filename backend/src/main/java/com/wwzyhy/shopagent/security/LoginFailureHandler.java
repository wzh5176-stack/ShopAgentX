package com.wwzyhy.shopagent.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Component
public class LoginFailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception) throws IOException {

        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");

        if (exception instanceof AuthenticationServiceException) {
            response.setStatus(503);
            response.getWriter().write("""
                {"error":{"code":"AUTH_SERVICE_UNAVAILABLE","message":"登录服务暂时不可用，请稍后再试"}}
                """);
            return;
        }

        response.setStatus(401);
        response.getWriter().write("""
            {"error":{"code":"INVALID_CREDENTIALS","message":"用户名或密码不正确"}}
            """);
    }
}