package com.dragonfly.service.impl;

import com.dragonfly.enums.NotificationType;
import com.dragonfly.mapper.CommentMapper;
import com.dragonfly.mapper.LikeRecordMapper;
import com.dragonfly.mapper.NoteMapper;
import com.dragonfly.mapper.NotificationMapper;
import com.dragonfly.pojo.LikeRecord;
import com.dragonfly.pojo.Note;
import com.dragonfly.pojo.Notification;
import com.dragonfly.service.LikeService;
import com.dragonfly.service.NotificationService;
import com.dragonfly.utils.JwtUtil;
import com.dragonfly.utils.RedisCache;
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
import java.util.concurrent.TimeUnit;

@Service
public class LikeServiceImpl implements LikeService {

    private static final Logger log = LoggerFactory.getLogger(LikeServiceImpl.class);

    // Redis Key前缀
    private static final String LIKE_NOTE_KEY = "like:note:";
    private static final String LIKE_COMMENT_KEY = "like:comment:";
    private static final String LIKE_COUNT_NOTE_KEY = "like:count:note:";
    private static final String LIKE_COUNT_COMMENT_KEY = "like:count:comment:";
    private static final String SYNC_NOTE_IDS_KEY = "sync:note:ids";
    private static final String SYNC_COMMENT_IDS_KEY = "sync:comment:ids";
    @Autowired
    private NoteMapper noteMapper;
    @Autowired
    private CommentMapper commentMapper;
    @Autowired
    private LikeRecordMapper likeRecordMapper;
    @Autowired
    private RedisCache redisCache;
    @Autowired
    private NotificationService notificationService;


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

