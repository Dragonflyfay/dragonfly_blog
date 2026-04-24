package com.dragonfly.controller;

import com.dragonfly.pojo.Article;
import com.dragonfly.pojo.PageBean;
import com.dragonfly.pojo.Result;
import com.dragonfly.service.ArticleService;
import com.dragonfly.utils.JwtUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 描述：
 *
 * @param
 * @author 蜻蜓大王
 * @date 2026/4/6 18:16
 */
@RestController
@RequestMapping("/article")
public class ArticleController {
    @Autowired
    private ArticleService articleService;

    @PostMapping
    public Result add(@RequestBody @Validated Article article){//接受前端的参数,将JSON数据转换成Article对象
        articleService.add(article);
        return Result.success();
    }

    @GetMapping
    public Result<PageBean<Article>> list(
            Integer pageNum,
            Integer pageSize,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false)String state
    ){
        PageBean<Article>   pb=articleService.list( pageNum, pageSize, categoryId, state);
        return Result.success(pb);
    }

    //获取文章详情
    @GetMapping("/detail")
    public Result<Article> detail(Integer id){
        Article article=articleService.detail(id);
        return Result.success(article);

    }
    //删除文章
    @DeleteMapping
    public Result delete(Integer  id){
        articleService.delete(id);
        return Result.success();
    }
    //更新修改文章
    @PutMapping
    public Result update(@RequestBody @Validated Article article){
        articleService.update(article);
        return Result.success();
    }
}
