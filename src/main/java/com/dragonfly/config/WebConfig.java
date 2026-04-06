package com.dragonfly.config;

import com.dragonfly.interceptors.LoginInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 描述：
 *
 * @param
 * @author 蜻蜓大王
 * @date 2026/4/6 19:37
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    //注入拦截器
    @Autowired
    private LoginInterceptor loginInterceptor;
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        //登录和注册放行
       registry.addInterceptor(loginInterceptor).excludePathPatterns("/user/login","/user/register");
    }
}
