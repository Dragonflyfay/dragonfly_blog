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
import java.util.HashMap;
import java.util.List;
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
        System.out.println("》》》》=== ThreadLocal 内容: " + map);
        if (map == null) {
            // 未登录用户，返回false
            return false;
        }
        
        Integer userId=(Integer) map.get("id");
        
        FavoriteRecord existing=favoriteRecordMapper.findByUserAndTarget(userId,1,noteId);
        System.out.println("===♥ 收藏检查结果: " + existing);
        if (existing != null) {
            System.out.println("♥=== 找到收藏记录: userId=" + existing.getUserId() + ", noteId=" + existing.getTargetId());
        }
        return existing != null;
    }

    @Override
    public Map<Integer, Boolean> batchCheckFavoritedNotes(List<Integer> noteIds) {
        Map<Integer, Boolean> result = new HashMap<>();

        if (noteIds == null || noteIds.isEmpty()) {
            return result;
        }

        Map<String,Object> map = ThreadLocalUtil.get();
        if (map == null) {
            for (Integer noteId : noteIds) {
                result.put(noteId, false);
            }
            return result;
        }

        Integer userId = (Integer) map.get("id");

        // 批量查询收藏记录
        List<FavoriteRecord> favoritedRecords = favoriteRecordMapper.batchFindByUserAndTarget(userId, 1, noteIds);

        // 初始化所有笔记为未收藏
        for (Integer noteId : noteIds) {
            result.put(noteId, false);
        }

        // 标记已收藏的笔记
        for (FavoriteRecord record : favoritedRecords) {
            result.put(record.getTargetId(), true);
        }

        return result;
    }

    @Override
    public List<Integer> getFavoriteNoteIds() {
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("id");
        return favoriteRecordMapper.findFavoriteNoteIdsByUserId(userId);
    }


}
