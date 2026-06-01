package com.dragonfly.mapper;

import com.dragonfly.pojo.Follow;
import com.dragonfly.pojo.User;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 描述：
 *
 * @param
 * @author 蜻蜓大王
 * @date 2026/6/1 22:31
 */
@Mapper
public interface FollowMapper {
    // 添加关注
    @Insert("INSERT INTO follow (follower_id, following_id, status, create_time) " +
            "VALUES (#{followerId}, #{followingId}, 1, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void add(Follow follow);

    // 取消关注（软删除）
    @Update("UPDATE follow SET status = 0, update_time = NOW() " +
            "WHERE follower_id = #{followerId} AND following_id = #{followingId}")
    void delete(@Param("followerId") Integer followerId,
                @Param("followingId") Integer followingId);

    // 重新关注（恢复）
    @Update("UPDATE follow SET status = 1, update_time = NOW() " +
            "WHERE follower_id = #{followerId} AND following_id = #{followingId}")
    void reactivate(@Param("followerId") Integer followerId,
                    @Param("followingId") Integer followingId);

    // 查询关注关系
    @Select("SELECT * FROM follow WHERE follower_id = #{followerId} AND following_id = #{followingId}")
    Follow findByUsers(@Param("followerId") Integer followerId,
                       @Param("followingId") Integer followingId);

    // 检查是否正在关注
    @Select("SELECT COUNT(*) > 0 FROM follow " +
            "WHERE follower_id = #{followerId} AND following_id = #{followingId} AND status = 1")
    boolean isFollowing(@Param("followerId") Integer followerId,
                        @Param("followingId") Integer followingId);

    // 获取粉丝列表（关注我的人）
    @Select("SELECT u.id, u.username, u.nickname, u.user_pic " +
            "FROM follow f LEFT JOIN user u ON f.follower_id = u.id " +
            "WHERE f.following_id = #{userId} AND f.status = 1")
    List<User> getFollowers(@Param("userId") Integer userId);

    // 获取关注列表（我关注的人）
    @Select("SELECT u.id, u.username, u.nickname, u.user_pic " +
            "FROM follow f LEFT JOIN user u ON f.following_id = u.id " +
            "WHERE f.follower_id = #{userId} AND f.status = 1")
    List<com.dragonfly.pojo.User> getFollowing(@Param("userId") Integer userId);
}
