package com.dragonfly.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 描述：管理后台统计数据
 *
 * @param
 * @author 蜻蜓大王
 * @date 2026/8/18 20:42
 */
@Data
public class AdminStatsVO {
    //总览数据
    // 总览数据
    private Long totalUsers;
    private Long totalNotes;
    private Long totalComments;
    private Long totalViews;
    private Long todayNewUsers;
    private Long todayNewNotes;
    private Long todayNewComments;

    // 笔记状态统计
    private Long publishedNotes;
    private Long draftNotes;
    private Long archivedNotes;

    // 用户角色统计
    private Long adminCount;
    private Long userCount;
    private Long superAdminCount;

    // 近期数据
    private List<Map<String, Object>> recentNotes;
    private List<Map<String, Object>> recentComments;
    private List<Map<String, Object>> recentUsers;
}
