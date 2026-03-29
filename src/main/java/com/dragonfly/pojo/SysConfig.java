package com.dragonfly.pojo;

import lombok.Data;

import java.time.LocalDateTime;
@Data

public class SysConfig {
    private Integer id;
    private String configKey;
    private String configValue;
    private String configDesc;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}