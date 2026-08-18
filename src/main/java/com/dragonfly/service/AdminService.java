package com.dragonfly.service;

import com.dragonfly.vo.AdminStatsVO;

import java.util.Map;

/**
 * 描述：
 *
 * @param
 * @author 蜻蜓大王
 * @date 2026/8/18 20:40
 */

public interface AdminService {
    /**
     * 获取仪表盘统计数据
     */
    Map<String, Object> getDashboardStats();

    /**
     * 获取最近7天每日新增数据
     */
    Map<String, Object> getWeeklyTrend();

    /**
     * 获取各话题笔记数量统计
     */
    Map<String, Object> getTopicStats();
}
