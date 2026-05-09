package com.dragonfly.pojo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 描述：浏览记录实体类
 *
 * @param
 * @author 蜻蜓大王
 * @date 2026/5/9 16:59
 */
@Data
public class ViewRecord {
    private Integer id;
    private Integer userId;
    private Integer noteId;
    private LocalDateTime viewTime;
    private String ip;
    private String userAgent;
}
