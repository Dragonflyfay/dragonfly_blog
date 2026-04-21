package com.dragonfly.pojo;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.time.LocalDateTime;
@Data

public class Category {
    private Integer id;
    @NotEmpty
    private String categoryName;
    @NotEmpty
    private String categoryAlias;
    private String categoryDesc;
    private String categoryIcon;
    private Integer sortOrder;
    private Integer articlesCount;
    private Integer createUser;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}