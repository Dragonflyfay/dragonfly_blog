package com.dragonfly.anno;

import java.lang.annotation.*;

/**
 * 描述：权限校验注解
 *
 * @param
 * @author 蜻蜓大王
 * @date 2026/5/5 17:07
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireRole {
    /**
     * 需要的角色
     * ADMIN: 管理员
     * USER: 普通用户
     */
    String value() default "ADMIN";

    /**
     * 是否允许超级管理员
     */
    boolean allowSuperAdmin() default true;
}
