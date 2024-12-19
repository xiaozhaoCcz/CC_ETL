package com.cc.job.shared.auth.service.impl;


import com.cc.job.shared.auth.service.AuthService;
import com.cc.job.system.model.dto.LoginResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;

import org.springframework.stereotype.Service;

import java.awt.*;
import java.util.concurrent.TimeUnit;

/**
 * 认证服务实现类
 *
 * @author haoxr
 * @since 2.4.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 登录
     *
     * @param username 用户名
     * @param password 密码
     * @return 登录结果
     */
    @Override
    public LoginResult login(String username, String password) {
        // 创建认证令牌对象
//        UsernamePasswordAuthenticationToken authenticationToken =
//                new UsernamePasswordAuthenticationToken(username.toLowerCase().trim(), password);
//        // 执行用户认证
//        Authentication authentication = authenticationManager.authenticate(authenticationToken);
//        // 认证成功后生成JWT令牌
//        String accessToken = JwtUtils.createToken(authentication);
//        // 将认证信息存入Security上下文，便于在AOP（如日志记录）中获取当前用户信息
//        SecurityContextHolder.getContext().setAuthentication(authentication);
        // 返回包含JWT令牌的登录结果
        return LoginResult.builder()
                .tokenType("Bearer")
                .accessToken("default_token")
                .build();
    }

    /**
     * 注销
     */
    @Override
    public void logout() {
//        String token = SecurityUtils.getTokenFromRequest();
//        if (StrUtil.isNotBlank(token)) {
//            SecurityUtils.invalidateToken(token);
//        }
    }

}
