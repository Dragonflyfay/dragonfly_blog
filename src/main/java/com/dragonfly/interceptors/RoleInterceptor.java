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

@Component
public class RoleInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (handler instanceof HandlerMethod) {
            HandlerMethod hm = (HandlerMethod) handler;
            RequireRole requireRole = hm.getMethodAnnotation(RequireRole.class);

            if (requireRole != null) {
                Map<String, Object> userInfo = ThreadLocalUtil.get();
                String role = (String) userInfo.get("role");
                String requiredRole = requireRole.value();

                if (role == null || !requiredRole.equalsIgnoreCase(role)) {
                    response.setStatus(403);
                    return false;
                }
            }
        }
        return true;
    }
}