package com.dragonfly.pojo;

import lombok.Data;

import java.time.LocalDateTime;
@Data

public class Comment {
    private Integer id;
    private Integer noteId;
    private Integer userId;
    private Integer parentId;
    private Integer replyToUserId;
    private String content;
    private Integer likesCount;
    private Integer status;//1:正常，2:删除
    private LocalDateTime createTime;
}