package com.dragonfly.service.impl;

import com.dragonfly.mapper.NotificationMapper;
import com.dragonfly.pojo.Notification;
import com.dragonfly.pojo.PageBean;
import com.dragonfly.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 描述：
 *
 * @param
 * @author 蜻蜓大王
 * @date 2026/7/30 11:26
 */
@Service
public class NotificationServiceImpl implements NotificationService {
    @Autowired
    private NotificationMapper notificationMapper;
    @Override
    @Transactional
    public void sendNotification(Notification notification) {
        //防止给自己发通知
        if (notification.getUserId().equals(notification.getFromUserId())) {
            return;
        }
        notification.setIsRead(0);
        notification.setCreateTime(LocalDateTime.now());
        notificationMapper.insert(notification);

    }

    @Override
    @Transactional
    public void sendBatchNotifications(List<Notification> notifications) {
        if (notifications == null || notifications.isEmpty()) {
            return;
        }
        // 过滤掉给自己发通知的情况
        notifications.removeIf(n -> n.getUserId().equals(n.getFromUserId()));// 使用lambda表达式
        for (Notification n : notifications) {
            n.setIsRead(0);
            n.setCreateTime(LocalDateTime.now());
        }
        notificationMapper.batchInsert(notifications);
    }

    @Override
    public PageBean<Notification> getUserNotifications(Integer userId, Integer pageNum, Integer pageSize) {
        int offset = (pageNum - 1) * pageSize;
        List<Notification> items = notificationMapper.findByUserId(userId, offset, pageSize);
        int total = notificationMapper.countByUserId(userId);
        PageBean<Notification> pageBean = new PageBean<>();
        pageBean.setTotal((long) total);
        pageBean.setItems(items);
        return pageBean;
    }

    @Override
    public int getUnreadCount(Integer userId) {
        return notificationMapper.countUnreadByUserId(userId);
    }

    @Override
    @Transactional
    public void markAsRead(Integer id, Integer userId) {
        notificationMapper.markAsRead(id, userId);
    }

    @Override
    @Transactional
    public void markAllAsRead(Integer userId) {
        notificationMapper.markAllAsRead(userId);

    }

    @Override
    @Transactional
    public void deleteNotification(Integer id, Integer userId) {
        notificationMapper.deleteById(id, userId);
    }

    @Override
    @Transactional
    public void deleteAllRead(Integer userId) {
        notificationMapper.deleteAllRead(userId);
    }

    @Override
    public List<Notification> getRecentUnread(Integer userId) {
        return notificationMapper.findRecentUnread(userId);
    }
}
