package com.dragonfly.service;

import com.dragonfly.pojo.Note;
import com.dragonfly.pojo.PageBean;

import java.util.List;

/**
 * 描述：笔记服务接口
 *
 * @param
 * @author 蜻蜓大王
 * @date 2026/5/5 14:08
 */
public interface NoteService {

    // 新增笔记
    void add(Note note);

    // 查询笔记列表
    List<Note> list();

    // 根据id查询笔记详情
    Note detail(Integer id);

    // 更新笔记
    void update(Note note);

    // 删除笔记
    void delete(Integer id);

    // 分页查询笔记（可选 topicId、state）
    PageBean<Note> pageList(Integer pageNum, Integer pageSize, Integer topicId, String state);
}
