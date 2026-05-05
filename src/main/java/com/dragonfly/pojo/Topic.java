package com.dragonfly.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 描述：话题实体类
 *
 * @param
 * @author 蜻蜓大王
 * @date 2026/5/5 14:08
 */
@Data
public class Topic {

    @NotNull(groups = Update.class)
    private Integer id;

    @NotEmpty
    private String topicName;

    private Integer notesCount;

    private String coverImg;

    private String description;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    // 校验分组
    public interface Add extends jakarta.validation.groups.Default {
    }

    public interface Update extends jakarta.validation.groups.Default {
    }
}
