package com.cc.job.executor.compose.controller;

import com.cc.job.executor.compose.handler.JobGroupExecutorComplete;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 任务组控制接口
 * 
 * <p>提供给 Admin 调用的任务组控制接口，包括：
 * <ul>
 *   <li>停止任务组</li>
 *   <li>查询任务组状态</li>
 * </ul>
 * 
 * @author xiaozhao
 */
@RestController
@RequestMapping("/api/jobgroup")
public class JobGroupControlController {
    
    private static final Logger logger = LoggerFactory.getLogger(JobGroupControlController.class);
    
    private final JobGroupExecutorComplete jobGroupExecutor;
    
    public JobGroupControlController(JobGroupExecutorComplete jobGroupExecutor) {
        this.jobGroupExecutor = jobGroupExecutor;
    }
    
    /**
     * 停止任务组执行
     * 
     * @param jobId 任务组ID
     * @param randomId 批次ID
     * @return 操作结果
     */
    @PostMapping("/stop")
    public Map<String, Object> stopJobGroup(@RequestParam Long jobId, 
                                           @RequestParam String randomId) {
        logger.info("[JobGroupControl] 收到停止任务组请求 - jobId: {}, randomId: {}", jobId, randomId);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            jobGroupExecutor.stopJobGroup(jobId, randomId);
            
            result.put("code", 200);
            result.put("message", "任务组停止成功");
            result.put("data", null);
            
            logger.info("[JobGroupControl] 任务组停止成功 - jobId: {}, randomId: {}", jobId, randomId);
            
        } catch (Exception e) {
            logger.error("[JobGroupControl] 任务组停止失败 - jobId: {}, randomId: {}", jobId, randomId, e);
            
            result.put("code", 500);
            result.put("message", "任务组停止失败: " + e.getMessage());
            result.put("data", null);
        }
        
        return result;
    }
    
    /**
     * 查询任务组状态
     * 
     * @param jobId 任务组ID
     * @param randomId 批次ID
     * @return 任务组状态
     */
    @GetMapping("/status")
    public Map<String, Object> getJobGroupStatus(@RequestParam Long jobId, 
                                                 @RequestParam String randomId) {
        logger.debug("[JobGroupControl] 查询任务组状态 - jobId: {}, randomId: {}", jobId, randomId);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            boolean isRunning = jobGroupExecutor.isJobGroupRunning(jobId, randomId);
            
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", Map.of(
                "jobId", jobId,
                "randomId", randomId,
                "isRunning", isRunning
            ));
            
        } catch (Exception e) {
            logger.error("[JobGroupControl] 查询任务组状态失败 - jobId: {}, randomId: {}", jobId, randomId, e);
            
            result.put("code", 500);
            result.put("message", "查询失败: " + e.getMessage());
            result.put("data", null);
        }
        
        return result;
    }
    
    /**
     * 获取所有运行中的任务组
     * 
     * @return 运行中的任务组列表
     */
    @GetMapping("/running")
    public Map<String, Object> getRunningJobGroups() {
        logger.debug("[JobGroupControl] 查询所有运行中的任务组");
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            Map<String, Boolean> runningJobs = jobGroupExecutor.getAllRunningJobGroups();
            
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", runningJobs);
            
        } catch (Exception e) {
            logger.error("[JobGroupControl] 查询运行中的任务组失败", e);
            
            result.put("code", 500);
            result.put("message", "查询失败: " + e.getMessage());
            result.put("data", null);
        }
        
        return result;
    }
    
    /**
     * 健康检查
     * 
     * @return 健康状态
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "UP");
        result.put("service", "job-group-executor-compose");
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }
    
}
