package com.dragonfly.controller;

import com.dragonfly.pojo.Notification;
import com.dragonfly.pojo.PageBean;
import com.dragonfly.pojo.Result;
import com.dragonfly.service.NotificationService;
import com.dragonfly.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 描述：
 *
 * @param
 * @author 蜻蜓大王
 * @date 2026/7/31 19:47
 */
@RestController
@RequestMapping("/notification")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    //获取当前用户的通知列表
    @GetMapping("/list")
    public Result<PageBean<Notification>> list(
            @RequestParam(defaultValue="1")Integer pageNum,
            @RequestParam(defaultValue="10")Integer pageSize
    ){
        Map<String,Object> map= ThreadLocalUtil.get();
        Integer userId=(Integer) map.get("id");
        PageBean<Notification> pageBean=notificationService.getUserNotifications(userId,pageNum,pageSize);
        return Result.success(pageBean);
    }
    // 获取未读通知数
    @GetMapping("/unread/count")
    public Result<Integer> getUnreadCount() {
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("id");
        int count = notificationService.getUnreadCount(userId);
        return Result.success(count);
    }
    // 获取最近未读通知（用于红点提示）
    @GetMapping("/unread/recent")
    public Result<List<Notification>> getRecentUnread() {
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("id");
        List<Notification> notifications = notificationService.getRecentUnread(userId);
        return Result.success(notifications);
    }
    // 标记单条通知为已读
    @PutMapping("/read/{id}")
    public Result markAsRead(@PathVariable Integer id) {
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("id");
        notificationService.markAsRead(id, userId);
        return Result.success();
    }
    // 全部标记为已读
    @PutMapping("/read/all")
    public Result markAllAsRead() {
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("id");
        notificationService.markAllAsRead(userId);
        return Result.success();
    }
    // 删除通知
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("id");
        notificationService.deleteNotification(id, userId);
        return Result.success();
    }
    // 删除所有已读通知
    @DeleteMapping("/read/all")
    public Result deleteAllRead() {
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("id");
        notificationService.deleteAllRead(userId);
        return Result.success();
    }
}
