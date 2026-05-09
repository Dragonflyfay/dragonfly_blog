package com.dragonfly.service.impl;

import com.dragonfly.mapper.CommentMapper;
import com.dragonfly.mapper.LikeRecordMapper;
import com.dragonfly.mapper.NoteMapper;
import com.dragonfly.pojo.LikeRecord;
import com.dragonfly.pojo.Note;
import com.dragonfly.service.LikeService;
import com.dragonfly.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 描述：点赞记录 serviceimpl
 *
 * @param
 * @author 蜻蜓大王
 * @date 2026/5/9 21:00
 */
@Service
public class LikeServiceImpl implements LikeService {
    @Autowired
    private NoteMapper noteMapper;
    @Autowired
    private CommentMapper commentMapper;
    @Autowired
    private LikeRecordMapper likeRecordMapper;
    
    @Override
    @Transactional
    public void likeNote(Integer noteId) {
        Map<String,Object> map= ThreadLocalUtil.get();
        Integer userId=(Integer) map.get("id");

        // 检查是否已经点过赞
        LikeRecord existing=likeRecordMapper.findByUserAndTarget(userId,1,noteId);
        if(existing != null) {
            throw new RuntimeException("已经点过赞了");
        }

        // 创建点赞记录
        LikeRecord likeRecord = new LikeRecord();
        likeRecord.setUserId(userId);
        likeRecord.setTargetType(1); // 1表示笔记
        likeRecord.setTargetId(noteId);
        likeRecord.setCreateTime(LocalDateTime.now());
        likeRecordMapper.add(likeRecord);

        // 更新笔记的点赞数
        noteMapper.incrementLikesCount(noteId);
    }

    @Override
    @Transactional
    public void unlikeNote(Integer noteId) {
        Map<String,Object> map= ThreadLocalUtil.get();
        Integer userId=(Integer) map.get("id");

        // 删除点赞记录
        likeRecordMapper.delete(userId, 1, noteId);

        // 更新笔记的点赞数
        noteMapper.decrementLikesCount(noteId);
    }

    @Override
    public boolean isLikedNote(Integer noteId) {
        Map<String,Object> map= ThreadLocalUtil.get();
        if (map == null) {
            // 未登录用户，返回false
            return false;
        }
        
        Integer userId=(Integer) map.get("id");
        
        LikeRecord existing=likeRecordMapper.findByUserAndTarget(userId,1,noteId);
        return existing != null;
    }

    @Override
    @Transactional
    public void unlikeComment(Integer commentId) {
        Map<String,Object> map= ThreadLocalUtil.get();
        Integer userId=(Integer) map.get("id");

        // 删除点赞记录
        likeRecordMapper.delete(userId, 2, commentId);

        // 更新评论的点赞数
        commentMapper.decrementLikesCount(commentId);
    }

    @Override
    @Transactional
    public void likeComment(Integer commentId) {
        Map<String,Object> map= ThreadLocalUtil.get();
        Integer userId=(Integer) map.get("id");

        // 检查是否已经点过赞
        LikeRecord existing=likeRecordMapper.findByUserAndTarget(userId,2,commentId);
        if(existing != null) {
            throw new RuntimeException("已经点过赞了");
        }

        // 创建点赞记录
        LikeRecord likeRecord = new LikeRecord();
        likeRecord.setUserId(userId);
        likeRecord.setTargetType(2); // 2表示评论
        likeRecord.setTargetId(commentId);
        likeRecord.setCreateTime(LocalDateTime.now());
        likeRecordMapper.add(likeRecord);

        // 更新评论的点赞数
        commentMapper.incrementLikesCount(commentId);
    }

    @Override
    public boolean isLikedComment(Integer commentId) {
        Map<String,Object> map= ThreadLocalUtil.get();
        if (map == null) {
            // 未登录用户，返回false
            return false;
        }
        
        Integer userId=(Integer) map.get("id");
        
        LikeRecord existing=likeRecordMapper.findByUserAndTarget(userId,2,commentId);
        return existing != null;
    }
}
