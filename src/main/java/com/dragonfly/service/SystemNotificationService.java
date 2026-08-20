package com.dragonfly.service;

import com.dragonfly.pojo.Notification;

import java.util.List;

/**
 * 描述：
 *
 * @param
 * @author 蜻蜓大王
 * @date 2026/8/20 15:20
 */
public interface SystemNotificationService {

    /**
     * 获取所有系统通知
     */
    List<Notification> getSystemNotifications();

    /**
     * 发送系统通知
     */
    void sendSystemNotification(Notification notification);

    /**
     * 更新系统通知
     */
    void updateSystemNotification(Notification notification);

    /**
     * 删除系统通知
     */
    void deleteSystemNotification(Integer id);

    /**
     * 切换通知状态
     */
    void toggleStatus(Integer id, String status);

}
