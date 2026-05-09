package com.dragonfly.service.impl;

import com.dragonfly.mapper.NoteMapper;
import com.dragonfly.mapper.ViewRecordMapper;
import com.dragonfly.pojo.Note;
import com.dragonfly.pojo.ViewRecord;
import com.dragonfly.service.ViewService;
import com.dragonfly.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 描述：浏览记录服务实现类
 *
 * @param
 * @author 蜻蜓大王
 * @date 2026/5/9 21:40
 */
@Service
public class ViewServiceImpl implements ViewService {
    @Autowired
    private ViewRecordMapper viewRecordMapper;
    @Autowired
    private NoteMapper noteMapper;

    @Override
    @Transactional
    public void recordView(Integer noteId) {
        Map<String, Object> map = ThreadLocalUtil.get();
        if (map == null) {
            // 未登录用户，不记录浏览
            return;
        }
        
        Integer userId = (Integer) map.get("id");

        // 检查是否已经浏览过（避免重复计数）
        ViewRecord latestView = viewRecordMapper.findLatestByNoteAndUser(noteId, userId);
        
        // 如果从未浏览过，或者上次浏览时间超过1小时，则记录新的浏览
        boolean shouldRecord = false;
        if (latestView == null) {
            shouldRecord = true;
        } else {
            // 检查上次浏览时间是否超过1小时
            LocalDateTime lastViewTime = latestView.getViewTime();
            LocalDateTime now = LocalDateTime.now();
            if (lastViewTime != null && lastViewTime.plusHours(1).isBefore(now)) {
                shouldRecord = true;
            }
        }

        if (shouldRecord) {
            // 创建浏览记录
            ViewRecord viewRecord = new ViewRecord();
            viewRecord.setNoteId(noteId);
            viewRecord.setUserId(userId);
            viewRecord.setViewTime(LocalDateTime.now());
            viewRecordMapper.add(viewRecord);

            // 更新笔记的浏览次数
            noteMapper.incrementViewsCount(noteId);
        }
    }

    @Override
    public int getViewCount(Integer noteId) {
        return viewRecordMapper.countUniqueViewsByNoteId(noteId);
    }
}
