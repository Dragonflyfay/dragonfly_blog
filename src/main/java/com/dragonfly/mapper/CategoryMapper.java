package com.dragonfly.mapper;

import com.dragonfly.pojo.Category;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

/**
 * 描述：
 *
 * @param
 * @author 蜻蜓大王
 * @date 2026/4/21 19:34
 */
@Mapper
public interface CategoryMapper {
    //新增分类
    @Insert("insert into category (category_name,category_alias,create_user,create_time,update_time) "+
    "values(#{categoryName},#{categoryAlias},#{createUser},#{createTime},#{updateTime})")
    void add(Category category);
}
