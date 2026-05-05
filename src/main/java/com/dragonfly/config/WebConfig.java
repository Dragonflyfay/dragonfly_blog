package com.dragonfly.config;

import com.dragonfly.interceptors.RoleInterceptor;
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
    //注入拦截器
    @Autowired
    private RoleInterceptor roleInterceptor;
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        //登录和注册放行
       registry.addInterceptor(loginInterceptor).excludePathPatterns("/user/login","/user/register","/upload");
       //权限拦截器
        // 权限拦截器（验证角色）- 在登录拦截器之后执行
        // 注意：Spring Boot会按添加顺序执行，loginInterceptor先执行，authInterceptor后执行
        // authInterceptor只需要拦截需要权限校验的接口即可
        registry.addInterceptor(roleInterceptor)
                .addPathPatterns("/admin/**");  // 只有admin路径需要角色验证
    }
}
