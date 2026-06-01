package com.dragonfly.service.impl;

import com.dragonfly.mapper.FollowMapper;
import com.dragonfly.mapper.UserMapper;
import com.dragonfly.pojo.Follow;
import com.dragonfly.pojo.User;
import com.dragonfly.service.FollowService;
import com.dragonfly.utils.JwtUtil;
import com.dragonfly.utils.ThreadLocalUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 描述：关注
 *
 * @param
 * @author 蜻蜓大王
 * @date 2026/5/31 16:48
 */
@Service
public class FollowServiceImpl implements FollowService {

    private static final Logger log = LoggerFactory.getLogger(FollowServiceImpl.class);

    @Autowired
    private FollowMapper followMapper;
    @Autowired
    private UserMapper userMapper;
    private Integer getCurrentUserId() {
        Map<String, Object> map = ThreadLocalUtil.get();
        if (map != null && map.get("id") != null) {
            return (Integer) map.get("id");
        }

        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String token = request.getHeader("Authorization");
                if (token != null && !token.isEmpty()) {
                    Map<String, Object> claims = JwtUtil.parseToken(token);
                    return (Integer) claims.get("id");
                }
            }
        } catch (Exception e) {
            log.error("获取用户ID失败: {}", e.getMessage());
        }
        return null;
    }
    @Override
    @Transactional
    public void follow(Integer followingId) {
        Integer followerId = getCurrentUserId();
        if (followerId == null) {
            throw new RuntimeException("请先登录");
        }

        if (followerId.equals(followingId)) {
            throw new RuntimeException("不能关注自己");
        }

        // 检查是否已经关注过
        Follow existing = followMapper.findByUsers(followerId, followingId);
        if (existing != null) {
            if (existing.getStatus() == 1) {
                throw new RuntimeException("已经关注过了");
            } else {
                // 重新激活关注
                followMapper.reactivate(followerId, followingId);
                // 更新计数
                userMapper.incrementFollowersCount(followingId);
                userMapper.incrementFollowingCount(followerId);
                return;
            }
        }

        // 添加关注
        Follow follow = new Follow();
        follow.setFollowerId(followerId);
        follow.setFollowingId(followingId);
        followMapper.add(follow);

        // 更新计数
        userMapper.incrementFollowersCount(followingId);
        userMapper.incrementFollowingCount(followerId);

    }

    @Override
    @Transactional
    public void unfollow(Integer followingId) {
        Integer followerId = getCurrentUserId();
        if (followerId == null) {
            throw new RuntimeException("请先登录");
        }

        Follow existing = followMapper.findByUsers(followerId, followingId);
        if (existing == null || existing.getStatus() == 0) {
            throw new RuntimeException("尚未关注该用户");
        }

        followMapper.delete(followerId, followingId);

        // 更新计数
        userMapper.decrementFollowersCount(followingId);
        userMapper.decrementFollowingCount(followerId);
    }

    @Override
    public boolean isFollowing(Integer followingId) {
        Integer followerId = getCurrentUserId();
        if (followerId == null) {
            return false;
        }
        return followMapper.isFollowing(followerId, followingId);
    }


    @Override
    public Map<Integer, Boolean> batchCheckFollowing(List<Integer> userIds) {
        Map<Integer, Boolean> result = new HashMap<>();

        if (userIds == null || userIds.isEmpty()) {
            return result;
        }

        Integer followerId = getCurrentUserId();
        if (followerId == null) {
            for (Integer userId : userIds) {
                result.put(userId, false);
            }
            return result;
        }

        for (Integer followingId : userIds) {
            result.put(followingId, followMapper.isFollowing(followerId, followingId));
        }

        return result;
    }

    @Override
    public List<User> getFollowers(Integer userId) {
        return followMapper.getFollowers(userId);
    }

    @Override
    public List<User> getFollowing(Integer userId) {
        return followMapper.getFollowing(userId);
    }
}
