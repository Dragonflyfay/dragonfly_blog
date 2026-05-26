package com.dragonfly.mapper;
import com.dragonfly.pojo.Comment;
import org.apache.ibatis.annotations.*;

import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 描述：评论mapper
 *
 * @param
 * @author 蜻蜓大王
 * @date 2026/5/9 17:07
 */
@Mapper
public interface CommentMapper {
    @Insert("INSERT INTO comment (note_id, user_id, parent_id, reply_to_user_id, content, likes_count, status, create_time) " +
            "VALUES (#{noteId}, #{userId}, #{parentId}, #{replyToUserId}, #{content}, #{likesCount}, #{status}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void add(Comment comment);

    @Select("SELECT c.*, u.username, u.nickname, u.user_pic, ru.nickname AS replyToUserName FROM comment c " +
            "LEFT JOIN user u ON c.user_id = u.id " +
            "LEFT JOIN user ru ON c.reply_to_user_id = ru.id " +
            "WHERE c.note_id = #{noteId} AND c.status = 1 " +
            "ORDER BY c.create_time ASC")
    List<Comment> listByNoteId(Integer noteId);

    @Select("SELECT c.*, u.username, u.nickname, u.user_pic FROM comment c " +
            "LEFT JOIN user u ON c.user_id = u.id " +
            "WHERE c.id = #{id}")
    Comment findById(Integer id);

    @Update("UPDATE comment SET content = #{content}, update_time = NOW() WHERE id = #{id}")
    void update(Comment comment);

    @Update("UPDATE comment SET status = 0, update_time = NOW() WHERE id = #{id}")
    void delete(Integer id);

    @Update("UPDATE comment SET likes_count = likes_count + 1 WHERE id = #{id}")
    void incrementLikesCount(Integer id);

    @Update("UPDATE comment SET likes_count = likes_count - 1 WHERE id = #{id} AND likes_count > 0")
    void decrementLikesCount(Integer id);

    @Select("SELECT COUNT(*) FROM comment WHERE note_id = #{noteId} AND status = 1")
    int countByNoteId(Integer noteId);

    // 管理员分页查询所有评论（含用户信息和笔记标题）
    @Select("<script>" +
            "SELECT c.*, u.username, u.nickname, u.user_pic, n.title AS note_title " +
            "FROM comment c " +
            "LEFT JOIN user u ON c.user_id = u.id " +
            "LEFT JOIN note n ON c.note_id = n.id " +
            "WHERE 1=1 " +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (c.content LIKE CONCAT('%', #{keyword}, '%') OR u.nickname LIKE CONCAT('%', #{keyword}, '%') OR n.title LIKE CONCAT('%', #{keyword}, '%')) " +
            "</if>" +
            "ORDER BY c.create_time DESC " +
            "LIMIT #{offset}, #{pageSize}" +
            "</script>")
    List<Comment> listAll(@Param("offset") Integer offset, @Param("pageSize") Integer pageSize, @Param("keyword") String keyword);

    // 统计评论总数（管理员）
    @Select("<script>" +
            "SELECT COUNT(*) FROM comment c " +
            "LEFT JOIN user u ON c.user_id = u.id " +
            "LEFT JOIN note n ON c.note_id = n.id " +
            "WHERE 1=1 " +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (c.content LIKE CONCAT('%', #{keyword}, '%') OR u.nickname LIKE CONCAT('%', #{keyword}, '%') OR n.title LIKE CONCAT('%', #{keyword}, '%')) " +
            "</if>" +
            "</script>")
    int countAll(@Param("keyword") String keyword);

    /**
     * 根据父评论ID查找子评论
     */
    @Select("SELECT * FROM comment WHERE parent_id = #{parentId} AND status = 1 ORDER BY create_time ASC")
    List<Comment> findByParentId(Integer parentId);


}