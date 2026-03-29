package com.dragonfly.pojo;

import lombok.Data;

import java.time.LocalDateTime;
@Data

public class ArticleTag {
    private Integer id;
    private Integer articleId;
    private Integer tagId;
    private LocalDateTime createTime;
}