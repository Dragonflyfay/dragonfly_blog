package com.dragonfly.pojo;

import com.dragonfly.anno.State;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDateTime;
@Data

public class Article {
    private Integer id;
    @NotEmpty
    @Pattern(regexp="^.{1,100}$")
    private String title;
    private String summary;
    @NotEmpty
    private String content;
    @NotEmpty
    @URL
    private String coverImg;
    private String videoUrl;
    @State
    private String state;
    private Integer isTop;
    private Integer isOriginal;
    private String sourceUrl;
    private Integer viewsCount;
    private Integer likesCount;
    private Integer commentsCount;
    private Integer favoritesCount;
    @NotNull
    private Integer categoryId;
    private Integer createUser;
    private LocalDateTime publishTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}