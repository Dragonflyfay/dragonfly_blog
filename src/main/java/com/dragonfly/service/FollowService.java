package com.dragonfly.service;

import com.dragonfly.pojo.User;

import java.util.List;
import java.util.Map;

/**
 * 描述：关注
 *
 * @param
 * @author 蜻蜓大王
 * @date 2026/5/31 16:48
 */
public interface FollowService {
    //关注用户
    void follow(Integer userId);

    //取消关注
    void unfollow(Integer userId);

    //检查是否已关注
    boolean isFollowing(Integer userId);

    //批量检查关注状态
    Map<Integer, Boolean> batchCheckFollowing(List<Integer> userIds);

    //获取关注列表
    List<User> getFollowers(Integer userId);


    //获取关注列表
    List<User> getFollowing(Integer userId);
}
