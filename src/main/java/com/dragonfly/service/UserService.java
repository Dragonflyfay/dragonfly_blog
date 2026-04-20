package com.dragonfly.service;

import com.dragonfly.pojo.User;

/**
 * 描述：
 *
 * @param
 * @author 蜻蜓大王
 * @date 2026/3/29 16:12
 */


public interface UserService {
    //根据用户名查询用户
    User findByUserName(String username);

    void register(String username, String password);

    //修改用户信息
    void update(User user);

    //修改用户头像
    void updateAvatar(String avatarUrl);
}
