package com.dragonfly.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * 描述：Redis配置类
 *
 * @param
 * @author 蜻蜓大王
 * @date 2026/8/9 15:46
 */
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String,Object> redisTemplate(RedisConnectionFactory factory){
        RedisTemplate<String,Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);//设置连接工厂
        //使用String 序列化Key
        template.setKeySerializer(new StringRedisSerializer());//设置Key的序列化器
        template.setHashKeySerializer(new StringRedisSerializer());//设置HashKey的序列化器

        //  使用JSON序列化Value
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();//创建JSON序列化器
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();//初始化
        return template;
    }
}
