package com.dragonfly.service;

import java.util.Map;

/**
 * 描述：
 *
 * @param
 * @author 蜻蜓大王
 * @date 2026/8/20 15:28
 */
public interface OperationLogService {
    /**
     * 获取操作日志列表（分页）
     */
    Map<String, Object> list(String operator, String module, String startDate, String endDate, Integer pageNum, Integer pageSize);

    /**
     * 记录操作日志（供 AOP 或手动调用）
     */
    void log(String operator, String module, String action, String detail, String ip);
}
