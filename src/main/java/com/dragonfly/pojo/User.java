package com.dragonfly.pojo;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import net.minidev.json.annotate.JsonIgnore;

import java.time.LocalDate;
import java.time.LocalDateTime;
@Data

public class User {
    //lombok   在编译阶段，为实体类自动生成构造方法、getter、setter、toString、equals、hashCode方法
    @NotNull
    private Integer id;
    private String username;
    @JsonIgnore//忽略字段,让SpringMVC把对象转成json字符串的时候，忽略password，最终的json字符串中就没有password字段了
    private String password;

    @NotEmpty
    @Pattern(regexp = "^\\S{1,10}$")
    private String nickname;

    @NotNull
    @Email
    private String email;
    private String userPic;
    private String bio;
    private Integer gender;
    private LocalDate birthday;
    private String location;
    private Integer followersCount;
    private Integer followingCount;
    private Integer articlesCount;
    private Integer level;
    private Integer status;
    private String lastLoginIp;
    private LocalDateTime lastLoginTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

}
