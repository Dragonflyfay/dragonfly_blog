package com.dragonfly.pojo;

import lombok.Data;

import java.time.LocalDateTime;
@Data

public class Article {
    private Integer id;
    private String title;
    private String summary;
    private String content;
    private String coverImg;
    private String videoUrl;
    private String state;
    private Integer isTop;
    private Integer isOriginal;
    private String sourceUrl;
    private Integer viewsCount;
    private Integer likesCount;
    private Integer commentsCount;
    private Integer favoritesCount;
    private Integer categoryId;
    private Integer createUser;
    private LocalDateTime publishTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}