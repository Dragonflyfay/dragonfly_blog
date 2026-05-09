package com.dragonfly.controller;

import com.dragonfly.pojo.Note;
import com.dragonfly.pojo.PageBean;
import com.dragonfly.pojo.Result;
import com.dragonfly.service.NoteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 描述：笔记控制器
 *
 * @param
 * @author 蜻蜓大王
 * @date 2026/5/5 14:08
 */
@RestController
@RequestMapping("/note")
public class NoteController {

    @Autowired
    private NoteService noteService;

    @PostMapping
    public Result add(@RequestBody @Validated Note note) {
        noteService.add(note);
        return Result.success();
    }

    @GetMapping
    public Result<List<Note>> list() {
        List<Note> notes = noteService.list();
        return Result.success(notes);
    }

    // 获取笔记详情
    @GetMapping("/detail")
    public Result<Note> detail(Integer id) {
        Note note = noteService.detail(id);
        return Result.success(note);
    }

    @PutMapping
    public Result update(@RequestBody @Validated Note note) {
        noteService.update(note);
        return Result.success();
    }

    @DeleteMapping
    public Result delete(Integer id) {
        noteService.delete(id);
        return Result.success();
    }

    // 分页查询笔记（可选话题、状态、用户）
    @GetMapping("/page")
    public Result<PageBean<Note>> pageList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer topicId,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) Integer userId) {
        PageBean<Note> pageBean = noteService.pageList(pageNum, pageSize, topicId, state, userId);
        return Result.success(pageBean);
    }

    // 分页查询笔记（可选话题、状态、用户）
    @GetMapping("/user/page")
    public Result<PageBean<Note>> userPageList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "12") Integer pageSize,
            @RequestParam(required = false) Integer topicId,
            @RequestParam(required = false) String keyword
            )
    {

        PageBean<Note> pageBean = noteService.userPageList(pageNum, pageSize, topicId, keyword,null);
        return Result.success(pageBean);
    }
}
