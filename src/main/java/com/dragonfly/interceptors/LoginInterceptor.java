package com.dragonfly.interceptors;

import com.dragonfly.pojo.Result;
import com.dragonfly.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;

/**
 * 描述：
 *
 * @param
 * @author 蜻蜓大王
 * @date 2026/4/6 18:46
 */
@Component
public class LoginInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //令牌验证
        String token = request.getHeader("Authorization");
        //验证token
        try {
            Map<String,Object> claims= JwtUtil.parseToken(token);
            //放行
            return true;
        } catch (Exception e) {
            //http响应状态码401
            response.setStatus(401);
            //不放行
            return false;
        }
    }
}
