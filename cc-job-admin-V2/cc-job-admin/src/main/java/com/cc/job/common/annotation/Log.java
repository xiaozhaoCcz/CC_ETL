package com.cc.job.common.annotation;

import com.cc.job.common.enums.LogModuleEnum;

import java.lang.annotation.*;

/**
 * 日志注解
 *
 * @author Ray
 * @since 2024/6/25
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface Log {

    String value() default "";

    LogModuleEnum module()  ;


}