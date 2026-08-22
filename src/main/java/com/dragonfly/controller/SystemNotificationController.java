package com.dragonfly.controller;
import com.dragonfly.pojo.Notification;
import com.dragonfly.pojo.Result;
import com.dragonfly.service.SystemNotificationService;
import com.dragonfly.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
/**
 * 描述：系统通知管理
 *
 * @param
 * @author 蜻蜓大王
 * @date 2026/8/20 15:18
 */
@RestController
@RequestMapping("/admin/notifications")
public class SystemNotificationController {
    @Autowired
    private SystemNotificationService notificationService;

    /**
     * 获取系统通知列表
     */
    @GetMapping
    public Result<List<Notification>> list() {
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("id");
        List<Notification> notifications = notificationService.getSystemNotifications();
        return Result.success(notifications);
    }

    /**
     * 发送系统通知
     */
    @PostMapping
    public Result send(@RequestBody Notification notification) {
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("id");
        notification.setFromUserId(userId);
        notificationService.sendSystemNotification(notification);
        return Result.success();
    }

    /**
     * 更新系统通知
     */
    @PutMapping
    public Result update(@RequestBody Notification notification) {
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("id");
        notification.setFromUserId(userId);
        notificationService.updateSystemNotification(notification);
        return Result.success();
    }

    /**
     * 删除系统通知
     */
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        notificationService.deleteSystemNotification(id);
        return Result.success();
    }

    /**
     * 切换通知状态（发布/撤回）
     */
    @PutMapping("/{id}/status")
    public Result toggleStatus(@PathVariable Integer id, @RequestBody Map<String, String> params) {
        String status = params.get("status");
        notificationService.toggleStatus(id, status);
        return Result.success();
    }
}
