package com.cc.job.executor.compose.controller;

import cn.hutool.core.lang.Pair;
import com.cc.job.executor.compose.handler.JobGroupExecutorComplete;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 任务回调接口
 * 
 * <p>用于接收子任务执行完成后的回调通知
 * 
 * @author xiaozhao
 */
@RestController
@RequestMapping("/api")
public class JobCallbackController {
    
    private static final Logger logger = LoggerFactory.getLogger(JobCallbackController.class);
    
    /**
     * 接收子任务执行结果
     * 
     * <p>当任务组中的子任务执行完成后，执行器会通过回调将结果发送到这个接口。
     * 接口将结果写入 jobResultMap，供 JobExecutionMonitor 监听使用。
     * 
     * <p>回调路径：{composeAddress}/api/addJobGroupData
     * 
     * @param data 任务执行结果数据 (格式: Pair<String executeKey, Boolean success>)
     * @return 操作结果
     */
    @PostMapping("/addJobGroupData")
    @ResponseBody
    public Map<String, Object> addJobGroupData(@RequestBody Pair<String, Boolean> data) {
        String executeKey = data.getKey();
        Boolean success = data.getValue();
        
        logger.info("[JobCallback] 收到子任务执行结果 - executeKey: {}, 成功: {}", executeKey, success);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 将结果写入 jobResultMap
            JobGroupExecutorComplete.addJobResult(executeKey, success);
            
            result.put("code", 200);
            result.put("message", "任务结果记录成功");
            result.put("data", null);
            
            logger.info("[JobCallback] 任务结果记录成功 - executeKey: {}", executeKey);
            
        } catch (Exception e) {
            logger.error("[JobCallback] 记录任务结果失败 - executeKey: {}", executeKey, e);
            
            result.put("code", 500);
            result.put("message", "记录失败: " + e.getMessage());
            result.put("data", null);
        }
        
        return result;
    }
}
