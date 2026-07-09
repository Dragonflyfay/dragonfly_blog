package com.dragonfly.controller;

import com.dragonfly.pojo.Result;
import com.dragonfly.service.FavoriteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 描述：收藏控制器
 *
 * @param
 * @author 蜻蜓大王
 * @date 2026/5/9 22:00
 */
@RestController
@RequestMapping("/favorite")
public class FavoriteController {
    @Autowired
    private FavoriteService favoriteService;

    // 收藏笔记
    @PostMapping("/note/{noteId}")
    public Result favoriteNote(@PathVariable Integer noteId) {
        favoriteService.favoriteNote(noteId);
        return Result.success();
    }

    // 取消收藏笔记
    @DeleteMapping("/note/{noteId}")
    public Result unfavoriteNote(@PathVariable Integer noteId) {
        favoriteService.unfavoriteNote(noteId);
        return Result.success();
    }

    // 检查是否收藏笔记
    @GetMapping("/note/{noteId}/check")
    public Result<Boolean> isFavoritedNote(@PathVariable Integer noteId) {
        boolean isFavorited = favoriteService.isFavoritedNote(noteId);
        return Result.success(isFavorited);
    }
    // 批量检查是否收藏笔记
    @PostMapping("/notes/batch-check")
    public Result<Map<Integer, Boolean>> batchCheckFavorited(@RequestBody List<Integer> noteIds) {
        Map<Integer, Boolean> result = favoriteService.batchCheckFavoritedNotes(noteIds);
        return Result.success(result);
    }

    // 获取当前用户收藏的笔记ID列表
    @GetMapping("/notes")
    public Result<List<Integer>> getFavoriteNoteIds() {
        List<Integer> noteIds = favoriteService.getFavoriteNoteIds();
        return Result.success(noteIds);
    }
}
