package com.dragonfly.service.impl;

import com.dragonfly.mapper.NotificationMapper;
import com.dragonfly.pojo.Notification;
import com.dragonfly.service.SystemNotificationService;
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
 * @date 2026/8/20 15:21
 */
@Service
public class SystemNotificationServiceImpl implements SystemNotificationService {
    @Autowired
    private NotificationMapper notificationMapper;

    @Override
    public List<Notification> getSystemNotifications() {
        return notificationMapper.findSystemNotifications();
    }

    @Override
    @Transactional
    public void sendSystemNotification(Notification notification) {
        notification.setType(5); // 系统通知类型
        notification.setIsRead(0);
        notification.setCreateTime(LocalDateTime.now());
        // 系统通知不使用 note/comment 的 target_type/target_id
        notification.setTargetType(null);
        notification.setTargetId(null);
        if (notification.getPriority() == null || notification.getPriority().isEmpty()) {
            notification.setPriority("normal");
        }
        if (notification.getStatus() == null || notification.getStatus().isEmpty()) {
            notification.setStatus("published");
        }
        notificationMapper.insert(notification);
    }

    @Override
    @Transactional
    public void updateSystemNotification(Notification notification) {
        notification.setTargetType(null);
        notification.setTargetId(null);
        if (notification.getPriority() == null || notification.getPriority().isEmpty()) {
            notification.setPriority("normal");
        }
        notificationMapper.update(notification);
    }

    @Override
    @Transactional
    public void deleteSystemNotification(Integer id) {
        notificationMapper.deleteById(id, null);

    }

    @Override
    @Transactional
    public void toggleStatus(Integer id, String status) {
        notificationMapper.updateStatus(id, status);

    }
}
