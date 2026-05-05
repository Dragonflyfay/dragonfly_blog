package com.dragonfly.pojo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.dragonfly.anno.State;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 描述：笔记实体类
 *
 * @param
 * @author 蜻蜓大王
 * @date 2026/5/5 16：25
 */
@Data
public class Note {

    private Integer id;

    @NotEmpty
    private String title;

    @NotEmpty
    private String content;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> images; // 改为List<String>类型，使用JacksonTypeHandler处理JSON


    @NotNull
    private Integer topicId; // 对应数据库的 topic_id

    private String location;
    private String coverImg;


    @State
    private String state;

    private Integer viewsCount;
    private Integer likesCount;
    private Integer commentsCount;
    private Integer favoritesCount;

    private Integer createUser;
    private LocalDateTime publishTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

}
