package com.dragonfly.mapper;
import com.dragonfly.pojo.Comment;
import org.apache.ibatis.annotations.*;

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
    @Insert("INSERT INTO comment (note_id, user_id, parent_id, reply_to_user_id, content, likes_count, status, create_time, update_time) " +
            "VALUES (#{noteId}, #{userId}, #{parentId}, #{replyToUserId}, #{content}, #{likesCount}, #{status}, #{createTime}, #{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void add(Comment comment);

    @Select("SELECT c.*, u.username, u.nickname, u.user_pic FROM comment c " +
            "LEFT JOIN user u ON c.user_id = u.id " +
            "WHERE c.note_id = #{noteId} AND c.status = 1 " +
            "ORDER BY c.create_time ASC")
    List<Comment> listByNoteId(Integer noteId);

    @Select("SELECT c.*, u.username, u.nickname, u.user_pic FROM comment c " +
            "LEFT JOIN user u ON c.user_id = u.id " +
            "WHERE c.id = #{id}")
    Comment findById(Integer id);

    @Update("UPDATE comment SET content = #{content}, update_time = #{updateTime} WHERE id = #{id}")
    void update(Comment comment);

    @Update("UPDATE comment SET status = 0, update_time = NOW() WHERE id = #{id}")
    void delete(Integer id);

    @Update("UPDATE comment SET likes_count = likes_count + 1 WHERE id = #{id}")
    void incrementLikesCount(Integer id);

    @Update("UPDATE comment SET likes_count = likes_count - 1 WHERE id = #{id} AND likes_count > 0")
    void decrementLikesCount(Integer id);

    @Select("SELECT COUNT(*) FROM comment WHERE note_id = #{noteId} AND status = 1")
    int countByNoteId(Integer noteId);
}
