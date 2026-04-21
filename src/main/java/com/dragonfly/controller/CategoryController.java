package com.dragonfly.controller;

import com.dragonfly.pojo.Category;
import com.dragonfly.pojo.Result;
import com.dragonfly.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public Result add(@RequestBody @Validated Category category){
        categoryService.add(category);
        return Result.success();
    }
}
