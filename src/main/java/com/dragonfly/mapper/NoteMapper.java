package com.dragonfly.mapper;

import com.dragonfly.pojo.Note;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 描述：笔记数据访问接口
 */
@Mapper
public interface NoteMapper {

    // 新增笔记（支持图文和视频）
    @Insert("INSERT INTO note (title, content, images, video, cover_img, note_category, topic_id, location, state, views_count, likes_count, comments_count, favorites_count, create_user, publish_time, create_time, update_time) " +
            "VALUES (#{title}, #{content}, #{images, typeHandler=com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler}, #{video}, #{coverImg}, #{noteCategory}, #{topicId}, #{location}, #{state}, #{viewsCount}, #{likesCount}, #{commentsCount}, #{favoritesCount}, #{createUser}, #{publishTime}, #{createTime}, #{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void add(Note note);

    // 查询所有笔记（按用户）
    @Select("SELECT * FROM note WHERE create_user = #{userId}")
    List<Note> list(Integer userId);

    // 根据id查询笔记（在XML中定义，使用resultMap处理images字段）
    Note findById(Integer id);

    @Update("UPDATE note SET title = #{title}, content = #{content}, images = #{images, typeHandler=com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler}, video = #{video}, cover_img = #{coverImg}, note_category = #{noteCategory}, topic_id = #{topicId}, " +
            "location = #{location}, state = #{state}, views_count = #{viewsCount}, likes_count = #{likesCount}, comments_count = #{commentsCount}, favorites_count = #{favoritesCount}, publish_time = #{publishTime}, update_time = #{updateTime} " +
            "WHERE id = #{id}")
    void update(Note note);


    // 删除笔记
    @Delete("DELETE FROM note WHERE id = #{id} ")
    void delete(Integer id);

    // 分页查询笔记（可选话题、状态、用户筛选，按更新时间倒序）
    @Select("<script>" +
            "SELECT * FROM note WHERE 1=1 " +
            "<if test='userId != null'> AND create_user = #{userId} </if>" +
            "<if test='topicId != null'> AND topic_id = #{topicId} </if>" +
            "<if test='state != null and state != \"\"'> AND state = #{state} </if>" +
            "ORDER BY COALESCE(update_time, create_time) DESC " +
            "LIMIT #{offset}, #{pageSize}" +
            "</script>")
    List<Note> listByPage(@Param("userId") Integer userId,
                          @Param("topicId") Integer topicId,
                          @Param("state") String state,
                          @Param("offset") Integer offset,
                          @Param("pageSize") Integer pageSize);

    // 统计笔记数量（与分页筛选条件一致）
    @Select("<script>" +
            "SELECT COUNT(*) FROM note WHERE 1=1 " +
            "<if test='userId != null'> AND create_user = #{userId} </if>" +
            "<if test='topicId != null'> AND topic_id = #{topicId} </if>" +
            "<if test='state != null and state != \"\"'> AND state = #{state} </if>" +
            "</script>")
    int countByFilters(@Param("userId") Integer userId,
                       @Param("topicId") Integer topicId,
                       @Param("state") String state);

    // 用户首页分页查询
    List<Note> listUserPage(@Param("userId") Integer userId,@Param("topicId") Integer topicId, @Param("keyword") String keyword,  @Param("offset") Integer offset, @Param("pageSize") Integer pageSize);
    int countUserPage(@Param("topicId") Integer topicId, @Param("keyword") String keyword, @Param("userId") Integer userId);

    // 增加笔记点赞数
    @Update("UPDATE note SET likes_count = likes_count + 1 WHERE id = #{id}")
    void incrementLikesCount(Integer id);

    // 减少笔记点赞数
    @Update("UPDATE note SET likes_count = likes_count - 1 WHERE id = #{id} AND likes_count > 0")
    void decrementLikesCount(Integer id);

    // 增加笔记浏览数
    @Update("UPDATE note SET views_count = views_count + 1 WHERE id = #{id}")
    void incrementViewsCount(Integer id);

    // 增加笔记收藏数
    @Update("UPDATE note SET favorites_count = favorites_count + 1 WHERE id = #{id}")
    void incrementFavoritesCount(Integer id);

    // 减少笔记收藏数
    @Update("UPDATE note SET favorites_count = favorites_count - 1 WHERE id = #{id} AND favorites_count > 0")
    void decrementFavoritesCount(Integer id);
}