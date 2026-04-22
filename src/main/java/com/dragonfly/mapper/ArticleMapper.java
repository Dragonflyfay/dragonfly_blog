package com.dragonfly.mapper;

import com.dragonfly.pojo.Article;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

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
    @Insert("insert into article (title,content,cover_img,state,category_id,create_user,create_time,update_time)"+
            "values(#{title},#{content},#{coverImg},#{state},#{categoryId},#{createUser},#{createTime},#{updateTime})")
    void add(Article article);
}
