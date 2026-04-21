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
}
