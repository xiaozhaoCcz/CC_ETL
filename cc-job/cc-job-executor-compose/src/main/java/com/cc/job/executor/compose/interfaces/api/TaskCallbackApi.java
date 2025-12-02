package com.cc.job.executor.compose.interfaces.api;

import com.cc.job.executor.compose.core.service.TaskWrapperFactory;
import com.cc.job.executor.compose.interfaces.dto.TaskResultCallbackRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 任务回调API
 * 
 * <p>接收子任务执行完成后的回调通知
 *
 * @author cc-job-team
 */
@RestController
@RequestMapping("/api/task-callback")
public class TaskCallbackApi {
    
    private static final Logger logger = LoggerFactory.getLogger(TaskCallbackApi.class);
    
    /**
     * 接收子任务执行结果
     * 
     * <p>当任务组中的子任务执行完成后，执行器会回调此接口
     * 
     * @param request 回调请求
     * @return 操作结果
     */
    @PostMapping("/result")
    public Map<String, Object> receiveTaskResult(@RequestBody TaskResultCallbackRequest request) {
        if (request == null || request.getExecuteKey() == null || request.getSuccess() == null) {
            logger.error("[TaskCallback] 请求参数无效 - request: {}", request);
            return buildErrorResponse(400, "请求参数无效");
        }
        
        String executeKey = request.getExecuteKey();
        Boolean success = request.getSuccess();
        
        logger.info("[TaskCallback] 收到任务执行结果 - executeKey: {}, 成功: {}", 
                executeKey, success);
        
        try {
            TaskWrapperFactory.getJobResults().put(executeKey, success);
            logger.info("[TaskCallback] 任务结果记录成功 - executeKey: {}", executeKey);
            return buildSuccessResponse("任务结果记录成功");
            
        } catch (Exception e) {
            logger.error("[TaskCallback] 记录任务结果失败 - executeKey: {}", executeKey, e);
            return buildErrorResponse(500, "记录失败: " + e.getMessage());
        }
    }
    
    /**
     * 兼容旧接口（保留用于向后兼容）
     */
    @PostMapping("/addJobGroupData")
    @Deprecated
    public Map<String, Object> addJobGroupData(@RequestBody Map<String, Object> data) {
        String key = (String) data.get("key");
        Boolean value = (Boolean) data.get("value");
        
        TaskResultCallbackRequest request = new TaskResultCallbackRequest();
        request.setExecuteKey(key);
        request.setSuccess(value);
        
        return receiveTaskResult(request);
    }
    
    /**
     * 构建成功响应
     */
    private Map<String, Object> buildSuccessResponse(String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", message);
        result.put("data", null);
        return result;
    }
    
    /**
     * 构建错误响应
     */
    private Map<String, Object> buildErrorResponse(int code, String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", code);
        result.put("message", message);
        result.put("data", null);
        return result;
    }
}

