package com.cc.job.admin.task.controller;


import com.cc.job.admin.task.service.JobUserService;
import com.cc.job.xo.common.result.Result;
import com.cc.job.xo.model.dto.LoginResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final JobUserService jobUserService;

    public AuthController(JobUserService jobUserService) {
        this.jobUserService = jobUserService;
    }

    @Operation(summary = "登录")
    @PostMapping("/login")
    public Result<LoginResult> login(
            @Parameter(description = "用户名", example = "admin") @RequestParam String username,
            @Parameter(description = "密码", example = "123456") @RequestParam String password
    ) {
        try {
            LoginResult loginResult = jobUserService.login(username, password);
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
            LoginResult loginResult = jobUserService.register(username, password);
            return Result.success(loginResult);
        } catch (Exception e) {
            log.error("注册失败 - 用户名: {}, 错误: {}", username, e.getMessage());
            return Result.failed(e.getMessage());
        }
    }

    @Operation(summary = "注销")
    @DeleteMapping("/logout")
    public Result<?> logout() {
        // TODO: 可以在这里清理session、token黑名单等
        return Result.success("注销成功");
    }
}
