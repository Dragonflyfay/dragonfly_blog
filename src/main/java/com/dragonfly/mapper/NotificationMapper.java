package com.dragonfly.mapper;

import com.dragonfly.pojo.Notification;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 描述：
 *
 * @param
 * @author 蜻蜓大王
 * @date 2026/7/30 10:43
 */
@Mapper
public interface NotificationMapper {
    @Insert("INSERT INTO notification (user_id, from_user_id, type, target_type, target_id, content, is_read, create_time) " +
            "VALUES (#{userId}, #{fromUserId}, #{type}, #{targetType}, #{targetId}, #{content}, #{isRead}, #{createTime})")
    @Options(useGeneratedKeys = true,keyProperty = "id")
    void insert(Notification notification);
    // 批量插入通知（用于提升性能）
    @Insert("<script>" +
            "INSERT INTO notification (user_id, from_user_id, type, target_type, target_id, content, is_read, create_time) VALUES " +
            "<foreach collection='list' item='item' separator=','>" +
            "(#{item.userId}, #{item.fromUserId}, #{item.type}, #{item.targetType}, #{item.targetId}, #{item.content}, #{item.isRead}, #{item.createTime})" +
            "</foreach>" +
            "</script>")
    void batchInsert(List<Notification> notifications);

    // 查询用户通知列表（按时间倒序）
    @Select("SELECT n.*, u.nickname as from_user_name, u.user_pic as from_user_pic, " +
            "CASE " +
            "  WHEN n.target_type = 1 THEN (SELECT title FROM note WHERE id = n.target_id) " +
            "  WHEN n.target_type = 2 THEN (SELECT content FROM comment WHERE id = n.target_id) " +
            "  ELSE NULL " +
            "END as target_title " +
            "FROM notification n " +
            "LEFT JOIN user u ON n.from_user_id = u.id " +
            "WHERE n.user_id = #{userId} " +
            "ORDER BY n.create_time DESC " +
            "LIMIT #{offset}, #{pageSize}")
    List<Notification> findByUserId(@Param("userId") Integer userId,
                                    @Param("offset") Integer offset,
                                    @Param("pageSize") Integer pageSize);

    @Select("SELECT COUNT(*) FROM notification WHERE user_id=#{userId}")
    int countByUserId(Integer userId);

    // 统计未读通知数
    @Select("SELECT COUNT(*) FROM notification WHERE user_id = #{userId} AND is_read = 0")
    int countUnreadByUserId(Integer userId);

    // 标记通知为已读
    @Update("UPDATE notification SET is_read = 1 WHERE id = #{id} AND user_id = #{userId}")
    int markAsRead(@Param("id") Integer id, @Param("userId") Integer userId);

    // 批量标记已读
    @Update("UPDATE notification SET is_read = 1 WHERE user_id = #{userId} AND is_read = 0")
    int markAllAsRead(Integer userId);

    // 删除通知
    @Delete("DELETE FROM notification WHERE id = #{id} AND user_id = #{userId}")
    int deleteById(@Param("id") Integer id, @Param("userId") Integer userId);

    // 删除所有已读通知
    @Delete("DELETE FROM notification WHERE user_id = #{userId} AND is_read = 1")
    int deleteAllRead(Integer userId);

    // 获取最近的未读通知（用于红点提示）
    @Select("SELECT * FROM notification WHERE user_id = #{userId} AND is_read = 0 ORDER BY create_time DESC LIMIT 5")
    List<Notification> findRecentUnread(Integer userId);
}
