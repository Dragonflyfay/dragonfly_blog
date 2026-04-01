package com.dragonfly.service.impl;

import com.dragonfly.mapper.UserMapper;
import com.dragonfly.pojo.User;
import com.dragonfly.service.UserService;
import com.dragonfly.utils.Md5Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 描述：
 *
 * @param
 * @author 蜻蜓大王
 * @date 2026/3/29 16:12
 */
@Service
public class UserServiceImpl implements UserService {
    //用Mapper层方法实现Sql查询
    @Autowired
    private UserMapper userMapper;
    @Override
    public User findByUserName(String username) {
        User u=userMapper.findByUserName(username);

        return u;
    }

    @Override
    public void register(String username, String password) {
        //加密处理  Md5加密
        String md5String = Md5Utils.getMD5String(password);


        //添加
        userMapper.add(username,md5String);

    }
}
