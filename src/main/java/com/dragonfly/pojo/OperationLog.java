package com.dragonfly.pojo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OperationLog {
    private Integer id;
    private String operator;
    private String module;
    private String action;
    private String detail;
    private String ip;
    private LocalDateTime createTime;
}