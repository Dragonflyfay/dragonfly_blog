package com.dragonfly.controller;

import com.dragonfly.pojo.Result;
import com.dragonfly.pojo.Topic;
import com.dragonfly.service.TopicService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 描述：话题控制器
 *
 * @param
 * @author 蜻蜓大王
 * @date 2026/5/5 14:08
 */
@RestController
@RequestMapping("/topic")
public class TopicController {

    @Autowired
    private TopicService topicService;

    @PostMapping
    public Result add(@RequestBody @Validated(Topic.Add.class) Topic topic) {
        topicService.add(topic);
        return Result.success();
    }

    @GetMapping
    public Result<List<Topic>> list() {
        List<Topic> topics = topicService.list();
        return Result.success(topics);
    }

    // 获取话题详情
    @GetMapping("/detail")
    public Result<Topic> detail(Integer id) {
        Topic topic = topicService.detail(id);
        return Result.success(topic);
    }

    @PutMapping
    public Result update(@RequestBody @Validated(Topic.Update.class) Topic topic) {
        topicService.update(topic);
        return Result.success();
    }

    @DeleteMapping
    public Result delete(Integer id) {
        topicService.delete(id);
        return Result.success();
    }
}
