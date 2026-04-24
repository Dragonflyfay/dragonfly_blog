package com.dragonfly.service.impl;

import com.dragonfly.mapper.ArticleMapper;
import com.dragonfly.pojo.Article;
import com.dragonfly.pojo.PageBean;
import com.dragonfly.service.ArticleService;
import com.dragonfly.utils.ThreadLocalUtil;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 描述：
 *
 * @param
 * @author 蜻蜓大王
 * @date 2026/4/22 19:55
 */
@Service
public class ArticleImpl implements ArticleService {

    @Autowired
    private ArticleMapper articleMapper;

    @Override
    public void add(Article article) {
        //补充属性值
        article.setCreateTime(LocalDateTime.now());
        article.setUpdateTime(LocalDateTime.now());

        Map<String,Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("id");
        article.setCreateUser(userId);
        articleMapper.add(article);
    }

    //查询文章,条件分页列表查询
    @Override
    public PageBean<Article> list(Integer pageNum, Integer pageSize, Integer categoryId, String state) {
        //1.创建PageBean对象
        PageBean<Article> pb=new PageBean<>();


        //2.开启分页查询 PageHelper
        PageHelper.startPage(pageNum,pageSize);//设置分页参数
        //3.调用mapper查询
        Map<String,Object> map=ThreadLocalUtil.get();
        Integer userId=(Integer) map.get("id");
        List<Article> as=articleMapper.list(userId,categoryId,state);
        //Page中提供了方法，可以获取PageHelper分页查询后得到的总记录条数和当前页数据
        //数据对象
        Page<Article> p=(Page<Article>)as;//List作为 Page的父类,无法调用Page中的方法

        //把数据填充到到PageBean中
        pb.setTotal(p.getTotal());
        //当前页数据
        pb.setItems(p.getResult());
        return pb;
    }

    @Override
    public Article detail(Integer id) {
        Map<String,Object> map=ThreadLocalUtil.get();
        Integer userId=(Integer)map.get("id");

        Article article=articleMapper.findById(id,userId);

        return article;
    }

    @Override
    public void delete(Integer id) {
        //1.获取文章
        Map<String,Object> map=ThreadLocalUtil.get();
        Integer userId=(Integer)map.get("id");

        Article article=articleMapper.findById(id,userId);
        if(article==null){
            throw new RuntimeException("要删除的文章不存在");
        }
        if(!article.getCreateUser().equals(userId)){
            throw new RuntimeException("没有权限,只能删除自己创建的文章");
        }
        articleMapper.delete(id);

    }

    @Override
    public void update(Article article) {
        Map<String,Object> map=ThreadLocalUtil.get();
        Integer userId=(Integer)map.get("id");
        //验证机制
        Article a=articleMapper.findById(article.getId(),userId);
        if(a==null){
            throw new RuntimeException("要修改的文章不存在");
        }
        if(!a.getCreateUser().equals(userId)){
            throw new RuntimeException("没有权限,只能修改自己创建的文章");
        }
        articleMapper.update(article);
    }
}
