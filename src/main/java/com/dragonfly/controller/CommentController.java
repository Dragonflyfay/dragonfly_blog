package com.dragonfly.controller;

import com.dragonfly.pojo.Comment;
import com.dragonfly.pojo.PageBean;
import com.dragonfly.pojo.Result;
import com.dragonfly.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 描述：评论controller
 *
 * @param
 * @author 蜻蜓大王
 * @date 2026/5/9 17:02
 */
@RestController
@RequestMapping("/comment")
public class CommentController {
    @Autowired
    private CommentService  commentService;


    // 添加评论
    @PostMapping
    public Result add(@RequestBody Comment comment){
        commentService.add(comment);
        return Result.success();

    }

    // 获取笔记的评论列表
    @GetMapping("/note/{noteId}")
    public Result<List<Comment>> listByNoteId(@PathVariable Integer noteId) {
        List<Comment> comments = commentService.listByNoteId(noteId);
        return Result.success(comments);
    }

    // 更新评论
    @PutMapping
    public Result update(@RequestBody Comment comment) {
        commentService.update(comment);
        return Result.success();
    }

    // 删除评论
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        commentService.delete(id);
        return Result.success();
    }

    // 点赞评论
    @PostMapping("/{id}/like")
    public Result like(@PathVariable Integer id) {
        commentService.like(id);
        return Result.success();
    }

    // 取消点赞评论
    @DeleteMapping("/{id}/like")
    public Result unlike(@PathVariable Integer id) {
        commentService.unlike(id);
        return Result.success();
    }

    // 管理员分页查询所有评论
    @GetMapping("/page")
    public Result<PageBean<Comment>> pageList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword) {
        PageBean<Comment> pageBean = commentService.pageList(pageNum, pageSize, keyword);
        return Result.success(pageBean);
    }
}