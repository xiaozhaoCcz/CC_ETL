package com.cc.job.admin.task.auth;

import com.cc.job.admin.task.utils.JwtUtil;
import com.cc.job.xo.common.result.Result;
import com.cc.job.xo.common.result.ResultCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * 认证过滤器：对 /api/v1/** 校验 JWT，排除登录/注册；未带或无效 token 返回 401
 */
public class AuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AuthFilter.class);

    private static final List<String> EXCLUDE_PATHS = Arrays.asList(
            "/api/v1/auth/login",
            "/api/v1/auth/register"
    );

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path == null) {
            return false;
        }
        if (!path.startsWith("/api/v1/")) {
            return true;
        }
        return EXCLUDE_PATHS.stream().anyMatch(path::equalsIgnoreCase);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String authHeader = request.getHeader("Authorization");
            String token = null;
            if (authHeader != null && authHeader.startsWith(JwtUtil.TOKEN_PREFIX)) {
                token = authHeader.substring(JwtUtil.TOKEN_PREFIX.length()).trim();
            }
            if (token == null || token.isEmpty()) {
                writeUnauthorized(response, ResultCode.TOKEN_INVALID.getMsg());
                return;
            }
            if (JwtUtil.isTokenExpired(token)) {
                writeUnauthorized(response, "token已过期");
                return;
            }
            Long userId = JwtUtil.getUserIdFromToken(token);
            String username = JwtUtil.getUsernameFromToken(token);
            if (userId == null || username == null) {
                writeUnauthorized(response, ResultCode.TOKEN_INVALID.getMsg());
                return;
            }
            AuthContext.set(userId, username);
            request.setAttribute("userId", userId);
            request.setAttribute("username", username);
            filterChain.doFilter(request, response);
        } finally {
            AuthContext.clear();
        }
    }

    private void writeUnauthorized(HttpServletResponse response, String msg) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        Result<Void> result = Result.failed(ResultCode.TOKEN_INVALID, msg);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
