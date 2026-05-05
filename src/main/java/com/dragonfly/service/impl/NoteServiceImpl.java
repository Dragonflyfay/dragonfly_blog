package com.dragonfly.service.impl;

import com.dragonfly.mapper.NoteMapper;
import com.dragonfly.pojo.Note;
import com.dragonfly.pojo.PageBean;
import com.dragonfly.service.NoteService;
import com.dragonfly.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * 描述：笔记实现类
 *
 * @param
 * @author 蜻蜓大王
 * @date 2026/5/5 15:25
 */
@Service
public class NoteServiceImpl implements NoteService {
    @Autowired
    private NoteMapper noteMapper;

    @Override
    public void add(Note note) {
        note.setCreateTime(LocalDateTime.now());
        note.setUpdateTime(LocalDateTime.now());

        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("id");
        note.setCreateUser(userId);

        if ("已发布".equals(note.getState())) {
            note.setPublishTime(LocalDateTime.now());
        }
        syncCoverIntoImages(note);
        if (note.getViewsCount() == null) {
            note.setViewsCount(0);
        }
        if (note.getLikesCount() == null) {
            note.setLikesCount(0);
        }
        if (note.getCommentsCount() == null) {
            note.setCommentsCount(0);
        }
        if (note.getFavoritesCount() == null) {
            note.setFavoritesCount(0);
        }

        noteMapper.add(note);

    }

    /** 封面优先写入图片列表首位，便于前端「小红书」封面展示 */
    private void syncCoverIntoImages(Note note) {
        if (note.getCoverImg() == null || note.getCoverImg().isEmpty()) {
            return;
        }
        List<String> imgs = note.getImages();
        if (imgs == null) {
            imgs = new ArrayList<>();
        }
        LinkedHashSet<String> ordered = new LinkedHashSet<>();
        ordered.add(note.getCoverImg());
        ordered.addAll(imgs);
        note.setImages(new ArrayList<>(ordered));
    }

    @Override
    public List<Note> list() {
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("id");
        return noteMapper.list(userId);
    }

    @Override
    public Note detail(Integer id) {
        return noteMapper.findById(id);
    }

    @Override
    public void update(Note note) {
        note.setUpdateTime(LocalDateTime.now());
        if ("已发布".equals(note.getState()) && note.getPublishTime() == null) {
            note.setPublishTime(LocalDateTime.now());
        }
        syncCoverIntoImages(note);
        noteMapper.update(note);
    }

    @Override
    public void delete(Integer id) {
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("id");

        // 验证笔记是否存在及权限
        Note note = noteMapper.findById(id);
        if (note == null) {
            throw new RuntimeException("要删除的笔记不存在");
        }
        if (!note.getCreateUser().equals(userId)) {
            throw new RuntimeException("没有权限，只能删除自己创建的笔记");
        }

        noteMapper.delete(id, userId);
    }

    @Override
    public PageBean<Note> pageList(Integer pageNum, Integer pageSize, Integer topicId, String state) {
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("id");

        Integer offset = (pageNum - 1) * pageSize;

        List<Note> notes = noteMapper.listByPage(userId, topicId, state, offset, pageSize);

        int total = noteMapper.countByFilters(userId, topicId, state);

        PageBean<Note> pageBean = new PageBean<>();
        pageBean.setTotal((long) total);
        pageBean.setItems(notes);

        return pageBean;
    }
}
