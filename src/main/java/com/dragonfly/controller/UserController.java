package com.dragonfly.controller;

import com.dragonfly.pojo.Result;
import com.dragonfly.pojo.User;
import com.dragonfly.service.UserService;
import com.dragonfly.utils.JwtUtil;
import com.dragonfly.utils.Md5Utils;
import jakarta.validation.constraints.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 描述：
 *
 * @param
 * @author 蜻蜓大王
 * @date 2026/3/29 16:08
 */
@RestController
@RequestMapping("/user")
@Validated
public class UserController {
    //注入对象
    @Autowired
    private UserService userService;
    @PostMapping ("/register")
    //用Spring Validation注解对参数进行校验，要求用户名和密码必须是5-16位的非空字符串
    public Result register(@Pattern(regexp="^\\S{5,16}$") String username, @Pattern(regexp="^\\S{5,16}$")String password){
        //查询用户
        User u=userService.findByUserName(username);
        //判断
        if(u==null){
            // 没有查询到用户，说明用户名可用，执行注册
            userService.register(username,password);
            return Result.success();
        }else{
            //查询到用户，说明用户名已被占用，返回错误结果
            return Result.error("用户名已被占用");
        }





        //注册
    }
    @PostMapping ("/login")
    public Result<String> login(@Pattern(regexp="^\\S{5,16}$") String username, @Pattern(regexp="^\\S{5,16}$")String password){
        //根据用户名查询用户
        User loginUser=userService.findByUserName(username);


        //判断用户是否存在
        if(loginUser==null){
            return Result.error("用户名错误");
        }

        //判断密码是否正确，loginUser对象中的密码是经过MD5加密的
        if(Md5Utils.getMD5String( password).equals(loginUser.getPassword())){
            //密码正确，生成JWT令牌并返回
            Map<String,Object> claims=new HashMap<>();
            claims.put("id",loginUser.getId());
            claims.put("username",loginUser.getUsername());
            String token= JwtUtil.genToken(claims);
            return Result.success(token);
        }
        return Result.error("密码错误");

    }
}
