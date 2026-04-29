package com.dragonfly.mapper;

import com.dragonfly.pojo.Article;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 描述：
 *
 * @param
 * @author 蜻蜓大王
 * @date 2026/4/22 19:55
 */
@Mapper
public interface ArticleMapper {

    //新增
    @Insert("insert into article (title,summary,content,cover_img,video_url,state,is_top,is_original,source_url,category_id,create_user,publish_time,create_time,update_time) "+
            "values(#{title},#{summary},#{content},#{coverImg},#{videoUrl},#{state},#{isTop},#{isOriginal},#{sourceUrl},#{categoryId},#{createUser},#{publishTime},#{createTime},#{updateTime})")
    void add(Article article);

    //列表查询, 用mapper封装参数
    List<Article> list(Integer userId, Integer categoryId, String state);

    //根据id查询
    @Select("select * from article where id=#{id} and create_user=#{userId}")
    Article findById(Integer id, Integer userId);

    //删除
    @Delete("delete from article where id=#{id} and create_user=#{userId}")
    void delete(Integer id);

    //修改
    @Update("update article set title=#{title},content=#{content},cover_img=#{coverImg},state=#{state},category_id=#{categoryId},update_time=now() where id=#{id} and create_user=#{createUser}")
    void update(Article article);
}
