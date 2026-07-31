package com.dragonfly.service;

import com.dragonfly.pojo.Notification;
import com.dragonfly.pojo.PageBean;

import java.util.List;

/**
 * 描述：
 *
 * @param
 * @author 蜻蜓大王
 * @date 2026/7/30 11:24
 */
public interface NotificationService {
    // 发送通知
    void sendNotification(Notification notification);

    // 批量发送通知
    void sendBatchNotifications(List<Notification> notifications);

    // 获取用户通知列表（分页）
    PageBean<Notification> getUserNotifications(Integer userId, Integer pageNum, Integer pageSize);

    // 获取未读通知数
    int getUnreadCount(Integer userId);

    // 标记为已读
    void markAsRead(Integer id, Integer userId);

    // 全部标记为已读
    void markAllAsRead(Integer userId);

    // 删除通知
    void deleteNotification(Integer id, Integer userId);

    // 删除所有已读通知
    void deleteAllRead(Integer userId);

    // 获取最近未读通知（用于红点提示）
    List<Notification> getRecentUnread(Integer userId);

}
