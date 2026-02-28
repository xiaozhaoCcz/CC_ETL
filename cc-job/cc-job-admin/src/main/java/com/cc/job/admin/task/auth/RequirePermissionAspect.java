package com.cc.job.admin.task.auth;

import com.cc.job.admin.exception.ForbiddenException;
import com.cc.job.admin.task.service.PermissionService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 权限注解 AOP：在带 @RequirePermission 的接口方法执行前校验当前用户是否拥有对应权限
 */
@Aspect
@Component
@Order(20)
public class RequirePermissionAspect {

    private final PermissionService permissionService;

    public RequirePermissionAspect(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @Around("@annotation(com.cc.job.admin.task.auth.RequirePermission)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequirePermission ann = method.getAnnotation(RequirePermission.class);
        if (ann == null) {
            return joinPoint.proceed();
        }
        String permissionCode = ann.value();
        Long userId = AuthContext.getUserId();
        if (userId == null) {
            throw new ForbiddenException("未登录或登录已过期");
        }
        if (!permissionService.hasPermission(userId, permissionCode)) {
            throw new ForbiddenException("无权限: " + permissionCode);
        }
        return joinPoint.proceed();
    }
}
