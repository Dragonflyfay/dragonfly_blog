package com.dragonfly.service.impl;

import com.dragonfly.mapper.OperationLogMapper;
import com.dragonfly.pojo.OperationLog;
import com.dragonfly.service.OperationLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 描述：
 *
 * @param
 * @author 蜻蜓大王
 * @date 2026/8/20 15:29
 */
@Service
public class OperationLogServiceImpl implements OperationLogService {

    @Autowired
    private OperationLogMapper operationLogMapper;

    @Override
    public Map<String, Object> list(String operator, String module, String startDate, String endDate, Integer pageNum, Integer pageSize) {
        int offset = (pageNum - 1) * pageSize;
        List<OperationLog> items = operationLogMapper.list(operator, module, startDate, endDate, offset, pageSize);
        int total = operationLogMapper.count(operator, module, startDate, endDate);

        Map<String, Object> result = new HashMap<>();
        result.put("items", items);
        result.put("total", total);
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);
        return result;
    }

    @Override
    public void log(String operator, String module, String action, String detail, String ip) {
        OperationLog log = new OperationLog();
        log.setOperator(operator);
        log.setModule(module);
        log.setAction(action);
        log.setDetail(detail);
        log.setIp(ip);
        log.setCreateTime(LocalDateTime.now());
        operationLogMapper.insert(log);
    }
}
