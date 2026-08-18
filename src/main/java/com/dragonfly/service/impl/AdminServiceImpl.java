package com.dragonfly.service.impl;

import com.dragonfly.mapper.*;
import com.dragonfly.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 描述：
 *
 * @param
 * @author 蜻蜓大王
 * @date 2026/8/18 20:44
 */

@Service
public class AdminServiceImpl implements AdminService {

    @Autowired
    private NoteMapper noteMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private TopicMapper topicMapper;

    @Autowired
    private LikeRecordMapper likeRecordMapper;

    @Autowired
    private FavoriteRecordMapper favoriteRecordMapper;

    @Override
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        // 1. 笔记相关统计
        int totalNotes = getTotalNotes();
        int publishedNotes = getPublishedNotes();
        int draftNotes = getDraftNotes();
        int totalViews = getTotalViews();
        int totalLikes = getTotalLikes();
        int totalFavorites = getTotalFavorites();
        int totalComments = getTotalComments();

        stats.put("totalNotes", totalNotes);
        stats.put("publishedNotes", publishedNotes);
        stats.put("draftNotes", draftNotes);
        stats.put("totalViews", totalViews);
        stats.put("totalLikes", totalLikes);
        stats.put("totalFavorites", totalFavorites);
        stats.put("totalComments", totalComments);

        // 2. 用户统计
        int totalUsers = getTotalUsers();
        int todayNewUsers = getTodayNewUsers();
        int activeUsers = getActiveUsers();

        stats.put("totalUsers", totalUsers);
        stats.put("todayNewUsers", todayNewUsers);
        stats.put("activeUsers", activeUsers);

        // 3. 话题统计
        int totalTopics = getTotalTopics();

        stats.put("totalTopics", totalTopics);

        // 4. 今日数据
        int todayNewNotes = getTodayNewNotes();
        int todayComments = getTodayComments();

        stats.put("todayNewNotes", todayNewNotes);
        stats.put("todayComments", todayComments);

        return stats;
    }

    @Override
    public Map<String, Object> getWeeklyTrend() {
        Map<String, Object> result = new HashMap<>();
        List<String> dates = new ArrayList<>();
        List<Integer> notesData = new ArrayList<>();
        List<Integer> usersData = new ArrayList<>();
        List<Integer> commentsData = new ArrayList<>();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");
        LocalDate today = LocalDate.now();

        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            dates.add(date.format(formatter));

            // 查询当天新增笔记数
            int notesCount = countNotesByDate(date);
            notesData.add(notesCount);

            // 查询当天新增用户数
            int usersCount = countUsersByDate(date);
            usersData.add(usersCount);

            // 查询当天新增评论数
            int commentsCount = countCommentsByDate(date);
            commentsData.add(commentsCount);
        }

        result.put("dates", dates);
        result.put("notes", notesData);
        result.put("users", usersData);
        result.put("comments", commentsData);

        return result;
    }

    @Override
    public Map<String, Object> getTopicStats() {
        Map<String, Object> result = new HashMap<>();
        List<String> names = new ArrayList<>();
        List<Integer> counts = new ArrayList<>();

        // 获取所有话题及其笔记数量
        List<com.dragonfly.pojo.Topic> topics = topicMapper.list();
        for (com.dragonfly.pojo.Topic topic : topics) {
            names.add(topic.getTopicName());
            counts.add(topic.getNotesCount() != null ? topic.getNotesCount() : 0);
        }

        result.put("names", names);
        result.put("counts", counts);

        return result;
    }

    // ==================== 私有统计方法 ====================

    private int getTotalNotes() {
        // 统计所有笔记（含草稿）
        return noteMapper.countByFilters(null, null, null);
    }

    private int getPublishedNotes() {
        return noteMapper.countByFilters(null, null, "已发布");
    }

    private int getDraftNotes() {
        return noteMapper.countByFilters(null, null, "草稿");
    }

    private int getTotalViews() {
        // 需要新增 Mapper 方法：SUM(views_count)
        return noteMapper.sumViewsCount();
    }

    private int getTotalLikes() {
        return noteMapper.sumLikesCount();
    }

    private int getTotalFavorites() {
        return noteMapper.sumFavoritesCount();
    }

    private int getTotalComments() {
        return commentMapper.countAll(null);
    }

    private int getTotalUsers() {
        return userMapper.countAll();
    }

    private int getTodayNewUsers() {
        return userMapper.countByDate(LocalDate.now());
    }

    private int getActiveUsers() {
        // 最近7天有登录或操作的用户
        return userMapper.countActiveUsers(7);
    }

    private int getTotalTopics() {
        return topicMapper.countAll();
    }

    private int getTodayNewNotes() {
        return noteMapper.countByDate(LocalDate.now());
    }

    private int getTodayComments() {
        return commentMapper.countByDate(LocalDate.now());
    }

    // ==================== 按日期统计 ====================

    private int countNotesByDate(LocalDate date) {
        return noteMapper.countByDate(date);
    }

    private int countUsersByDate(LocalDate date) {
        return userMapper.countByDate(date);
    }

    private int countCommentsByDate(LocalDate date) {
        return commentMapper.countByDate(date);
    }
}