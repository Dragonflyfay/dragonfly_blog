package com.dragonfly.service.impl;

import com.dragonfly.mapper.CategoryMapper;
import com.dragonfly.pojo.Category;
import com.dragonfly.service.CategoryService;
import com.dragonfly.utils.ThreadLocalUtil;
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
 * @date 2026/4/21 19:33
 */
@Service
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;
    @Override
    public void add(Category category) {

        //补充属性值
        category.setCreateTime(LocalDateTime.now());
        category.setUpdateTime(LocalDateTime.now());

        Map<String,Object> map= ThreadLocalUtil.get();
        Integer userId=(Integer)map.get("id");
        category.setCreateUser(userId);
        categoryMapper.add(category);
    }

    @Override
    public List<Category> list() {
        Map<String,Object> map= ThreadLocalUtil.get();
        Integer userId =(Integer) map.get("id");

        return categoryMapper.list(userId);
    }

    @Override
    public Category detail(Integer id) {
        Category c =categoryMapper.findById(id);
        return c;
    }

    @Override
    public void update(Category category) {
        category.setUpdateTime(LocalDateTime.now());
        categoryMapper.update(category);
    }

    @Override
    public void delete(Integer id) {
        Map<String,Object> map=ThreadLocalUtil.get();
        Integer userId=(Integer)map.get("id");
        //
        Category category=categoryMapper.findById(id);
        if(category==null){
            throw new RuntimeException("要删除的分类不存在");
        }
        if(!category.getCreateUser().equals(userId)){
            throw new RuntimeException("没有权限,只能删除自己创建的分类");
        }
        categoryMapper.delete(id,userId);
    }
}
