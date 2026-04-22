package com.dragonfly.controller;

import com.dragonfly.pojo.Category;
import com.dragonfly.pojo.Result;
import com.dragonfly.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 描述：
 *
 * @param
 * @author 蜻蜓大王
 * @date 2026/4/21 19:31
 */
@RestController
@RequestMapping("/category")
public class CategoryController {
    //注入对象
    @Autowired
    private CategoryService categoryService;
    @PostMapping
    public Result add(@RequestBody @Validated(Category.Add.class) Category category){
        categoryService.add(category);
        return Result.success();
    }
    @GetMapping
    public Result<List<Category>> list() {
        List<Category> cs=categoryService.list();
        return Result.success(cs);
    }
    @GetMapping("/detail")
    public Result<Category> detail(Integer id){//接受前端的参数
        Category category=categoryService.detail(id);
        return Result.success(category);

    }

    @PutMapping
    public Result update(@RequestBody @Validated(Category.Update.class) Category category){
        categoryService.update(category);
        return Result.success();

    }

    @DeleteMapping
    public Result delete(Integer id){
        categoryService.delete(id);
        return Result.success();
    }
}
