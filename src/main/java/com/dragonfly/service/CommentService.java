package com.dragonfly.service;

import com.dragonfly.pojo.Comment;
import com.dragonfly.pojo.PageBean;

import java.util.List;

/**
 * 描述：评论service类
 *
 * @param
 * @author 蜻蜓大王
 * @date 2026/5/9 17:03
 */

public interface CommentService {

    void add(Comment comment);

    List<Comment> listByNoteId(Integer noteId);

    void update(Comment comment);

    void delete(Integer id);

    void like(Integer id);

    void unlike(Integer id);

    // 管理员分页查询评论
    PageBean<Comment> pageList(Integer pageNum, Integer pageSize, String keyword);
}
