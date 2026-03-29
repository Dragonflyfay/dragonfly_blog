package com.dragonfly.pojo;

import lombok.Data;

import java.time.LocalDateTime;
@Data

public class Comment {
    private Integer id;
    private Integer articleId;
    private Integer userId;
    private Integer parentId;
    private Integer replyToUserId;
    private String content;
    private Integer likesCount;
    private Integer status;
    private LocalDateTime createTime;
}