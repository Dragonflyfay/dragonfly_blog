package com.dragonfly.service;

import java.util.List;
import java.util.Map;

/**
 * 描述：点赞 service
 *
 * @param
 * @author 蜻蜓大王
 * @date 2026/5/9 20:57
 */
public interface LikeService {
    void likeNote(Integer noteId);// 点赞笔记
    void unlikeNote(Integer noteId);// 取消点赞笔记
    boolean isLikedNote(Integer commentId);  // 判断用户是否点赞笔记
    void unlikeComment(Integer commentId);// 取消点赞评论
    void likeComment(Integer commentId);// 点赞评论
    boolean isLikedComment(Integer commentId);// 判断用户是否点赞评论
    
    // 批量检查是否点赞笔记
    Map<Integer, Boolean> batchCheckLikedNotes(List<Integer> noteIds);
    
    // 批量检查是否点赞评论
    Map<Integer, Boolean> batchCheckLikedComments(List<Integer> commentIds);
}
