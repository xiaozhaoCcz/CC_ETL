package com.cc.job.admin.config;

import com.cc.job.admin.task.auth.AuthFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * 认证过滤器注册：对 /api/v1/** 要求携带有效 JWT 或执行器 accessToken，排除 login/register
 */
@Configuration
public class AuthFilterConfig {

    @Autowired
    private AuthFilter authFilter;

    @Bean
    public FilterRegistrationBean<AuthFilter> authFilterRegistration() {
        FilterRegistrationBean<AuthFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(authFilter);
        registration.addUrlPatterns("/api/v1/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10); // 在 CORS 之后
        registration.setName("authFilter");
        return registration;
    }
}
