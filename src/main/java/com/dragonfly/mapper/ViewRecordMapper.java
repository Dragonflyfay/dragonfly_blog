package com.dragonfly.mapper;

import com.dragonfly.pojo.ViewRecord;
import org.apache.ibatis.annotations.*;

/**
 * 描述：浏览记录mapper
 *
 * @param
 * @author 蜻蜓大王
 * @date 2026/5/9 17:10
 */
@Mapper
public interface ViewRecordMapper {
    @Insert("INSERT INTO view_record (note_id, user_id, view_time) " +
            "VALUES (#{noteId}, #{userId}, #{viewTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void add(ViewRecord viewRecord);

    @Select("SELECT * FROM view_record WHERE note_id = #{noteId} AND user_id = #{userId} ORDER BY view_time DESC LIMIT 1")
    ViewRecord findLatestByNoteAndUser(@Param("noteId") Integer noteId,
                                       @Param("userId") Integer userId);

    @Select("SELECT COUNT(DISTINCT user_id) FROM view_record WHERE note_id = #{noteId}")
    int countUniqueViewsByNoteId(Integer noteId);
}
