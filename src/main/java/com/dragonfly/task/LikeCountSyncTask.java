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
 * 描述：
 *
 * @param
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
    @Scheduled(cron = "0 */5 * * * ?") // 5分钟
    @Transactional
    public void syncNoteLikeCounts() {
        log.info("🔄 开始同步笔记点赞数到MySQL...");

        Set<Object> noteIds = redisCache.getSetMembers(SYNC_NOTE_IDS_KEY);
        if (noteIds == null || noteIds.isEmpty()) {
            log.info("📭 没有需要同步的笔记点赞数");
            return;
        }

        int successCount = 0;
        int failCount = 0;

        for (Object idObj : noteIds) {
            try {
                Integer noteId = Integer.parseInt(idObj.toString());
                String countKey = LIKE_COUNT_NOTE_KEY + noteId;
                String countStr = redisCache.get(countKey);

                if (countStr != null) {
                    int redisCount = Integer.parseInt(countStr);
                    // 更新数据库
                    noteMapper.updateLikesCount(noteId, redisCount);
                    successCount++;
                    log.debug("✅ 同步笔记 {} 点赞数: {}", noteId, redisCount);
                }

                // 从同步集合中移除（无论是否成功，避免重复尝试）
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

        Set<Object> commentIds = redisCache.getSetMembers(SYNC_COMMENT_IDS_KEY);
        if (commentIds == null || commentIds.isEmpty()) {
            log.info("📭 没有需要同步的评论点赞数");
            return;
        }

        int successCount = 0;
        int failCount = 0;

        for (Object idObj : commentIds) {
            try {
                Integer commentId = Integer.parseInt(idObj.toString());
                String countKey = LIKE_COUNT_COMMENT_KEY + commentId;
                String countStr = redisCache.get(countKey);

                if (countStr != null) {
                    int redisCount = Integer.parseInt(countStr);
                    // 更新数据库
                    commentMapper.updateLikesCount(commentId, redisCount);
                    successCount++;
                    log.debug("✅ 同步评论 {} 点赞数: {}", commentId, redisCount);
                }

                // 从同步集合中移除
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
     * 把所有 Redis 中的点赞数全部同步到 MySQL
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void fullSync() {
        log.info("🔄 开始全量同步点赞数到MySQL...");

        // 先执行增量同步
        syncNoteLikeCounts();
        syncCommentLikeCounts();

        // TODO: 如果需要全量扫描所有 note 和 comment 的点赞数
        // 可以用 SCAN 命令扫描所有 like:count:note:* 和 like:count:comment:* 的 key

        log.info("✅ 全量同步完成");
    }
}