package com.dragonfly.task;

import com.dragonfly.mapper.CommentMapper;
import com.dragonfly.mapper.NoteMapper;
import com.dragonfly.utils.RedisCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * 描述：点赞数定时同步任务
 *
 * @author 蜻蜓大王
 * @date 2026/8/9 21:52
 */
@Slf4j
@Component
@EnableScheduling
public class LikeCountSyncTask {

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private NoteMapper noteMapper;

    @Autowired
    private CommentMapper commentMapper;

    // Redis Key
    private static final String LIKE_COUNT_NOTE_KEY = "like:count:note:";
    private static final String LIKE_COUNT_COMMENT_KEY = "like:count:comment:";
    private static final String SYNC_NOTE_IDS_KEY = "sync:note:ids";
    private static final String SYNC_COMMENT_IDS_KEY = "sync:comment:ids";

    /**
     * 每5分钟同步一次笔记点赞数
     */
    @Scheduled(cron ="0 */5 * * * ?") //cron 表达式格式:
    @Transactional
    public void syncNoteLikeCounts() {
        log.info("🔄 开始同步笔记点赞数到MySQL...");

        // 使用 getSetMembers（返回 Set<Object>）
        Set<Object> noteIds = redisCache.getSetMembers(SYNC_NOTE_IDS_KEY);
        if (noteIds == null || noteIds.isEmpty()) {
            log.info("📭 没有需要同步的笔记点赞数");
            return;
        }

        int successCount = 0;
        int failCount = 0;

        // ✅ 直接遍历 String，不需要转 Object
        for (Object idObj : noteIds) {
            try {
                String idStr = idObj.toString();
                Integer noteId = Integer.parseInt(idStr);
                String countKey = LIKE_COUNT_NOTE_KEY + noteId;
                Number redisCount=redisCache.get(countKey);

                if (redisCount != null) {
                    noteMapper.updateLikesCount(noteId, redisCount.intValue());
                    successCount++;
                    log.debug("✅ 同步笔记 {} 点赞数: {}", noteId, redisCount);
                }else{
                    log.warn("⚠️ 笔记 {} 的点赞数在Redis中不存在", noteId);
                }

                // 从同步集合中移除
                redisCache.removeFromSet(SYNC_NOTE_IDS_KEY, idObj);

            } catch (NumberFormatException e) {
                failCount++;
                log.warn("⚠️ 无效的笔记ID格式: {}", idObj);
                redisCache.removeFromSet(SYNC_NOTE_IDS_KEY, idObj);
            } catch (Exception e) {
                failCount++;
                log.error("❌ 同步笔记 {} 失败: {}", idObj, e.getMessage());
            }
        }

        log.info("✅ 笔记点赞数同步完成: 成功 {} 条，失败 {} 条", successCount, failCount);
    }

    /**
     * 每5分钟同步一次评论点赞数
     */
    @Scheduled(cron = "0 */5 * * * ?")
    @Transactional
    public void syncCommentLikeCounts() {
        log.info("🔄 开始同步评论点赞数到MySQL...");

        // ✅ 同样使用 getSetMembers
        Set<Object> commentIds = redisCache.getSetMembers(SYNC_COMMENT_IDS_KEY);
        if (commentIds == null || commentIds.isEmpty()) {
            log.info("📭 没有需要同步的评论点赞数");
            return;
        }

        int successCount = 0;
        int failCount = 0;

        // ✅ 直接遍历 String
        for (Object idObj : commentIds) {
            try {
                String idStr=idObj.toString();
                Integer commentId = Integer.parseInt(idStr);
                String countKey = LIKE_COUNT_COMMENT_KEY + commentId;
                Number countNum = redisCache.get(countKey);

                if (countNum != null) {

                    commentMapper.updateLikesCount(commentId, countNum.intValue());
                    successCount++;
                    log.debug("✅ 同步评论 {} 点赞数: {}", commentId, countNum);
                }

                redisCache.removeFromSet(SYNC_COMMENT_IDS_KEY, idObj);

            } catch (NumberFormatException e) {
                failCount++;
                log.warn("⚠️ 无效的评论ID格式: {}", idObj);
                redisCache.removeFromSet(SYNC_COMMENT_IDS_KEY, idObj);
            } catch (Exception e) {
                failCount++;
                log.error("❌ 同步评论 {} 失败: {}", idObj, e.getMessage());
            }
        }

        log.info("✅ 评论点赞数同步完成: 成功 {} 条，失败 {} 条", successCount, failCount);
    }

    /**
     * 每天凌晨3点全量同步一次（兜底方案）
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void fullSync() {
        log.info("🔄 开始全量同步点赞数到MySQL...");
        syncNoteLikeCounts();
        syncCommentLikeCounts();
        log.info("✅ 全量同步完成");
    }
}