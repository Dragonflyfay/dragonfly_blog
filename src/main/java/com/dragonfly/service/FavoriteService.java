package com.dragonfly.service;

/**
 * 描述：收藏服务接口
 *
 * @param
 * @author 蜻蜓大王
 * @date 2026/5/9 22:00
 */
public interface FavoriteService {
    void favoriteNote(Integer noteId); // 收藏笔记
    void unfavoriteNote(Integer noteId); // 取消收藏笔记
    boolean isFavoritedNote(Integer noteId); // 判断用户是否收藏笔记
}
