package com.dragonfly.service;

/**
 * 描述：浏览记录服务接口
 *
 * @param
 * @author 蜻蜓大王
 * @date 2026/5/9 21:40
 */
public interface ViewService {
    // 记录浏览
    void recordView(Integer noteId);
    
    // 获取笔记的浏览次数
    int getViewCount(Integer noteId);
}
