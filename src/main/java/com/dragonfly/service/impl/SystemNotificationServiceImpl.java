package com.dragonfly.service.impl;

import com.dragonfly.mapper.NotificationMapper;
import com.dragonfly.mapper.UserMapper;
import com.dragonfly.pojo.Notification;
import com.dragonfly.pojo.User;
import com.dragonfly.service.SystemNotificationService;
import com.dragonfly.utils.ThreadLocalUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 描述：
 *
 * @param
 * @author 蜻蜓大王
 * @date 2026/8/20 15:21
 */
@Slf4j
@Service
public class SystemNotificationServiceImpl implements SystemNotificationService {
    @Autowired
    private NotificationMapper notificationMapper;
    @Autowired
    private UserMapper userMapper;

    @Override
    public List<Notification> getSystemNotifications() {
        return notificationMapper.findSystemNotifications();
    }

    @Override
    @Transactional
    public void sendSystemNotification(Notification notification) {
        notification.setType(5);
        notification.setIsRead(0);
        notification.setCreateTime(LocalDateTime.now());
        notification.setTargetType(null);
        notification.setTargetId(null);

        if (notification.getPriority() == null) {
            notification.setPriority("normal");
        }
        if (notification.getStatus() == null) {
            notification.setStatus("published");
        }

        //  获取当前操作的管理员ID
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer adminId = (Integer) map.get("id");

        // 判断是否发送给"全部用户"
        if (notification.getUserId() == null || notification.getUserId() == 0) {
            // ----- 广播：给每个用户插一行 -----
            List<User> allUsers = userMapper.list();

            if (allUsers.isEmpty()) {
                log.warn("没有用户可接收广播通知");
                return;
            }

            // 1️⃣ 先插入一条"母版记录"（用于后台管理展示）
            Notification template = new Notification();
            BeanUtils.copyProperties(notification, template);
            template.setUserId(0);
            template.setBatchId(null);
            template.setIsRead(1); // 母版标记为已读
            notificationMapper.insert(template);

            Integer batchId = template.getId(); // 用母版ID作为batchId

            // 2️⃣ 批量插入所有用户的行
            List<Notification> userNotifications = new ArrayList<>();
            for (User user : allUsers) {
                Notification copy = new Notification();
                BeanUtils.copyProperties(notification, copy);
                copy.setUserId(user.getId());
                copy.setBatchId(batchId);
                copy.setIsRead(0);
                copy.setCreateTime(LocalDateTime.now());
                userNotifications.add(copy);
            }

            notificationMapper.batchInsert(userNotifications);

            // 3️⃣ 回填母版的 batch_id（指向自己）
            notificationMapper.updateBatchId(batchId, batchId);

            log.info("📢 广播通知已发送，共 {} 个用户", allUsers.size());

        } else {
            // ----- 发送给指定用户 -----
            List<User> allUsers = userMapper.list();
            boolean userExists = allUsers.stream()
                    .anyMatch(u -> u.getId().equals(notification.getUserId()));

            if (!userExists) {
                throw new RuntimeException("目标用户不存在: userId=" + notification.getUserId());
            }

            notification.setBatchId(null);
            notificationMapper.insert(notification);
            log.info("📨 通知已发送给用户: {}", notification.getUserId());

            // ✅ 管理员也复制一条（用于在系统通知页面看到）
            if (adminId != null && !adminId.equals(notification.getUserId())) {
                Notification adminCopy = new Notification();
                BeanUtils.copyProperties(notification, adminCopy);
                adminCopy.setUserId(adminId);
                adminCopy.setBatchId(notification.getId());
                adminCopy.setIsRead(1);
                notificationMapper.insert(adminCopy);
                log.info("📨 通知已复制给管理员: {}", adminId);
            }
        }
    }
    @Override
    @Transactional
    public void updateSystemNotification(Notification notification) {
        notification.setTargetType(null);
        notification.setTargetId(null);

        if (notification.getPriority() == null) {
            notification.setPriority("normal");
        }

        Notification existing = notificationMapper.findById(notification.getId());
        if (existing != null && existing.getBatchId() != null) {
            notificationMapper.updateBroadcast(notification);
            log.info("📢 广播通知已更新: batchId={}", existing.getBatchId());
        } else {
            notificationMapper.update(notification);
            log.info("📨 通知已更新: id={}", notification.getId());
        }
    }

    @Override
    @Transactional
    public void deleteSystemNotification(Integer id) {
        int deleted = notificationMapper.deleteSystemNotificationById(id);
        log.info("🗑️ 已删除系统通知: id={}, 影响行数={}", id, deleted);
    }

    @Override
    @Transactional
    public void toggleStatus(Integer id, String status) {
        notificationMapper.updateStatus(id, status);
        log.info("🔄 通知状态已切换: id={}, status={}", id, status);
    }
}