        //优先查找Redis
        String userKey= LIKE_NOTE_KEY + noteId + ":" + userId;
        if(redisCache.hasKey(userKey)){
            return true;
        }
        //Redis没有，查数据库
        LikeRecord existing =likeRecordMapper.findByUserAndTarget(userId, 1, noteId);
        if(existing!=null){
            //回填Redis
            redisCache.set(userKey, "1",7,TimeUnit.HOURS);
            return true;
        }
        return false;
    }

    @Override
    @Transactional
    public void likeNote(Integer noteId) {
        Integer userId = getCurrentUserId();

        if (userId == null) {
            throw new RuntimeException("用户未登录");
        }
        // 1. Redis去重：检查是否已经点赞
        String userKey = LIKE_NOTE_KEY + noteId + ":" + userId;
        if (redisCache.hasKey(userKey)) {
            log.info("用户 {} 已经点赞过笔记 {}", userId, noteId);
            return;
        }
        // 2. 获取笔记信息（复用：初始化计数器 + 发通知）
        Note note = noteMapper.findById(noteId);

        // 3. 记录点赞状态（7天过期，避免Redis无限膨胀）
        redisCache.set(userKey, "1", 7, TimeUnit.DAYS);

        // 4. 初始化Redis计数器（若key不存在，先从DB加载，避免覆盖真实数据）
        String countKey = LIKE_COUNT_NOTE_KEY + noteId;
        if (!redisCache.hasKey(countKey)) {
            if (note != null && note.getLikesCount() != null) {
                redisCache.set(countKey, note.getLikesCount());
            }
        }
        // 5. 点赞数 +1（Redis自增）
        Long count = redisCache.increment(countKey);
        log.info("笔记 {} 点赞数: {}", noteId, count);

        // 6. 写入点赞记录表
        LikeRecord likeRecord = new LikeRecord();
        likeRecord.setUserId(userId);
        likeRecord.setTargetType(1);
        likeRecord.setTargetId(noteId);
        likeRecord.setCreateTime(LocalDateTime.now());
        likeRecordMapper.add(likeRecord);

        // 7. 更新 note 表的点赞数（从Redis读取最新值）
        if (count != null) {
            noteMapper.updateLikesCount(noteId, count.intValue());
        }
        // ===== 发送通知 =====
        if (note != null && !note.getCreateUser().equals(userId)) {
            Notification notification = new Notification();
            notification.setUserId(note.getCreateUser());
            notification.setFromUserId(userId);
            notification.setType(NotificationType.LIKE_NOTE.getCode());
            notification.setTargetType(1);
            notification.setTargetId(noteId);
            notification.setContent("点赞了你的笔记《" + note.getTitle() + "》");
            notificationService.sendNotification(notification);
        }
        // 8. 添加到 Redis 的同步列表
        redisCache.addToSet(SYNC_NOTE_IDS_KEY, String.valueOf(noteId));
    }

    @Override
    @Transactional
    public void unlikeNote(Integer noteId) {
        Integer userId = getCurrentUserId();
        if (userId == null) {
            throw new RuntimeException("用户未登录");
        }

        // 1. 删除Redis点赞状态
        String userKey = LIKE_NOTE_KEY + noteId + ":" + userId;
        redisCache.delete(userKey);

        // 2. 初始化Redis计数器（若key不存在，先从DB加载，避免减到负数）
        String countKey = LIKE_COUNT_NOTE_KEY + noteId;
        if (!redisCache.hasKey(countKey)) {
            Note note = noteMapper.findById(noteId);
            if (note != null && note.getLikesCount() != null) {
                redisCache.set(countKey, note.getLikesCount());
            }
        }
        // 3. 点赞数 -1
        Long count = redisCache.decrement(countKey);

        //  4. 删除MySQL点赞记录
        likeRecordMapper.delete(userId, 1, noteId);

        //  5. 更新 note 表的点赞数（从Redis读取最新值）
        if (count != null && count >= 0) {
            noteMapper.updateLikesCount(noteId, count.intValue());
        } else {
            // 如果Redis异常，直接减1
            noteMapper.decrementLikesCount(noteId);
        }

        // 标记需要同步的笔记ID
        redisCache.addToSet(SYNC_NOTE_IDS_KEY, String.valueOf(noteId));
    }



    @Override
    @Transactional
    public void unlikeComment(Integer commentId) {
        Integer userId=getCurrentUserId();
        if(userId==null){
            throw new RuntimeException("用户未登录");

        }
        //1.删除Redis点赞状态
        String userKey=LIKE_COMMENT_KEY+commentId+":"+userId;
        redisCache.delete(userKey);

        //2.初始化Redis计数器（若key不存在，先从DB加载，避免减到负数）
        String countKey=LIKE_COUNT_COMMENT_KEY+commentId;
        if (!redisCache.hasKey(countKey)) {
            var comment = commentMapper.findById(commentId);
            if (comment != null && comment.getLikesCount() != null) {
                redisCache.set(countKey, comment.getLikesCount());
            }
        }
        //3.点赞数-1(Redis自减)
        Long count= redisCache.decrement(countKey);

        //4.删除MySql 点赞记录

        likeRecordMapper.delete(userId, 2, commentId);

        //  5. 更新 comment 表的点赞数（从Redis读取最新值）
        if (count != null && count >= 0) {
            commentMapper.updateLikesCount(commentId, count.intValue());
        } else {
            // 兜底：如果Redis异常，直接减1
            commentMapper.decrementLikesCount(commentId);
        }
        //  标记需要同步的评论ID
        redisCache.addToSet(SYNC_COMMENT_IDS_KEY, String.valueOf(commentId));

    }
    //点赞评论

    @Override
    @Transactional
    public void likeComment(Integer commentId) {
        Integer userId = getCurrentUserId();
        if (userId == null) {
            throw new RuntimeException("用户未登录");
        }

        // 1. Redis去重：检查是否已经点赞
        String userKey = LIKE_COMMENT_KEY + commentId + ":" + userId;
        if (redisCache.hasKey(userKey)) {
            log.info("用户 {} 已经点赞过评论 {}", userId, commentId);
            return;
        }
        // 2. 获取评论信息（复用：初始化计数器 + 发通知）
        var comment = commentMapper.findById(commentId);

        // 3. 记录点赞状态（7天过期）
        redisCache.set(userKey, "1", 7, TimeUnit.DAYS);

        // 4. 初始化Redis计数器（若key不存在，先从DB加载，避免覆盖真实数据）
        String countKey = LIKE_COUNT_COMMENT_KEY + commentId;
        if (!redisCache.hasKey(countKey)) {
            if (comment != null && comment.getLikesCount() != null) {
                redisCache.set(countKey, comment.getLikesCount());
            }
        }
        // 5. 点赞数 +1（Redis自增）
        Long count = redisCache.increment(countKey);
        log.info("评论 {} 点赞数: {}", commentId, count);
        // 6. 写入MySQL点赞记录
        LikeRecord likeRecord = new LikeRecord();
        likeRecord.setUserId(userId);
        likeRecord.setTargetType(2); // 2表示评论
        likeRecord.setTargetId(commentId);
        likeRecord.setCreateTime(LocalDateTime.now());
        likeRecordMapper.add(likeRecord);

        // 7. 更新 comment 表的点赞数（从Redis读取最新值）
        if (count != null) {
            commentMapper.updateLikesCount(commentId, count.intValue());
        }
        // 8. 发送通知
        if (comment != null && !comment.getUserId().equals(userId)) {
            Notification notification = new Notification();
            notification.setUserId(comment.getUserId());
            notification.setFromUserId(userId);
            notification.setType(NotificationType.LIKE_COMMENT.getCode());
            notification.setTargetType(2);
            notification.setTargetId(commentId);
            notification.setContent("点赞了你的评论：" + comment.getContent());
            notificationService.sendNotification(notification);
        }
        // 9. 添加到 Redis 的同步列表
        //  标记需要同步的评论ID
        redisCache.addToSet(SYNC_COMMENT_IDS_KEY, String.valueOf(commentId));
    }

    // 7. 检查是否已经点赞
    @Override
    public boolean isLikedComment(Integer commentId) {
        Integer userId = getCurrentUserId();
        if (userId == null) {
            return false;
        }
        //  优先查找Redis
        String userKey = LIKE_COMMENT_KEY + commentId + ":" + userId;
        if (redisCache.hasKey(userKey)) {
            return true;
        }
        // ✅ Redis没有，查数据库（兜底）
        LikeRecord existing = likeRecordMapper.findByUserAndTarget(userId, 2, commentId);

        if (existing != null) {
            // 回填Redis（7天过期）
            redisCache.set(userKey, "1", 7, TimeUnit.DAYS);
            return true;
        }
        return false;
    }

    @Override
    public Map<Integer, Boolean> batchCheckLikedNotes(List<Integer> noteIds) {
        Map<Integer, Boolean> result = new HashMap<>();
        
        if (noteIds == null || noteIds.isEmpty()) {
            return result;
        }

        Integer userId = getCurrentUserId();
        if (userId == null) {
            for (Integer noteId : noteIds) {
                result.put(noteId, false);
            }
            return result;
        }

        // ✅ 批量查Redis
        for (Integer noteId : noteIds) {
            String userKey = LIKE_NOTE_KEY + noteId + ":" + userId;
            result.put(noteId, redisCache.hasKey(userKey));
        }
        
        return result;
    }

    @Override
    public List<Integer> getLikedNoteIds() {
        Integer userId = getCurrentUserId();
        if (userId == null) {
            return List.of();
        }
        return likeRecordMapper.findLikedNoteIdsByUserId(userId);
    }

    @Override
    public Map<Integer, Boolean> batchCheckLikedComments(List<Integer> commentIds) {
        Map<Integer, Boolean> result = new HashMap<>();
        
        if (commentIds == null || commentIds.isEmpty()) {
            return result;
        }

        Integer userId = getCurrentUserId();
        if (userId == null) {
            for (Integer commentId : commentIds) {
                result.put(commentId, false);
            }
            return result;
        }

        // ✅ 批量查Redis
        for (Integer commentId : commentIds) {
            String userKey = LIKE_COMMENT_KEY + commentId + ":" + userId;
            result.put(commentId, redisCache.hasKey(userKey));
        }
        return result;
    }
}
