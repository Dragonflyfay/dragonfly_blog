package com.dragonfly.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Component
public class RedisCache {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;// RedisTemplate对象
    @Autowired
    private ObjectMapper objectMapper;// Jackson对象

    // ========== 基本操作 ==========

    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public void set(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    public <T> T get(String key) {
        return (T) redisTemplate.opsForValue().get(key);
    }

    public Boolean delete(String key) {
        return redisTemplate.delete(key);
    }

    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    // ========== 计数器（用于点赞/收藏/浏览） ==========

    public Long increment(String key) {
        return redisTemplate.opsForValue().increment(key);
    }

    public Long increment(String key, long delta) {
        return redisTemplate.opsForValue().increment(key, delta);
    }

    public Long decrement(String key) {
        return redisTemplate.opsForValue().decrement(key);
    }

    // ========== Set 集合操作（用于记录需要同步的ID） ==========

    /**
     * 向 Set 中添加元素
     */
    public Long addToSet(String key, String value) {
        return redisTemplate.opsForSet().add(key, value);
    }

    /**
     * 获取 Set 中的所有元素
     */
    public Set<Object> getSetMembers(String key) {
        return redisTemplate.opsForSet().members(key);
    }

    /**
     * 从 Set 中移除元素
     */
    public Long removeFromSet(String key, Object value) {
        return redisTemplate.opsForSet().remove(key, value);
    }

    /**
     * 判断 Set 中是否包含某个元素
     */
    public Boolean isMemberOfSet(String key, Object value) {
        return redisTemplate.opsForSet().isMember(key, value);
    }

    // ========== 批量操作 ==========

    public void setBatch(java.util.Map<String, Object> map) {
        redisTemplate.opsForValue().multiSet(map);
    }

    // ========== 过期时间 ==========

    public Boolean expire(String key, long timeout, TimeUnit unit) {
        return redisTemplate.expire(key, timeout, unit);
    }

    public Long getExpire(String key) {
        return redisTemplate.getExpire(key);
    }
    // 在 RedisCacheUtil 中添加
    private static final String NULL_VALUE = "null";

    public <T> T getOrNull(String key, Class<T> clazz) {
        String cached = redisTemplate.opsForValue().get(key).toString();
        if (cached == null) {
            return null;
        }
        if (NULL_VALUE.equals(cached)) {
            // 这是空值缓存，说明数据库中也没有
            return null;
        }
        try {
            return objectMapper.readValue(cached, clazz);
        } catch (Exception e) {
            log.warn("反序列化失败: key={}", key, e);
            return null;
        }
    }

    public void setNullCache(String key) {
        redisTemplate.opsForValue().set(key, NULL_VALUE, 5, TimeUnit.MINUTES);
    }
    /**
     * 获取 Set 中的所有元素（返回 String 集合，类型安全）
     */
    @SuppressWarnings("unchecked")
    public Set<String> getSetMembersAsString(String key) {
        Set<Object> members = redisTemplate.opsForSet().members(key);
        if (members == null || members.isEmpty()) {
            return new HashSet<>();
        }
        return members.stream()
                .map(Object::toString)
                .collect(Collectors.toSet());
    }
}