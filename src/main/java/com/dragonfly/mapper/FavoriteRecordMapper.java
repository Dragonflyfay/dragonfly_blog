package com.dragonfly.mapper;

import com.dragonfly.pojo.FavoriteRecord;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 描述：收藏记录mapper
 *
 * @param
 * @author 蜻蜓大王
 * @date 2026/5/9 22:00
 */
@Mapper
public interface FavoriteRecordMapper {
    @Insert("INSERT INTO favorite_record (user_id, target_type, target_id, create_time) " +
            "VALUES (#{userId}, #{targetType}, #{targetId}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void add(FavoriteRecord favoriteRecord);

    @Delete("DELETE FROM favorite_record WHERE user_id = #{userId} AND target_type = #{targetType} AND target_id = #{targetId}")
    void delete(@Param("userId") Integer userId,
                @Param("targetType") Integer targetType,
                @Param("targetId") Integer targetId);

    @Select("SELECT * FROM favorite_record WHERE user_id = #{userId} AND target_type = #{targetType} AND target_id = #{targetId}")
    FavoriteRecord findByUserAndTarget(@Param("userId") Integer userId,
                                       @Param("targetType") Integer targetType,
                                       @Param("targetId") Integer targetId);

    @Select("SELECT COUNT(*) FROM favorite_record WHERE target_type = #{targetType} AND target_id = #{targetId}")
    int countByTarget(@Param("targetType") Integer targetType,
                      @Param("targetId") Integer targetId);

    //批量查询用户对多个目标的收藏记录（用于批量查询笔记或评论的收藏状态）
    @Select("<script>" +
            "SELECT * FROM favorite_record WHERE user_id = #{userId} AND target_type = #{targetType} AND target_id IN " +
            "<foreach collection='targetIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    List<FavoriteRecord> batchFindByUserAndTarget(@Param("userId") Integer userId,
                                                  @Param("targetType") Integer targetType,
                                                  @Param("targetIds") List<Integer> targetIds);

    // 查询用户收藏的所有笔记ID列表（过滤已删除的笔记）
    @Select("SELECT fr.target_id FROM favorite_record fr INNER JOIN note n ON fr.target_id = n.id WHERE fr.user_id = #{userId} AND fr.target_type = 1 ORDER BY fr.create_time DESC")
    List<Integer> findFavoriteNoteIdsByUserId(Integer userId);

    // 删除指定目标的所有收藏记录（笔记删除时级联清理）
    @Delete("DELETE FROM favorite_record WHERE target_type = #{targetType} AND target_id = #{targetId}")
    void deleteByTarget(@Param("targetType") Integer targetType, @Param("targetId") Integer targetId);
}
