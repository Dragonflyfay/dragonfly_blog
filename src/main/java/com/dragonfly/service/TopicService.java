package com.dragonfly.service;


import com.dragonfly.pojo.Topic;

import java.util.List;

/**
 * 描述：
 *
 * @param
 * @author 蜻蜓大王
 * @date 2026/4/21 19:32
 */
public interface TopicService {
    //新增分类
    void add(Topic topic);

    // 查询话题列表
    List<Topic> list();

    // 根据id查询话题详情
    Topic detail(Integer id);

    // 更新话题
    void update(Topic topic);

    // 删除话题
    void delete(Integer id);
}
