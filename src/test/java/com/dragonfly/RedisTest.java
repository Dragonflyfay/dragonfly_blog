package com.dragonfly;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

/**
 * 描述：
 *
 * @param
 * @author 蜻蜓大王
 * @date 2026/4/25 16:00
 */
@SpringBootTest//在测试类上添加这个注解，则将来单元测试方法执行之前，会先初始化Spring容器，加载SpringBoot应用程序的上下文环境
public class RedisTest {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Test
    public void testSet(){
        //往Redis中存储一个键值对  StringRedisTemplate
        ValueOperations<String, String> operations = stringRedisTemplate.opsForValue();
        operations.set("username","zhangsan");
        operations.set("id","1",25, TimeUnit.SECONDS);


    }

    @Test
    public void testGet(){
        ValueOperations<String, String> operations = stringRedisTemplate.opsForValue();
        System.out.println(operations.get("username"));
    }
}
