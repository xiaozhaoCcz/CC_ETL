package com.cc.job.executor.compose.interfaces.api;

import com.cc.job.executor.compose.handler.JobGroupExecutorComplete;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 任务组控制API
 * 
 * <p>提供任务组的控制接口，如停止、查询状态等
 *
 * @author cc-job-team
 */
@RestController
@RequestMapping("/api/task-group")
public class TaskGroupControlApi {
    
    private static final Logger logger = LoggerFactory.getLogger(TaskGroupControlApi.class);
    
    private final JobGroupExecutorComplete executor;
    
    public TaskGroupControlApi(JobGroupExecutorComplete executor) {
        this.executor = executor;
    }
    
    /**
     * 停止任务组执行
     * 
     * @param taskGroupId 任务组ID
     * @param batchId 批次ID
     * @return 操作结果
     */
    @PostMapping("/stop")
    public Map<String, Object> stopTaskGroup(
            @RequestParam("taskGroupId") Long taskGroupId,
            @RequestParam("batchId") String batchId) {
        logger.info("[TaskGroupControl] 收到停止请求 - taskGroupId: {}, batchId: {}", 
                taskGroupId, batchId);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            executor.stopJobGroup(taskGroupId, batchId);
            
            result.put("code", 200);
            result.put("message", "任务组已停止");
            result.put("data", null);
            
            logger.info("[TaskGroupControl] 任务组停止成功 - taskGroupId: {}", taskGroupId);
            
        } catch (Exception e) {
            logger.error("[TaskGroupControl] 停止任务组失败 - taskGroupId: {}", taskGroupId, e);
            
            result.put("code", 500);
            result.put("message", "停止失败: " + e.getMessage());
            result.put("data", null);
        }
        
        return result;
    }
    
    /**
     * 查询任务组是否正在运行
     * 
     * @param taskGroupId 任务组ID
     * @param batchId 批次ID
     * @return 查询结果
     */
    @GetMapping("/status")
    public Map<String, Object> getTaskGroupStatus(
            @RequestParam("taskGroupId") Long taskGroupId,
            @RequestParam("batchId") String batchId) {
        logger.debug("[TaskGroupControl] 查询任务组状态 - taskGroupId: {}, batchId: {}", 
                taskGroupId, batchId);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            boolean isRunning = executor.isJobGroupRunning(taskGroupId, batchId);
            
            Map<String, Object> data = new HashMap<>();
            data.put("taskGroupId", taskGroupId);
            data.put("batchId", batchId);
            data.put("isRunning", isRunning);
            
            result.put("code", 200);
            result.put("message", "查询成功");
            result.put("data", data);
            
        } catch (Exception e) {
            logger.error("[TaskGroupControl] 查询任务组状态失败 - taskGroupId: {}", taskGroupId, e);
            
            result.put("code", 500);
            result.put("message", "查询失败: " + e.getMessage());
            result.put("data", null);
        }
        
        return result;
    }
    
    /**
     * 获取所有运行中的任务组
     * 
     * @return 查询结果
     */
    @GetMapping("/running")
    public Map<String, Object> getAllRunningTaskGroups() {
        logger.debug("[TaskGroupControl] 查询所有运行中的任务组");
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            Map<String, Boolean> runningJobs = executor.getAllRunningJobGroups();
            
            result.put("code", 200);
            result.put("message", "查询成功");
            result.put("data", runningJobs);
            
            logger.info("[TaskGroupControl] 查询成功 - 运行中的任务组数: {}", runningJobs.size());
            
        } catch (Exception e) {
            logger.error("[TaskGroupControl] 查询运行中的任务组失败", e);
            
            result.put("code", 500);
            result.put("message", "查询失败: " + e.getMessage());
            result.put("data", null);
        }
        
        return result;
    }
}

