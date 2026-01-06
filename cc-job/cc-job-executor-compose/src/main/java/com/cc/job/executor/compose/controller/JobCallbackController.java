package com.cc.job.executor.compose.controller;

import com.cc.job.executor.compose.core.service.TaskWrapperFactory;
import com.cc.job.executor.compose.dto.JobGroupDataRequest;
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
     * <p>请求体格式：
     * <pre>
     * {
     *   "key": "jobId:randomId",
     *   "value": true
     * }
     * </pre>
     * 
     * @param data 任务执行结果数据
     * @return 操作结果
     */
    @PostMapping("/addJobGroupData")
    @ResponseBody
    public Map<String, Object> addJobGroupData(@RequestBody JobGroupDataRequest data) {
        if (data == null || data.getKey() == null || data.getValue() == null) {
            logger.error("[JobCallback] 请求参数无效 - data: {}", data);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("code", 400);
            errorResult.put("message", "请求参数无效：key 和 value 不能为空");
            errorResult.put("data", null);
            return errorResult;
        }
        
        String executeKey = data.getKey();
        Boolean success = data.getValue();
        Object executeResult = data.getExecuteResult();
        
        logger.info("[JobCallback] 收到子任务执行结果 - executeKey: {}, 成功: {}, 执行结果: {}", 
                executeKey, success, executeResult);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            TaskWrapperFactory.getJobResults().put(executeKey, success);
            // 存储执行结果
            if (executeResult != null) {
                TaskWrapperFactory.setJobExecuteResult(executeKey, executeResult);
            }
            
            TaskWrapperFactory.notifyTaskComplete(executeKey);
            
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
