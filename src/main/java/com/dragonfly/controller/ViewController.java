package com.dragonfly.controller;

import com.dragonfly.pojo.Result;
import com.dragonfly.service.ViewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 描述：浏览记录控制器
 *
 * @param
 * @author 蜻蜓大王
 * @date 2026/5/9 21:45
 */
@RestController
@RequestMapping("/view")
public class ViewController {
    @Autowired
    private ViewService viewService;

    // 记录浏览
    @PostMapping("/note/{noteId}")
    public Result recordView(@PathVariable Integer noteId) {
        viewService.recordView(noteId);
        return Result.success();
    }

    // 获取笔记的浏览次数
    @GetMapping("/note/{noteId}/count")
    public Result<Integer> getViewCount(@PathVariable Integer noteId) {
        int count = viewService.getViewCount(noteId);
        return Result.success(count);
    }
}
