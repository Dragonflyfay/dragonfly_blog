package com.dragonfly.pojo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.dragonfly.anno.State;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class Note {

    private Integer id;//

    @NotEmpty
    private String title;//

    @NotEmpty
    private String content;//

    // 媒体类型: image 或 video
    private String noteCategory;//

    // 图片列表 (图文笔记使用)
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> images;//

    // 视频URL (视频笔记使用)
    private String video;//


    @NotNull
    private Integer topicId;//

    private String location;//
    private String coverImg;//

    @State
    private String state;//

    private Integer viewsCount;//
    private Integer likesCount;//
    private Integer commentsCount;//
    private Integer favoritesCount;//

    private Integer createUser;//
    private LocalDateTime publishTime;//
    private LocalDateTime createTime;//
    private LocalDateTime updateTime;//
}