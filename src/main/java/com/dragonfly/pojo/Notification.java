package com.dragonfly.pojo;

import lombok.Data;

import java.time.LocalDateTime;
@Data

public class Notification {
    private Integer id;
    private Integer userId;
    private Integer fromUserId;
    private Integer type;
    private Integer targetType;
    private Integer targetId;
    private String content;
    private Integer isRead;
    private LocalDateTime createTime;
}