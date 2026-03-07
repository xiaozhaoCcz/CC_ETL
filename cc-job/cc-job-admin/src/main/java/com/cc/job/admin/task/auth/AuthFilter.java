package com.cc.job.admin.task.auth;

import com.cc.job.admin.task.utils.JwtUtil;
import com.cc.job.xo.common.result.Result;
import com.cc.job.xo.common.result.ResultCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * 认证过滤器：对 /api/v1/** 校验 JWT，排除登录/注册；
 * 执行器回调路径支持使用 accessToken（与 xxl.job.accessToken 一致）鉴权，通过后以系统用户身份放行。
 * 未带或无效 token 返回 401。
 */
@Component
public class AuthFilter extends OncePerRequestFilter {

    private static final List<String> EXCLUDE_PATHS = Arrays.asList(
            "/api/v1/auth/login",
            "/api/v1/auth/register"
    );

    /** 执行器回调路径：使用 accessToken 鉴权（与执行器 cc-job.job.accessToken 配置一致） */
    private static boolean isExecutorPath(String path) {
        if (path == null) return false;
        String p = path.toLowerCase();
        if ("/api/v1/jobinfos/status".equals(p)) return true;
        if (p.startsWith("/api/v1/jobinfos/updateranktriggerstatus/")) return true;
        if (p.startsWith("/api/v1/jobinfos/nodes/")) return true;
        if (p.startsWith("/api/v1/jobinfos/edges/")) return true;
        if ("/api/v1/jobinfos/batch".equals(p)) return true;
        if (p.matches("/api/v1/jobinfos/\\d+")) return true;
        if (p.startsWith("/api/v1/jobgroups/")) return true;
        if (p.startsWith("/api/v1/approvals/")) return true;
        if ("/api/v1/joblogs/savenodestatus".equals(p)) return true;
        if (p.startsWith("/api/v1/jobnoderesults/")) return true;
        return false;
    }

    /** 执行器 accessToken，与执行器配置一致；为空时使用 xxl.job.accessToken */
    @Value("${cc-job.executor.access-token:${xxl.job.accessToken:}}")
    private String executorAccessToken;

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
            String path = request.getRequestURI();
            String authHeader = request.getHeader("Authorization");

            // 执行器回调路径：允许使用 accessToken（与执行器配置一致）鉴权
            if (isExecutorPath(path) && executorAccessToken != null && !executorAccessToken.trim().isEmpty()) {
                String provided = authHeader != null ? authHeader.trim() : "";
                String expectedRaw = executorAccessToken.trim();
                String expectedBearer = JwtUtil.TOKEN_PREFIX + expectedRaw;
                if (expectedRaw.equals(provided) || expectedBearer.equals(provided)) {
                    AuthContext.set(PermissionConstants.SUPER_ADMIN_USER_ID, "executor");
                    request.setAttribute("userId", PermissionConstants.SUPER_ADMIN_USER_ID);
                    request.setAttribute("username", "executor");
                    filterChain.doFilter(request, response);
                    return;
                }
            }

            // 其余路径：要求 JWT
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
