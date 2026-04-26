package com.dragonfly.interceptors;

import com.dragonfly.pojo.Result;
import com.dragonfly.utils.JwtUtil;
import com.dragonfly.utils.ThreadLocalUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;

/**
 * 描述：拦截器
 *
 * @param
 * @author 蜻蜓大王
 * @date 2026/4/6 18:46
 */
@Component
public class LoginInterceptor implements HandlerInterceptor {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //令牌验证
        String token = request.getHeader("Authorization");

        //验证token
        try {
            //从redis中获取相同的token
            String redisToken = stringRedisTemplate.opsForValue().get(token);
            if(redisToken==null){
                throw new RuntimeException();
            }
            Map<String,Object> claims= JwtUtil.parseToken(token);
            //把业务数据存储到ThreadLocal中，供后续业务使用
            ThreadLocalUtil.set(claims);
            //放行
            return true;
        } catch (Exception e) {
            //http响应状态码401
            response.setStatus(401);
            //不放行
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable Exception ex) throws Exception {
       //清理ThreadLocal
        ThreadLocalUtil.remove();
    }
}
