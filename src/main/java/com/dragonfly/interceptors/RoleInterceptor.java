// interceptor/RoleInterceptor.java
package com.dragonfly.interceptors;

import com.dragonfly.anno.RequireRole;
import com.dragonfly.utils.ThreadLocalUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;

@Component// 将这个类注册为拦截器
// 实现HandlerInterceptor接口
public class RoleInterceptor implements HandlerInterceptor {
// 重写preHandle方法，拦截请求
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 判断当前请求是否为方法请求
        if (handler instanceof HandlerMethod) {
            HandlerMethod hm = (HandlerMethod) handler;
            RequireRole requireRole = hm.getMethodAnnotation(RequireRole.class);

            if (requireRole != null) {
                Map<String, Object> userInfo = ThreadLocalUtil.get();
                String role = (String) userInfo.get("role");
                String requiredRole = requireRole.value();

                if (role == null) {
                    response.setStatus(403);
                    return false;
                }

                String normalizedRole = role.toLowerCase();
                String normalizedRequiredRole = requiredRole.toLowerCase();

                boolean roleMatched = normalizedRole.equals(normalizedRequiredRole);

                if (requireRole.allowSuperAdmin() && "super_admin".equals(normalizedRole)) {
                    roleMatched = true;
                }

                if (!roleMatched) {
                    response.setStatus(403);
                    return false;
                }
            }
        }
        return true;
    }
}