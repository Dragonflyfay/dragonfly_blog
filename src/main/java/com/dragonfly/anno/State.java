package com.dragonfly.anno;

import com.dragonfly.validation.StateValidation;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * 描述：
 *
 * @param
 * @author 蜻蜓大王
 * @date 2026/4/22 20:39
 */
@Documented  // 注解被javadoc工具所生成文档,元注解
@Target({ FIELD})//元注解描述注解的用法位置  filed属性方法
@Retention(RUNTIME)//元注解，描述注解的保留时间，生命周期
@Constraint(
        validatedBy = { StateValidation.class}//确定提供校验规则的类
)//元注解，描述注解的验证逻辑

public @interface State {
    // 校验失败错误提示信息
    String message() default "state的值只能是已发布，草稿";
    // 指定分组
    Class<?>[] groups() default {};

    // 指定负载，获取附加信息
    Class<? extends Payload>[] payload() default {};
}
