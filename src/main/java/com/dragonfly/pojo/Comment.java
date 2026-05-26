package com.dragonfly.pojo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
@Data

public class Comment {
    private Integer id;
    private Integer noteId;
    private Integer userId;
    private Integer parentId;
    private Integer replyToUserId;
    private String content;
    private Integer likesCount;
    private Integer status;//1:正常，0:删除
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    // 关联查询字段
    private String username;
    private String nickname;
    private String userPic;//用户头像
    private String noteTitle;
    // 回复目标用户昵称
    private String replyToUserName;
    // 子评论列表（树形结构）
    // 子评论列表
    private List<Comment> children = new ArrayList<>();
}