package com.dragonfly.pojo;

import lombok.Data;

import java.time.LocalDateTime;
@Data

public class Tag {
    private Integer id;
    private String tagName;
    private String tagColor;
    private Integer articlesCount;
    private LocalDateTime createTime;
}