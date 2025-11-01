package com.cc.job.admin.task.controller;


import com.cc.job.admin.task.service.JobUserService;
import com.cc.job.xo.common.result.Result;
import com.cc.job.xo.model.dto.LoginResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制层
 *
 * @author Ray
 * @since 2022/10/16
 */
@Tag(name = "01.认证中心")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final JobUserService jobUserService;

    @Operation(summary = "登录")
    @PostMapping("/login")
    public Result<LoginResult> login(
            @Parameter(description = "用户名", example = "admin") @RequestParam String username,
            @Parameter(description = "密码", example = "123456") @RequestParam String password
    ) {
        try {
            log.info("收到登录请求 - 用户名: {}", username);
            LoginResult loginResult = jobUserService.login(username, password);
            log.info("登录成功 - 用户名: {}", username);
            return Result.success(loginResult);
        } catch (Exception e) {
            log.error("登录失败 - 用户名: {}, 错误: {}", username, e.getMessage());
            return Result.failed(e.getMessage());
        }
    }

    @Operation(summary = "注册")
    @PostMapping("/register")
    public Result<LoginResult> register(
            @Parameter(description = "用户名", example = "newuser") @RequestParam String username,
            @Parameter(description = "密码", example = "123456") @RequestParam String password
    ) {
        try {
            log.info("收到注册请求 - 用户名: {}", username);
            LoginResult loginResult = jobUserService.register(username, password);
            log.info("注册成功 - 用户名: {}", username);
            return Result.success(loginResult);
        } catch (Exception e) {
            log.error("注册失败 - 用户名: {}, 错误: {}", username, e.getMessage());
            return Result.failed(e.getMessage());
        }
    }

    @Operation(summary = "注销")
    @DeleteMapping("/logout")
    public Result<?> logout() {
        log.info("用户注销");
        // TODO: 可以在这里清理session、token黑名单等
        return Result.success("注销成功");
    }
}
