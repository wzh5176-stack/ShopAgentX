package com.wwzyhy.shopagent.config;

import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import com.wwzyhy.shopagent.mapper.AppUserMapper;
import com.wwzyhy.shopagent.service.AppUserDetailsService;
import com.wwzyhy.shopagent.security.LoginSuccessHandler;
import com.wwzyhy.shopagent.security.LoginFailureHandler;

import java.nio.charset.StandardCharsets;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.WebAuthenticationDetails;


@Configuration
@PropertySource("file:./config/admin-local.properties")
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            LoginSuccessHandler loginSuccessHandler,
            LoginFailureHandler loginFailureHandler) throws Exception {

        AuthenticationEntryPoint unauthorizedHandler =
                (request, response, exception) -> {
                    response.setStatus(401);
                    if (!"XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
                        response.setHeader(
                                "WWW-Authenticate", "Basic realm=\"ShopAgent\"");
                    }
                    response.setCharacterEncoding("UTF-8");
                    response.setContentType("application/json");
                    response.getWriter().write("""
                {"error":{"code":"UNAUTHORIZED","message":"请提供有效的登录凭据"}}
                """);
                };

        AccessDeniedHandler forbiddenHandler =
                (request, response, exception) -> {
                    response.setStatus(403);
                    response.setCharacterEncoding("UTF-8");
                    response.setContentType("application/json");
                    response.getWriter().write("""
                        {"error":{"code":"FORBIDDEN","message":"请求被拒绝，请检查权限和安全令牌"}}
                        """);
                };

        http.authorizeHttpRequests(auth -> auth
                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                .requestMatchers(HttpMethod.POST, "/api/payments/alipay/notify").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/payments",
                        "/api/payments/*/refresh",
                        "/api/payments/*/mock-result",
                        "/api/payments/alipay/page", "/api/payments/alipay/*/page")
                .hasRole("USER")
                .requestMatchers(HttpMethod.GET, "/api/payments/*")
                .hasRole("USER")
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET,
                        "/api/health",
                        "/api/products",
                        "/api/products/*",
                        "/api/csrf",
                        "/shopagent/**")
                .permitAll()
                .requestMatchers(HttpMethod.POST,
                        "/api/assistant/ask",
                        "/api/assistant/knowledge",
                        "/api/auth/register",
                        "/api/auth/login")
                .permitAll()
                .requestMatchers(HttpMethod.GET, "/api/auth/me")
                .authenticated()
                .requestMatchers(HttpMethod.POST,
                        "/api/conversations",
                        "/api/conversations/*/messages",
                        "/api/conversations/*/messages/stream",
                        "/api/conversations/*/turns")
                .hasRole("USER")
                .requestMatchers(HttpMethod.GET,
                        "/api/conversations",
                        "/api/conversations/*",
                        "/api/conversations/*/messages")
                .hasRole("USER")
                .requestMatchers(HttpMethod.GET, "/api/cart")
                .hasRole("USER")
                .requestMatchers(HttpMethod.PUT, "/api/cart/items/*")
                .hasRole("USER")
                .requestMatchers(HttpMethod.DELETE, "/api/cart/items/*")
                .hasRole("USER")
                .requestMatchers(HttpMethod.POST,
                        "/api/conversations/*/cart-actions/prepare")
                .hasRole("USER")
                .requestMatchers(HttpMethod.POST,
                        "/api/conversations/*/cart-actions")
                .hasRole("USER")
                .requestMatchers(HttpMethod.GET,
                        "/api/conversations/*/cart-actions/*")
                .hasRole("USER")
                .requestMatchers(HttpMethod.POST,
                        "/api/conversations/*/cart-actions/*/confirm",
                        "/api/conversations/*/cart-actions/*/cancel")
                .hasRole("USER")
                .requestMatchers(HttpMethod.GET,
                        "/api/addresses",
                        "/api/addresses/*")
                .hasRole("USER")
                .requestMatchers(HttpMethod.POST, "/api/addresses")
                .hasRole("USER")
                .requestMatchers(HttpMethod.PUT,
                        "/api/addresses/*",
                        "/api/addresses/*/default")
                .hasRole("USER")
                .requestMatchers(HttpMethod.DELETE, "/api/addresses/*")
                .hasRole("USER")
                .requestMatchers(HttpMethod.POST, "/api/checkout/preview")
                .hasRole("USER")
                .requestMatchers(HttpMethod.POST, "/api/orders")
                .hasRole("USER")
                .requestMatchers(HttpMethod.GET, "/api/orders", "/api/orders/*", "/api/orders/*/payments")
                .hasRole("USER")
                .requestMatchers(HttpMethod.POST, "/api/orders/*/cancel")
                .hasRole("USER")
                .anyRequest().denyAll()
        );

        http.csrf(csrf -> csrf.ignoringRequestMatchers(request ->
                "POST".equals(request.getMethod())
                        && "/api/payments/alipay/notify".equals(request.getServletPath())));

        http.httpBasic(basic -> basic
                .authenticationEntryPoint(unauthorizedHandler)
        );

        http.exceptionHandling(errors -> errors
                .authenticationEntryPoint(unauthorizedHandler)
                .accessDeniedHandler(forbiddenHandler)
        );
        http.sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .sessionFixation(fixation -> fixation.changeSessionId())
        );

        http.formLogin(form -> form
                .loginProcessingUrl("/api/auth/login")
                .usernameParameter("username")
                .passwordParameter("password")
                .authenticationDetailsSource(request -> {
                    String username = request.getParameter("username");
                    String password = request.getParameter("password");

                    if (username == null
                            || !username.matches("[a-z][a-z0-9_]{2,31}")
                            || password == null
                            || password.isBlank()
                            || password.length() > 64
                            || password.getBytes(StandardCharsets.UTF_8).length > 72) {
                        throw new BadCredentialsException("用户名或密码不正确");
                    }

                    return new WebAuthenticationDetails(request);
                })
                .successHandler(loginSuccessHandler)
                .failureHandler(loginFailureHandler)
        );

        http.logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .clearAuthentication(true)
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .logoutSuccessHandler((request, response, authentication) -> {
                    response.setStatus(204);
                })
        );

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService(
            AppUserMapper appUserMapper,
            @Value("${shopagent.admin.password-hash}") String passwordHash) {

        UserDetails admin = User.withUsername("shopagent_admin")
                .password(passwordHash)
                .roles("ADMIN")
                .build();

        AppUserDetailsService appUsers =
                new AppUserDetailsService(appUserMapper);

        return username -> {
            if ("shopagent_admin".equals(username)) {
                return admin;
            }

            return appUsers.loadUserByUsername(username);
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
