package com.dragonfly.controller;

import com.dragonfly.pojo.Result;
import com.dragonfly.pojo.User;
import com.dragonfly.service.UserService;
import com.dragonfly.utils.JwtUtil;
import com.dragonfly.utils.Md5Utils;
import com.dragonfly.utils.ThreadLocalUtil;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.URL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @PostMapping("/register")
    //用Spring Validation注解对参数进行校验，要求用户名和密码必须是5-16位的非空字符串
    public Result register(@Pattern(regexp = "^\\S{5,16}$") String username, @Pattern(regexp = "^\\S{5,16}$") String password) {
        //查询用户
        User u = userService.findByUserName(username);
        //判断
        if (u == null) {
            // 没有查询到用户，说明用户名可用，执行注册
            userService.register(username, password);
            return Result.success();
        } else {
            //查询到用户，说明用户名已被占用，返回错误结果
            return Result.error("用户名已被占用");
        }


        //注册
    }

    @PostMapping("/login")
    public Result<String> login(@Pattern(regexp = "^\\S{5,16}$") String username, @Pattern(regexp = "^\\S{5,16}$") String password) {
        //根据用户名查询用户
        User loginUser = userService.findByUserName(username);


        //判断用户是否存在
        if (loginUser == null) {
            return Result.error("用户名错误");
        }

        //判断密码是否正确，loginUser对象中的密码是经过MD5加密的
        if (Md5Utils.getMD5String(password).equals(loginUser.getPassword())) {
            //密码正确，生成JWT令牌并返回
            Map<String, Object> claims = new HashMap<>();
            claims.put("id", loginUser.getId());
            claims.put("username", loginUser.getUsername());
            claims.put("role", loginUser.getRole());
            String token = JwtUtil.genToken(claims);
            //把token存储到redis 中
            ValueOperations<String, String> operations = stringRedisTemplate.opsForValue();
            operations.set(token,token,12, TimeUnit.HOURS);//与登录令牌一样，设置12小时有效期
            return Result.success(token);
        }
        return Result.error("密码错误");

    }

    @GetMapping("/userInfo")
    public Result<User> userInfo(/*@RequestHeader(name="Authorization") String token*/) {
        //根据用户名查询用户
        /*Map<String,Object> map=JwtUtil.parseToken(token);
        String username = (String)map.get("username");*/
        Map<String, Object> map = ThreadLocalUtil.get();
        String username = (String) map.get("username");
        User user = userService.findByUserName(username);
        return Result.success(user);


    }

    @PutMapping("/update")
    public Result update(@RequestBody @Validated User user) {//@RequestBody注解将请求体中的JSON数据映射为User对象

        /*//获取当前用户名
        Map<String,Object> map=ThreadLocalUtil.get();
        String username=(String)map.get("username");
        //根据用户名查询用户
        User loginUser=userService.findByUserName(username);
        //判断用户是否存在
        if(loginUser==null){
            return Result.error("用户不存在");
        }
        //更新用户信息
        user.setId(loginUser.getId());*/
        userService.update(user);
        return Result.success();
    }

    @PutMapping("/updateAvatar")
    public Result updateAvatar(@RequestParam @URL String avatarUrl) {
        //校验头像URL必须是一个有效的URL地址
        userService.updateAvatar(avatarUrl);
        return Result.success();
    }


    @PatchMapping("/updatePwd")
    public Result updatePwd(@RequestBody Map<String, String> params,@RequestHeader("Authorization") String token) {
        //1.校验参数
        String oldPwd = params.get("old_pwd");
        String newPwd = params.get("new_pwd");
        String rePwd = params.get("re_pwd");
        if (!StringUtils.hasLength(oldPwd) || !StringUtils.hasLength(newPwd) || !StringUtils.hasLength(rePwd)) {
            return Result.error("缺少必要的参数");
        }

        //原密码是否正确
        //调用userService根据用户名拿到原密码，载荷old_pwd对比
        Map<String, Object> map = ThreadLocalUtil.get();
        String username = (String) map.get("username");
        User loginUser = userService.findByUserName(username);
        //判断原密码是否正确,加密后对比
        if (!Md5Utils.getMD5String(oldPwd).equals(loginUser.getPassword())) {
            return Result.error("原密码填写不正确");
        }
        //新密码和确认密码是否一致
        if (!newPwd.equals(rePwd)) {
            return Result.error("新密码和确认密码不一致");
        }
        //2.调用Service完成密码更新
        userService.updatePwd(newPwd);
        //更新密码成功之后，要删除对应的token
        ValueOperations<String, String> operations = stringRedisTemplate.opsForValue();
        operations.getOperations().delete(token);
        return Result.success();

    }
}

