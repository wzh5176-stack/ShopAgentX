package com.wwzyhy.shopagent.config;

import com.wwzyhy.shopagent.security.LoginFailureHandler;
import com.wwzyhy.shopagent.security.LoginSuccessHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = SecurityRegressionTest.TestWebConfig.class)
@WebAppConfiguration
class SecurityRegressionTest {
    @Autowired WebApplicationContext context;
    private MockMvc mvc;
    @BeforeEach void setup() { mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build(); }

    @Configuration @EnableWebSecurity @EnableWebMvc
    static class TestWebConfig {
        // 直接执行生产配置的方法，不加载它的本机 PropertySource 或整个业务应用。
        @Bean SecurityFilterChain chain(HttpSecurity http) throws Exception {
            return new SecurityConfig().securityFilterChain(http, new LoginSuccessHandler(), new LoginFailureHandler());
        }
        @Bean UserDetailsService users() {
            return new InMemoryUserDetailsManager(User.withUsername("fixture").password("{noop}test-only").roles("USER").build());
        }
        @Bean ProbeEndpoint probe() { return new ProbeEndpoint(); }
    }
    @RestController static class ProbeEndpoint {
        @RequestMapping("/**") String respond() { return "test-only"; }
    }

    @ParameterizedTest @ValueSource(strings = {"/api/orders", "/api/addresses", "/api/cart", "/api/conversations"})
    void privateReadsRequireLogin(String path) throws Exception {
        mvc.perform(get(path)).andExpect(status().isUnauthorized());
    }
    @ParameterizedTest @ValueSource(strings = {"/api/orders", "/api/addresses", "/api/payments", "/api/orders/1/cancel"})
    void loggedInWritesStillRequireCsrf(String path) throws Exception {
        mvc.perform(post(path).with(user("fixture").roles("USER"))).andExpect(status().isForbidden());
    }
    @Test void userWithCsrfCanReachOrderHandler() throws Exception {
        mvc.perform(post("/api/orders").with(user("fixture").roles("USER")).with(csrf())).andExpect(status().isOk());
    }
    @Test void ordinaryUserCannotReachAdminEvenWithCsrf() throws Exception {
        mvc.perform(post("/api/admin/products").with(user("fixture").roles("USER")).with(csrf())).andExpect(status().isForbidden());
    }
    @Test void exactNotifyPostCanReachSignatureHandlerWithoutSessionOrCsrf() throws Exception {
        String path = "/api/payments/alipay/notify";
        mvc.perform(post(path).servletPath(path)).andExpect(status().isOk());
    }
    @Test void notifyPrefixDoesNotExemptOtherPaths() throws Exception {
        String path = "/api/payments/alipay/notify/extra";
        mvc.perform(post(path).servletPath(path)).andExpect(status().isForbidden());
    }
    @Test void notifyGetIsNotPublic() throws Exception {
        mvc.perform(get("/api/payments/alipay/notify")).andExpect(status().isUnauthorized());
    }
    @Test void unknownRoutesAreDenied() throws Exception {
        mvc.perform(get("/api/internal/anything").with(user("fixture").roles("USER"))).andExpect(status().isForbidden());
    }
    @Test void restockRequiresAdminAndCsrf() throws Exception {
        String path = "/api/admin/products/1/inventory/replenishments";
        mvc.perform(post(path).with(user("fixture").roles("USER")).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(post(path).with(user("admin").roles("ADMIN"))).andExpect(status().isForbidden());
        mvc.perform(post(path).with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().isOk());
    }
    @Test void inventoryAuditIsNotPublicOrAvailableToOrdinaryUsers() throws Exception {
        String path = "/api/admin/products/1/inventory/replenishments";
        mvc.perform(get(path)).andExpect(status().isUnauthorized());
        mvc.perform(get(path).with(user("fixture").roles("USER"))).andExpect(status().isForbidden());
        mvc.perform(get(path).with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
    }
}
