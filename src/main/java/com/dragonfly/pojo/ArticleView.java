package com.dragonfly.pojo;

import lombok.Data;

import java.time.LocalDateTime;
@Data

public class ArticleView {
    private Integer id;
    private Integer articleId;
    private Integer userId;
    private String ip;
    private String userAgent;
    private LocalDateTime viewTime;
}