package com.dragonfly.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
@Data  // 自动生成getter和setter方法

public class Notification {
    private Integer id;
    private Integer userId;//接收通知的用户id
    private Integer fromUserId;//触发通知的用户id
    private Integer type;//
    private Integer targetType;
    private Integer targetId;//目标ID(评论/笔记)
    private String content;//内容
    private Integer isRead = 0;//是否已读，默认未读

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    // 扩展字段（非数据库字段）
    private String fromUserName;     // 触发者昵称
    private String fromUserPic;      // 触发者头像
    private String targetTitle;      // 目标标题（笔记标题）

}