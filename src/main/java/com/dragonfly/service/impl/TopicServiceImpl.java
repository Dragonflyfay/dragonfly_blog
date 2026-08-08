package com.dragonfly.service.impl;

import com.dragonfly.mapper.TopicMapper;
import com.dragonfly.pojo.Topic;
import com.dragonfly.service.TopicService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.type.TypeReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;// 添加日志记录
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 描述：话题服务实现类
 *
 * @param
 * @author 蜻蜓大王
 * @date 2026/5/5 15:29
 */
@Slf4j
@Service
public class TopicServiceImpl implements TopicService {

    @Autowired
    private TopicMapper topicMapper;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private ObjectMapper objectMapper;// Jackson对象
    private static final String TOPICS_CACHE_KEY="topics:all";// Redis缓存的key

    @Override
    public void add(Topic topic) {
        // 补充属性值
        topic.setCreateTime(LocalDateTime.now());
        topic.setUpdateTime(LocalDateTime.now());

        // 初始化笔记数量为0
        if (topic.getNotesCount() == null) {
            topic.setNotesCount(0);
        }

        topicMapper.add(topic);
        //清除缓存
        redisTemplate.delete(TOPICS_CACHE_KEY);
        log.info("🗑️ 已清除话题缓存");
    }
//    Redis改造前
//    @Override
//    public List<Topic> list() {
//        return topicMapper.list();
//    }
    @Override
    public List<Topic> list() {
       //1.先查Redis
        try {
            String cachedJson = redisTemplate.opsForValue().get(TOPICS_CACHE_KEY);
            if (cachedJson != null && !cachedJson.isEmpty()) {
                log.info("成功 从 Redis 缓存获取话题列表");
                // 用数组方式反序列化
                Topic[] topicsArray=objectMapper.readValue(cachedJson, Topic[].class);
                return Arrays.asList(topicsArray);// 将数组转换为列表
            }
        } catch (Exception e) {
            log.warn("Redis 缓存读取失败，将查询数据库", e);
            // 缓存数据可能损坏，删除它
            redisTemplate.delete(TOPICS_CACHE_KEY);
        }
        //2.查缓存未命中，查数据库
        log.info("缓存未命中，从数据库中查询话题列表");
        List<Topic> topics=topicMapper.list();//用mapper的方法从数据库中查
        //3.写入缓存
        if(topics!=null&&!topics.isEmpty()){
            try{
                String json=objectMapper.writeValueAsString(topics);// 用Jackson对象序列化
                // 设置缓存过期时间为1小时
                redisTemplate.opsForValue().set(TOPICS_CACHE_KEY,json,1, TimeUnit.HOURS);
                log.info("话题已缓存到Redis,共{}条",topics.size());
            } catch(Exception e){
                log.error("Redis 缓存写入失败",e);
            }

        }
        return topics;
    }

    @Override
    public Topic detail(Integer id) {
        return topicMapper.findById(id);
    }

    @Override
    public void update(Topic topic) {
        topic.setUpdateTime(LocalDateTime.now());
        topicMapper.update(topic);
        redisTemplate.delete(TOPICS_CACHE_KEY);
        log.info("已删除话题缓存");
    }

    @Override
    public void delete(Integer id) {
        int publishedCount = topicMapper.countPublishedNotesByTopicId(id);
        if (publishedCount > 0) {
            throw new RuntimeException("该话题下还有" + publishedCount + "篇已发布的笔记，无法删除");
        }
        topicMapper.delete(id);
        redisTemplate.delete(TOPICS_CACHE_KEY);
        log.info("已清除话题缓存");
    }
}
