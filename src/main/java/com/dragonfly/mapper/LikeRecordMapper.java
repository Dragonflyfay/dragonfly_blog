package com.dragonfly.mapper;
import com.dragonfly.pojo.LikeRecord;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 描述：点赞mapper
 *
 * @param
 * @author 蜻蜓大王
 * @date 2026/5/9 17:09
 */
@Mapper
public interface LikeRecordMapper {
    @Insert("INSERT INTO like_record (user_id, target_type, target_id, create_time) " +
            "VALUES (#{userId}, #{targetType}, #{targetId}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void add(LikeRecord likeRecord);

    @Delete("DELETE FROM like_record WHERE user_id = #{userId} AND target_type = #{targetType} AND target_id = #{targetId}")
    void delete(@Param("userId") Integer userId,
                @Param("targetType") Integer targetType,
                @Param("targetId") Integer targetId);

    @Select("SELECT * FROM like_record WHERE user_id = #{userId} AND target_type = #{targetType} AND target_id = #{targetId}")
    LikeRecord findByUserAndTarget(@Param("userId") Integer userId,
                                   @Param("targetType") Integer targetType,
                                   @Param("targetId") Integer targetId);

    @Select("SELECT COUNT(*) FROM like_record WHERE target_type = #{targetType} AND target_id = #{targetId}")
    int countByTarget(@Param("targetType") Integer targetType,
                      @Param("targetId") Integer targetId);

    @Select("<script>" +
            "SELECT * FROM like_record WHERE user_id = #{userId} AND target_type = #{targetType} AND target_id IN " +
            "<foreach item='id' collection='targetIds' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    List<LikeRecord> batchFindByUserAndTarget(@Param("userId") Integer userId,
                                               @Param("targetType") Integer targetType,
                                               @Param("targetIds") List<Integer> targetIds);

    // 查询用户点赞的所有笔记ID列表
    @Select("SELECT target_id FROM like_record WHERE user_id = #{userId} AND target_type = 1 ORDER BY create_time DESC")
    List<Integer> findLikedNoteIdsByUserId(Integer userId);
}
