package com.dragonfly.controller;

import com.dragonfly.pojo.Result;
import com.dragonfly.service.LikeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 描述:点赞控制器
 *
 * @param
 * @author 蜻蜓大王
 * @date 2026/5/9 21:30
 */
@RestController
@RequestMapping("/like")
public class LikeController {
    @Autowired
    private LikeService likeService;

    // 批量检查是否点赞笔记（必须放在 /note/{noteId} 之前）
    @PostMapping("/note/batch-check")
    public Result<Map<Integer, Boolean>> batchCheckLikedNotes(@RequestBody List<Integer> noteIds) {
        Map<Integer, Boolean> likedMap = likeService.batchCheckLikedNotes(noteIds);
        return Result.success(likedMap);
    }

    // 批量检查是否点赞评论（必须放在 /comment/{commentId} 之前）
    @PostMapping("/comment/batch-check")
    public Result<Map<Integer, Boolean>> batchCheckLikedComments(@RequestBody List<Integer> commentIds) {
        Map<Integer, Boolean> likedMap = likeService.batchCheckLikedComments(commentIds);
        return Result.success(likedMap);
    }

    // 点赞笔记
    @PostMapping("/note/{noteId}")
    public Result likeNote(@PathVariable Integer noteId) {
        likeService.likeNote(noteId);
        return Result.success();
    }

    // 取消点赞笔记
    @DeleteMapping("/note/{noteId}")
    public Result unlikeNote(@PathVariable Integer noteId) {
        likeService.unlikeNote(noteId);
        return Result.success();
    }

    // 检查是否点赞笔记
    @GetMapping("/note/{noteId}/check")
    public Result<Boolean> isLikedNote(@PathVariable Integer noteId) {
        boolean isLiked = likeService.isLikedNote(noteId);
        return Result.success(isLiked);
    }

    // 点赞评论
    @PostMapping("/comment/{commentId}")
    public Result likeComment(@PathVariable Integer commentId) {
        likeService.likeComment(commentId);
        return Result.success();
    }

    // 取消点赞评论
    @DeleteMapping("/comment/{commentId}")
    public Result unlikeComment(@PathVariable Integer commentId) {
        likeService.unlikeComment(commentId);
        return Result.success();
    }

    // 检查是否点赞评论
    @GetMapping("/comment/{commentId}/check")
    public Result<Boolean> isLikedComment(@PathVariable Integer commentId) {
        boolean isLiked = likeService.isLikedComment(commentId);
        return Result.success(isLiked);
    }
}
