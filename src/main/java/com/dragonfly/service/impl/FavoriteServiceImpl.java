package com.dragonfly.service.impl;

import com.dragonfly.mapper.FavoriteRecordMapper;
import com.dragonfly.mapper.NoteMapper;
import com.dragonfly.pojo.FavoriteRecord;
import com.dragonfly.service.FavoriteService;
import com.dragonfly.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 描述：收藏服务实现类
 *
 * @param
 * @author 蜻蜓大王
 * @date 2026/5/9 22:00
 */
@Service
public class FavoriteServiceImpl implements FavoriteService {
    @Autowired
    private NoteMapper noteMapper;
    @Autowired
    private FavoriteRecordMapper favoriteRecordMapper;
    
    @Override
    @Transactional
    public void favoriteNote(Integer noteId) {
        Map<String,Object> map= ThreadLocalUtil.get();
        Integer userId=(Integer) map.get("id");

        // 检查是否已经收藏过
        FavoriteRecord existing=favoriteRecordMapper.findByUserAndTarget(userId,1,noteId);
        if(existing != null) {
            throw new RuntimeException("已经收藏过了");
        }

        // 创建收藏记录
        FavoriteRecord favoriteRecord = new FavoriteRecord();
        favoriteRecord.setUserId(userId);
        favoriteRecord.setTargetType(1); // 1表示笔记
        favoriteRecord.setTargetId(noteId);
        favoriteRecord.setCreateTime(LocalDateTime.now());
        favoriteRecordMapper.add(favoriteRecord);

        // 更新笔记的收藏数
        noteMapper.incrementFavoritesCount(noteId);
    }

    @Override
    @Transactional
    public void unfavoriteNote(Integer noteId) {
        Map<String,Object> map= ThreadLocalUtil.get();
        Integer userId=(Integer) map.get("id");

        // 删除收藏记录
        favoriteRecordMapper.delete(userId, 1, noteId);

        // 更新笔记的收藏数
        noteMapper.decrementFavoritesCount(noteId);
    }

    @Override
    public boolean isFavoritedNote(Integer noteId) {
        Map<String,Object> map= ThreadLocalUtil.get();
        if (map == null) {
            // 未登录用户，返回false
            return false;
        }
        
        Integer userId=(Integer) map.get("id");
        
        FavoriteRecord existing=favoriteRecordMapper.findByUserAndTarget(userId,1,noteId);
        return existing != null;
    }
}
