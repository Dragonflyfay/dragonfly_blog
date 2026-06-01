package com.dragonfly.pojo;

import lombok.Data;

import java.time.LocalDateTime;
@Data

public class Follow {
    private Integer id;
    private Integer followerId;
    private Integer followingId;
    private Integer status;       // 1-正常 0-已取消
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}