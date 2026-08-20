package com.dragonfly.controller;

import com.dragonfly.pojo.Result;
import com.dragonfly.service.OperationLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 描述：操作日志
 *
 * @param
 * @author 蜻蜓大王
 * @date 2026/8/20 15:27
 */
@RestController
@RequestMapping("/admin/logs")
public class OperationLogController {

    @Autowired
    private OperationLogService operationLogService;

    /**
     * 获取操作日志列表
     */
    @GetMapping
    public Result<Map<String, Object>> list(
            @RequestParam(required = false) String operator,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {

        Map<String, Object> result = operationLogService.list(operator, module, startDate, endDate, pageNum, pageSize);
        return Result.success(result);
    }
}
