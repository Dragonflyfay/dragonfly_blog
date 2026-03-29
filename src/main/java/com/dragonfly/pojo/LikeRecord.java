package com.dragonfly.pojo;

import lombok.Data;

import java.time.LocalDateTime;
@Data

public class LikeRecord {
    private Integer id;
    private Integer userId;
    private Integer targetType;
    private Integer targetId;
    private LocalDateTime createTime;
}