package com.dragonfly.mapper;

import com.dragonfly.pojo.Category;
import org.apache.ibatis.annotations.*;

import java.util.List;

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


    //查询所有
    @Select("select *from category where create_user=#{userId}")
    List<Category> list(Integer userId);

    //根据id查询
    @Select("select *from category where id=#{id}")
    Category findById(Integer id);

    //更新
    @Update("update category set category_name=#{categoryName},category_alias=#{categoryAlias},update_time=#{updateTime} where id=#{id}")
    void update(Category category);

    //删除分类
    @Delete("delete from category where id=#{id} and create_user=#{userId}")
    void delete(Integer id, Integer userId);
}
