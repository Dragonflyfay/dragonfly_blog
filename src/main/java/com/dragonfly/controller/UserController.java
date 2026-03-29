package com.dragonfly.controller;

import com.dragonfly.pojo.Result;
import com.dragonfly.pojo.User;
import com.dragonfly.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.Mapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 描述：
 *
 * @param
 * @author 蜻蜓大王
 * @date 2026/3/29 16:08
 */
@RestController
@RequestMapping("/user")
public class UserController {
    //注入对象
    @Autowired
    private UserService userService;
    @PostMapping ("/register")
    public Result register(String username, String password){
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
}
