package com.dragonfly.service;

import com.dragonfly.pojo.Category;

import java.util.List;

/**
 * 描述：
 *
 * @param
 * @author 蜻蜓大王
 * @date 2026/4/21 19:32
 */
public interface CategoryService {
    //新增分类
    void add(Category category);

    //列表查询
    List<Category> list();

    //根据id查询分类信息
    Category detail(Integer id);

    //更新分类信息
    void update(Category category);

    //删除分类
    void delete(Integer id);
}
