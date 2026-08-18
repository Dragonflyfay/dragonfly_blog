package com.dragonfly.controller;

import com.dragonfly.pojo.Result;
import com.dragonfly.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 描述：管理员后台控制器
 *
 * @param
 * @author 蜻蜓大王
 * @date 2026/8/18 20:39
 */
@RestController
@RequestMapping("/admin")
public class AdminController {
    @Autowired
    AdminService adminService;

    /**
     * 获取管理员仪表盘统计数据
     */
    @GetMapping("/dashboard/stats")
    public Result<Map<String, Object>> getDashboardStats() {
        Map<String, Object> stats = adminService.getDashboardStats();
        return Result.success(stats);
    }

    /**
     * 获取最近7天每日新增数据（用于图表）
     */
    @GetMapping("/dashboard/weekly-trend")
    public Result<Map<String, Object>> getWeeklyTrend() {
        Map<String, Object> trend = adminService.getWeeklyTrend();
        return Result.success(trend);
    }

    /**
     * 获取各话题笔记数量统计
     */
    @GetMapping("/dashboard/topic-stats")
    public Result<Map<String, Object>> getTopicStats() {
        Map<String, Object> stats = adminService.getTopicStats();
        return Result.success(stats);
    }

}
