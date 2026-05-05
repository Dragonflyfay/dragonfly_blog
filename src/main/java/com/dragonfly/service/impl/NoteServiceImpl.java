package com.dragonfly.service.impl;

import com.dragonfly.mapper.NoteMapper;
import com.dragonfly.pojo.Note;
import com.dragonfly.pojo.PageBean;
import com.dragonfly.service.NoteService;
import com.dragonfly.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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
        // 补充属性值
        note.setCreateTime(LocalDateTime.now());
        note.setUpdateTime(LocalDateTime.now());

        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("id");
        note.setCreateUser(userId);

        noteMapper.add(note);

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
    public PageBean<Note> pageList(Integer pageNum, Integer pageSize) {
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("id");

        // 计算偏移量
        Integer offset = (pageNum - 1) * pageSize;

        // 查询数据列表
        List<Note> notes = noteMapper.listByPage(userId, offset, pageSize);

        // 查询总数
        int total = noteMapper.countByUserId(userId);

        // 封装分页结果
        PageBean<Note> pageBean = new PageBean<>();
        pageBean.setTotal((long) total);
        pageBean.setItems(notes);

        return pageBean;
    }
}
