package com.dragonfly.service.impl;

import com.dragonfly.mapper.CommentMapper;
import com.dragonfly.mapper.LikeRecordMapper;
import com.dragonfly.mapper.NoteMapper;
import com.dragonfly.pojo.LikeRecord;
import com.dragonfly.service.LikeService;
import com.dragonfly.utils.JwtUtil;
import com.dragonfly.utils.ThreadLocalUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LikeServiceImpl implements LikeService {
    private static final Logger log = LoggerFactory.getLogger(LikeServiceImpl.class);

    @Autowired
    private NoteMapper noteMapper;
    @Autowired
    private CommentMapper commentMapper;
    @Autowired
    private LikeRecordMapper likeRecordMapper;

    /**
     * 获取当前用户ID（优先从ThreadLocal，失败则从Token解析）
     */
    private Integer getCurrentUserId() {
        // 1. 先从 ThreadLocal 获取
        Map<String, Object> map = ThreadLocalUtil.get();
        if (map != null && map.get("id") != null) {
            return (Integer) map.get("id");
        }

        // 2. 从请求头获取 Token 并解析
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String token = request.getHeader("Authorization");
                if (token != null && !token.isEmpty()) {
                    Map<String, Object> claims = JwtUtil.parseToken(token);
                    return (Integer) claims.get("id");
                }
            }
        } catch (Exception e) {
            log.error("获取用户ID失败: {}", e.getMessage());
        }

        return null;
    }

    @Override
    public boolean isLikedNote(Integer noteId) {
        Integer userId = getCurrentUserId();


        if (userId == null) {
            return false;
        }

        LikeRecord existing = likeRecordMapper.findByUserAndTarget(userId, 1, noteId);


        return existing != null;
    }

    @Override
    @Transactional
    public void likeNote(Integer noteId) {
        Integer userId = getCurrentUserId();


        if (userId == null) {
            throw new RuntimeException("用户未登录");
        }

        // 检查是否已经点过赞
        LikeRecord existing = likeRecordMapper.findByUserAndTarget(userId, 1, noteId);
        if (existing != null) {

            return;
        }

        // 创建点赞记录
        LikeRecord likeRecord = new LikeRecord();
        likeRecord.setUserId(userId);
        likeRecord.setTargetType(1);
        likeRecord.setTargetId(noteId);
        likeRecord.setCreateTime(LocalDateTime.now());
        likeRecordMapper.add(likeRecord);

        // 更新笔记的点赞数
        noteMapper.incrementLikesCount(noteId);
    }

    @Override
    @Transactional
    public void unlikeNote(Integer noteId) {
        Integer userId = getCurrentUserId();


        if (userId == null) {
            throw new RuntimeException("用户未登录");
        }

        likeRecordMapper.delete(userId, 1, noteId);
        noteMapper.decrementLikesCount(noteId);

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
            return;
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

    @Override
    public Map<Integer, Boolean> batchCheckLikedNotes(List<Integer> noteIds) {
        Map<Integer, Boolean> result = new HashMap<>();
        
        if (noteIds == null || noteIds.isEmpty()) {
            return result;
        }
        
        Map<String,Object> map = ThreadLocalUtil.get();
        if (map == null) {
            // 未登录用户，所有笔记都返回false
            for (Integer noteId : noteIds) {
                result.put(noteId, false);
            }
            return result;
        }
        
        Integer userId = (Integer) map.get("id");
        
        // 批量查询点赞记录
        List<LikeRecord> likedRecords = likeRecordMapper.batchFindByUserAndTarget(userId, 1, noteIds);
        
        // 初始化所有笔记为未点赞
        for (Integer noteId : noteIds) {
            result.put(noteId, false);
        }
        
        // 标记已点赞的笔记
        for (LikeRecord record : likedRecords) {
            result.put(record.getTargetId(), true);
        }
        
        return result;
    }

    @Override
    public Map<Integer, Boolean> batchCheckLikedComments(List<Integer> commentIds) {
        Map<Integer, Boolean> result = new HashMap<>();
        
        if (commentIds == null || commentIds.isEmpty()) {
            return result;
        }
        
        Map<String,Object> map = ThreadLocalUtil.get();
        if (map == null) {
            // 未登录用户，所有评论都返回false
            for (Integer commentId : commentIds) {
                result.put(commentId, false);
            }
            return result;
        }
        
        Integer userId = (Integer) map.get("id");
        
        // 批量查询点赞记录
        List<LikeRecord> likedRecords = likeRecordMapper.batchFindByUserAndTarget(userId, 2, commentIds);
        
        // 初始化所有评论为未点赞
        for (Integer commentId : commentIds) {
            result.put(commentId, false);
        }
        
        // 标记已点赞的评论
        for (LikeRecord record : likedRecords) {
            result.put(record.getTargetId(), true);
        }
        
        return result;
    }
}
