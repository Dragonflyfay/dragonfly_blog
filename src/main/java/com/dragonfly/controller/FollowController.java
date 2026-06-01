package com.dragonfly.controller;

import com.dragonfly.pojo.Result;
import com.dragonfly.pojo.User;
import com.dragonfly.service.FollowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 描述：关注
 *
 * @param
 * @author 蜻蜓大王
 * @date 2026/5/31 16:47
 */
@RestController
@RequestMapping("/follow")
public class FollowController {
    @Autowired
    private FollowService followService;
    // 关注用户
    @PostMapping("/{userId}")
    public Result follow(@PathVariable Integer userId) {
        followService.follow(userId);
        return Result.success();
    }

    // 取消关注
    @DeleteMapping("/{userId}")
    public Result unfollow(@PathVariable Integer userId) {
        followService.unfollow(userId);
        return Result.success();
    }

    // 检查是否已关注
    @GetMapping("/{userId}/check")
    public Result<Boolean> isFollowing(@PathVariable Integer userId) {
        boolean isFollowing = followService.isFollowing(userId);
        return Result.success(isFollowing);
    }
    // 批量检查关注状态
    @PostMapping("/batch-check")
    public Result<Map<Integer, Boolean>> batchCheckFollowing(@RequestBody List<Integer> userIds) {
        Map<Integer, Boolean> result = followService.batchCheckFollowing(userIds);
        return Result.success(result);
    }

    // 获取粉丝列表
    @GetMapping("/{userId}/followers")
    public Result<List<User>> getFollowers(@PathVariable Integer userId) {
        List<User> followers = followService.getFollowers(userId);
        return Result.success(followers);
    }

    // 获取关注列表
    @GetMapping("/{userId}/following")
    public Result<List<User>> getFollowing(@PathVariable Integer userId) {
        List<User> following = followService.getFollowing(userId);
        return Result.success(following);
    }






}
