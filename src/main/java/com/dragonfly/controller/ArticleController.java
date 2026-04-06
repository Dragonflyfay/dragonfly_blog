package com.dragonfly.controller;

import com.dragonfly.pojo.Result;
import com.dragonfly.utils.JwtUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 描述：
 *
 * @param
 * @author 蜻蜓大王
 * @date 2026/4/6 18:16
 */
@RestController
@RequestMapping("/article")
public class ArticleController {
    @GetMapping("/list")
    public Result<String> list(/*@RequestHeader(name="Authorization")String token, HttpServletResponse response*/){
       /* //验证token
        try {
            Map<String,Object> claims= JwtUtil.parseToken(token);
            return Result.success("文章列表......");
        } catch (Exception e) {
            //http响应状态码401
            response.setStatus(401);
            return Result.error("未登录");
        }*/
        return Result.success("文章列表......");
    }
}
