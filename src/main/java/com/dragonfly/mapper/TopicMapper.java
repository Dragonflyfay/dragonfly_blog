package com.dragonfly.mapper;

import com.dragonfly.pojo.Topic;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 描述：话题数据访问接口
 *
 * @param
 * @author 蜻蜓大王
 * @date 2026/5/5 14:08
 */
@Mapper
public interface TopicMapper {

    // 新增话题
    @Insert("INSERT INTO topic (topic_name, notes_count, cover_img, description, create_time, update_time) " +
            "VALUES (#{topicName}, #{notesCount}, #{coverImg}, #{description}, #{createTime}, #{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void add(Topic topic);

    // 查询所有话题
    @Select("SELECT * FROM topic")
    List<Topic> list();

    // 根据id查询话题
    @Select("SELECT * FROM topic WHERE id = #{id}")
    Topic findById(Integer id);

    // 更新话题
    @Update("UPDATE topic SET topic_name = #{topicName}, notes_count = #{notesCount}, " +
            "cover_img = #{coverImg}, description = #{description}, update_time = #{updateTime} " +
            "WHERE id = #{id}")
    void update(Topic topic);

    // 删除话题
    @Delete("DELETE FROM topic WHERE id = #{id}")
    void delete(Integer id);
}
