package com.dragonfly.pojo;

import lombok.Data;

import java.time.LocalDateTime;
@Data

public class Category {
    private Integer id;
    private String categoryName;
    private String categoryAlias;
    private String categoryDesc;
    private String categoryIcon;
    private Integer sortOrder;
    private Integer articlesCount;
    private Integer createUser;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}