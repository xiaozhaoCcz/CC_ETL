package com.cc.job.admin.task.controller;


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

    @Operation(summary = "登录")
    @PostMapping("/login")
    public Result<LoginResult> login(
            @Parameter(description = "用户名", example = "admin") @RequestParam String username,
            @Parameter(description = "密码", example = "123456") @RequestParam String password
    ) {

        return Result.success(LoginResult.builder()
                .tokenType("Bearer")
                .accessToken("default_token")
                .build());
    }

    @Operation(summary = "注销")
    @DeleteMapping("/logout")
    public Result<?> logout() {
        return Result.success();
    }
}
