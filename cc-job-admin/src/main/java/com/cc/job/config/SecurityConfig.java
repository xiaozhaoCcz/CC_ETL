//package com.cc.job.config;
//
//import cn.hutool.captcha.generator.CodeGenerator;
//import cn.hutool.core.collection.CollectionUtil;
//import com.cc.job.common.constant.SecurityConstants;
//import com.cc.job.config.property.SecurityProperties;
//import com.cc.job.core.filter.RateLimiterFilter;
//import com.cc.job.core.security.exception.MyAccessDeniedHandler;
//import com.cc.job.core.security.exception.MyAuthenticationEntryPoint;
//import com.cc.job.core.security.filter.CaptchaValidationFilter;
//import com.cc.job.core.security.filter.JwtValidationFilter;
//import com.cc.job.system.service.ConfigService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.security.authentication.AuthenticationManager;
//import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
//import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
//import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
//import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
//import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
//import org.springframework.security.config.http.SessionCreationPolicy;
//import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.security.web.SecurityFilterChain;
//import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
//
///**
// * Spring Security 权限配置
// *
// * @author Ray
// * @since 2023/2/17
// */
////@Configuration
////@EnableWebSecurity
////@EnableMethodSecurity
//@RequiredArgsConstructor
//public class SecurityConfig {
//
//    private final MyAuthenticationEntryPoint authenticationEntryPoint;
//    private final MyAccessDeniedHandler accessDeniedHandler;
//    private final RedisTemplate<String, Object> redisTemplate;
//    private final CodeGenerator codeGenerator;
//    private final SecurityProperties securityProperties;
//    private final ConfigService configService;
//
//    //@Bean
//    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//        http
//
//                .authorizeHttpRequests(requestMatcherRegistry ->
//                        requestMatcherRegistry.requestMatchers(SecurityConstants.LOGIN_PATH).permitAll()
//                                .anyRequest().authenticated()
//                )
//                .exceptionHandling(httpSecurityExceptionHandlingConfigurer ->
//                        httpSecurityExceptionHandlingConfigurer
//                                .authenticationEntryPoint(authenticationEntryPoint)
//                                .accessDeniedHandler(accessDeniedHandler)
//                )
//                .sessionManagement(configurer -> configurer.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
//                .csrf(AbstractHttpConfigurer::disable)
//                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))
//
//        ;
//        // 限流过滤器
//        http.addFilterBefore(new RateLimiterFilter(redisTemplate, configService), UsernamePasswordAuthenticationFilter.class);
//        // 验证码校验过滤器
//        http.addFilterBefore(new CaptchaValidationFilter(redisTemplate, codeGenerator), UsernamePasswordAuthenticationFilter.class);
//        // JWT 校验过滤器
//        http.addFilterBefore(new JwtValidationFilter(redisTemplate,securityProperties.getJwt().getKey()), UsernamePasswordAuthenticationFilter.class);
//
//        return http.build();
//    }
//
//    /**
//     * 不走过滤器链的放行配置
//     */
//    //@Bean
//    public WebSecurityCustomizer webSecurityCustomizer() {
//        return (web) -> {
//            if (CollectionUtil.isNotEmpty(securityProperties.getIgnoreUrls())) {
//                web.ignoring().requestMatchers(securityProperties.getIgnoreUrls().toArray(new String[0]));
//            }
//        };
//    }
//
//    /**
//     * 密码编码器
//     */
//    //@Bean
//    public PasswordEncoder passwordEncoder() {
//        return new BCryptPasswordEncoder();
//    }
//
//    /**
//     * AuthenticationManager 手动注入
//     *
//     * @param authenticationConfiguration 认证配置
//     */
//    //@Bean
//    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
//        return authenticationConfiguration.getAuthenticationManager();
//    }
//
//}
