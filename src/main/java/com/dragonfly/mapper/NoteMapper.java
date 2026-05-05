package com.dragonfly.mapper;

import com.dragonfly.pojo.Note;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 描述：笔记数据访问接口
 *
 * @param
 * @author 蜻蜓大王
 * @date 2026/5/5 14:08
 */
@Mapper
public interface NoteMapper {

    // 新增笔记
    @Insert("INSERT INTO note (title, content, images, topic_id, location, state, create_user, publish_time, create_time, update_time) " +
            "VALUES (#{title}, #{content}, #{images}, #{topicId}, #{location}, #{state}, #{createUser}, #{publishTime}, #{createTime}, #{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void add(Note note);

    // 查询所有笔记（按用户）
    @Select("SELECT * FROM note WHERE create_user = #{userId}")
    List<Note> list(Integer userId);

    // 根据id查询笔记
    @Select("SELECT * FROM note WHERE id = #{id}")
    Note findById(Integer id);

    // 更新笔记
    @Update("UPDATE note SET title = #{title}, content = #{content}, images = #{images}, topic_id = #{topicId}, " +
            "location = #{location}, state = #{state}, publish_time = #{publishTime}, update_time = #{updateTime} " +
            "WHERE id = #{id}")
    void update(Note note);

    // 删除笔记
    @Delete("DELETE FROM note WHERE id = #{id} AND create_user = #{userId}")
    void delete(Integer id, Integer userId);

    // 分页查询笔记
    @Select("SELECT * FROM note WHERE create_user = #{userId} LIMIT #{offset}, #{pageSize}")
    List<Note> listByPage(@Param("userId") Integer userId, @Param("offset") Integer offset, @Param("pageSize") Integer pageSize);

    // 统计笔记数量
    @Select("SELECT COUNT(*) FROM note WHERE create_user = #{userId}")
    int countByUserId(Integer userId);
}
