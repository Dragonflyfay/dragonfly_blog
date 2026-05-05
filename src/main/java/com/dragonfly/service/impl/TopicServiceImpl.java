package com.dragonfly.service.impl;

import com.dragonfly.mapper.TopicMapper;
import com.dragonfly.pojo.Topic;
import com.dragonfly.service.TopicService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 描述：话题服务实现类
 *
 * @param
 * @author 蜻蜓大王
 * @date 2026/5/5 15:29
 */
@Service
public class TopicServiceImpl implements TopicService {

    @Autowired
    private TopicMapper topicMapper;

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
    }

    @Override
    public List<Topic> list() {
        return topicMapper.list();
    }

    @Override
    public Topic detail(Integer id) {
        return topicMapper.findById(id);
    }

    @Override
    public void update(Topic topic) {
        topic.setUpdateTime(LocalDateTime.now());
        topicMapper.update(topic);
    }

    @Override
    public void delete(Integer id) {
        topicMapper.delete(id);
    }
}
