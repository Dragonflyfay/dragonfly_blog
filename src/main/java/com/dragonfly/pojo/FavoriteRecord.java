package com.dragonfly.pojo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 描述：收藏记录实体类
 *
 * @param
 * @author 蜻蜓大王
 * @date 2026/5/9 22:00
 */
@Data
public class FavoriteRecord {
    private Integer id;
    private Integer userId;
    private Integer targetType; // 1:笔记, 2:评论
    private Integer targetId;
    private LocalDateTime createTime;
}
