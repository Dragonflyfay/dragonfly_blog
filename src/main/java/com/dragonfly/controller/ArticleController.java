package com.dragonfly.controller;

import com.dragonfly.pojo.Article;
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
}
