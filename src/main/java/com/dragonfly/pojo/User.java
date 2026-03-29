package com.dragonfly.pojo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
@Data

public class User {
    //lombok   在编译阶段，为实体类自动生成构造方法、getter、setter、toString、equals、hashCode方法
    private Integer id;
    private String username;
    private String password;
    private String nickname;
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
