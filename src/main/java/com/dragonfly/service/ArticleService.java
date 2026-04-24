package com.dragonfly.service;

import com.dragonfly.pojo.Article;
import com.dragonfly.pojo.PageBean;

/**
 * 描述：
 *
 * @param
 * @author 蜻蜓大王
 * @date 2026/4/22 19:54
 */
public interface ArticleService {

    //新增文章
    void add(Article article);

    //查询文章,条件分页列表查询
    PageBean<Article> list(Integer pageNum, Integer pageSize, Integer categoryId, String state);

    //根据id查询文章详情
    Article detail(Integer id);

    //删除文章
    void delete(Integer id);

    //修改文章，更新文章
    void update(Article article);
}
