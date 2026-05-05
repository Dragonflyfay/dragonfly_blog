package com.dragonfly.pojo;

import com.dragonfly.anno.State;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 描述：笔记实体类
 *
 * @param
 * @author 蜻蜓大王
 * @date 2026/5/5 14:08
 */
@Data
public class Note {

    private Integer id;

    @NotEmpty
    private String title;

    @NotEmpty
    private String content;

    private String images; // JSON 字符串存储图片数组

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
