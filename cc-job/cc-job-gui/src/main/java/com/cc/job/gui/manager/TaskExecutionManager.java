package com.cc.job.gui.manager;

import com.cc.job.gui.model.ProcessNode;
import com.cc.job.gui.model.RunningJobGroup;
import com.cc.job.gui.service.JobInfoService;
import com.cc.job.gui.service.JobLogService;
import com.cc.job.gui.service.SSEService;
import com.cc.job.gui.util.ApiUtil;
import com.cc.job.gui.util.NodeStatusSyncManager;
import com.cc.job.gui.util.NotificationToast;
import com.cc.job.gui.util.SessionManager;
import com.cc.job.gui.util.SnowflakeIdGenerator;
import com.cc.job.gui.view.LogPanel;
import com.cc.job.gui.view.NodeCanvas;
import com.cc.job.gui.view.TaskNavigationBar;
import com.cc.job.gui.view.TopToolBar;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 任务执行管理器 - 负责任务的执行、停止、日志获取等
 */
public class TaskExecutionManager {
    
    private static final Logger logger = LoggerFactory.getLogger(TaskExecutionManager.class);
    
    private final NodeCanvas canvas;
    private final LogPanel logPanel;
    private final TaskNavigationBar navigationBar;
    private final TopToolBar toolBar;
    
    private final JobInfoService jobInfoService;
    private final JobLogService jobLogService;
    private final SnowflakeIdGenerator snowflake;
    
    private final Map<Long, RunningJobGroup> runningJobs = new HashMap<>();
    private Map<String, String[]> predictedNodeTimes;
    
    public TaskExecutionManager(NodeCanvas canvas, LogPanel logPanel, 
                                TaskNavigationBar navigationBar, TopToolBar toolBar) {
        this.canvas = canvas;
        this.logPanel = logPanel;
        this.navigationBar = navigationBar;
        this.toolBar = toolBar;
        this.jobInfoService = new JobInfoService();
        this.jobLogService = new JobLogService();
        this.snowflake = SnowflakeIdGenerator.getInstance();
    }
    
    /**
     * 触发任务执行
     */
    public void triggerJobExecution(Long currentJobId, String jobName) {
        if (currentJobId == null || currentJobId == 0) {
            NotificationToast.showWarning("未选择任务组");
            toolBar.setRunButtonLoading(false);
            return;
        }
        
        // 检查本地状态
        RunningJobGroup existingJob = runningJobs.get(currentJobId);
        if (existingJob != null && existingJob.isRunning()) {
           NotificationToast.show("任务组"+ currentJobId + " 正在运行中，请稍后再试",
               NotificationToast.NotificationType.WARNING);
            toolBar.setRunButtonLoading(false);
            return;
        }
        
        // 在后台线程中检查服务器状态
        new Thread(() -> {
            try {
                boolean isRunningOnServer = jobInfoService.getJobStatus(currentJobId);
                
                Platform.runLater(() -> {
                    if (isRunningOnServer) {
                        NotificationToast.show("任务组"+ currentJobId + " 正在运行中，请稍后再试",
                           NotificationToast.NotificationType.WARNING);
                        logPanel.info("提示：该任务组可能正在其他客户端或服务器实例上运行");
                        toolBar.setRunButtonLoading(false);
                        
                        // ⭐ 从服务器同步节点状态（任务可能正在运行）
                        canvas.syncPendingNodeStatus();
                        return;
                    }
                    continueJobExecution(currentJobId, jobName);
                });
            } catch (Exception e) {
                logger.error("❌ 检查任务组运行状态失败: {}", e.getMessage(), e);
                Platform.runLater(() -> {
                    logPanel.warn("⚠ 检查任务组运行状态失败: " + e.getMessage());
                    logPanel.warn("为安全起见，取消本次任务启动");
                    toolBar.setRunButtonLoading(false);
                    
                    // ⭐ 检查失败时，同步节点状态（显示实际状态）
                    canvas.syncPendingNodeStatus();
                });
            }
        }).start();
    }
    
    private void continueJobExecution(Long currentJobId, String jobName) {
        String randomId = snowflake.nextIdStr();
        String currentUserId = SessionManager.getInstance().getUserId();
        
        RunningJobGroup runningJob = new RunningJobGroup(currentJobId, jobName, randomId, currentUserId);
        runningJobs.put(currentJobId, runningJob);
        
        // 重新执行任务组时，清空该任务组的历史日志并切换到该标签页
        logPanel.restartTaskGroup(currentJobId, jobName);
        logPanel.info(currentJobId, "════════════════════════════════");
        logPanel.success(currentJobId, "✨ 开始执行任务组: " + jobName + " (ID: " + currentJobId + ")");
        logPanel.info(currentJobId, "执行批次ID: " + randomId);
        
        navigationBar.updateTaskGroupRunningStatus(currentJobId, true);
        updateToolBarRunningJobs();
        canvas.setAllConnectionsRunning(true);
        
        // 连接SSE
        SSEService.getInstance().connect(currentJobId, randomId, message -> {
            handleSSEMessage(message, randomId);
        });
        
        // 触发任务
        new Thread(() -> {
            try {
                Long logId = jobInfoService.triggerJob(currentJobId, randomId);
                runningJob.setLogId(logId);
                
                Platform.runLater(() -> {
                    logPanel.success("✓ 任务已提交，日志ID: " + logId);
                    
                    // 任务提交成功后，同步节点状态（后端已将所有子节点状态设置为-1）
                    canvas.syncPendingNodeStatus();
                });
                
                startLogPolling(runningJob);
                
            } catch (Exception e) {
                logger.error("触发任务执行失败: {}", e.getMessage(), e);
                Platform.runLater(() -> {
                    logPanel.error("✗ 任务执行失败: " + e.getMessage());
                    
                    // ⭐ 任务触发失败时，清除节点状态缓存并刷新显示
                    NodeStatusSyncManager.getInstance().clearCacheForTaskGroupSwitch();
                    
                    // ⭐ 将所有节点状态设置为失败（红色）
                    for (ProcessNode node : canvas.getNodes()) {
                        node.updateStatus(ProcessNode.NodeStatus.FAILED);
                    }
                    
                    runningJob.cleanup();
                    runningJobs.remove(currentJobId);
                    updateToolBarRunningJobs();
                    canvas.setAllConnectionsRunning(false);
                    toolBar.setRunButtonLoading(false);
                });
            }
        }).start();
    }
    
    /**
     * 停止任务执行
     */
    public void stopJobExecution(Long jobId) {
        if (jobId == null || jobId == 0) {
            logPanel.warn("⚠ 无效的任务组ID");
            return;
        }
        
        RunningJobGroup runningJob = runningJobs.get(jobId);
        if (runningJob == null || !runningJob.isRunning()) {
            logPanel.warn("⚠ 任务组 " + jobId + " 当前没有运行");
            return;
        }
        
        String jobName = runningJob.getJobName();
        String randomId = runningJob.getRandomId();
        
        // 先调用后台停止接口
        try {
            jobInfoService.stopJobCompose(jobId, randomId);
            logger.debug("已调用后台停止接口 - jobId: {}, randomId: {}", jobId, randomId);
        } catch (Exception e) {
            logger.error("后台停止接口调用失败 - jobId: {}, randomId: {}", jobId, randomId, e);
        }
        
        // 更新UI状态
        Platform.runLater(() -> {
            logPanel.success("✓ 任务组 " + jobName + " 已停止");
            runningJob.cleanup();
            runningJobs.remove(jobId);
            navigationBar.updateTaskGroupRunningStatus(jobId, false);
            updateToolBarRunningJobs();
            canvas.setAllConnectionsRunning(false);
            canvas.syncPendingNodeStatus();
        });
        
        // 异步断开SSE连接，确保使用正确的randomId
        new Thread(() -> {
            try {
                // 短暂延迟，确保UI更新完成
                Thread.sleep(100);
                // 断开当前randomId对应的SSE连接
                SSEService.getInstance().disconnect(jobId, randomId);
                logger.debug("已断开SSE连接 - jobId: {}, randomId: {}", jobId, randomId);
            } catch (Exception e) {
                logger.error("异步断开SSE连接失败 - jobId: {}, randomId: {}", jobId, randomId, e);
            }
        }, "SSE-Disconnect-Stop-" + jobId + "-" + randomId).start();
    }
    
    private void handleSSEMessage(SSEService.SSEMessage message, String expectedRandomId) {
        if (message.getStatus() != null && message.getStatus() == -1) {
            logPanel.error("❌ SSE连接错误: " + message.getResult());
            return;
        }
        
        if (message.getRandomId() != null && !expectedRandomId.equals(message.getRandomId())) {
            return;
        }
        
        Long jobId = message.getJobId();
        Integer status = message.getStatus();
        
        // 处理预测时间（status=9）
        if (status != null && status == 9 && message.getResult() != null) {
            try {
                String[][] times = ApiUtil.getInstance().getGson()
                    .fromJson(message.getResult(), String[][].class);
                if (times != null) {
                    ensurePredictedTimeCache();
                    for (String[] row : times) {
                        if (row != null && row.length >= 3 && row[0] != null) {
                            predictedNodeTimes.put(row[0], new String[]{row[1], row[2]});
                        }
                    }
                }
            } catch (Exception e) {
                // 忽略解析错误
            }
        }
        
        // 更新节点状态
        if (jobId != null && status != null) {
            Platform.runLater(() -> {
                canvas.updateNodeStatusByJobId(jobId, status);
            });
        }
        
        // 任务完成处理（status=5）
        if (status != null && status == 5) {
            RunningJobGroup runningJob = jobId != null ? runningJobs.get(jobId) : null;
            if (runningJob != null && expectedRandomId.equals(runningJob.getRandomId())) {
                Timer delayTimer = new Timer("DelayStopTimer-" + runningJob.getJobId(), true);
                delayTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        Platform.runLater(() -> {
                            stopLogPolling(runningJob, "任务组执行完成");
                        });
                        delayTimer.cancel();
                    }
                }, 2000);
            }
        }
    }
    
    private void startLogPolling(RunningJobGroup runningJob) {
        Timer logTimer = new Timer("LogPollingTimer-" + runningJob.getJobId(), true);
        runningJob.setLogTimer(logTimer);
        
        logTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                fetchExecutionLog(runningJob);
            }
        }, 1000, 3000);
    }
    
    private void fetchExecutionLog(RunningJobGroup runningJob) {
        if (runningJob.getLogId() == null || runningJob.getPullFailCount() > 20) {
            return;
        }
        
        try {
            JobLogService.LogDetailResponse response = jobLogService.getLogDetail(
                runningJob.getLogId(), runningJob.getFromLineNum());
            
            if (response != null && response.isSuccess()) {
                JobLogService.LogContent content = response.getContent();
                if (content != null && content.getLogContent() != null && !content.getLogContent().isEmpty()) {
                    runningJob.setFromLineNum(content.getToLineNum() + 1);
                    runningJob.setPullFailCount(0);
                    
                    Platform.runLater(() -> {
                        logPanel.appendText(runningJob.getJobId(), 
                            "[" + runningJob.getJobName() + "] " + content.getLogContent());
                    });
                }
                
                if (content != null && content.isEnd()) {
                    stopLogPolling(runningJob, "任务执行完成");
                }
            } else {
                runningJob.setPullFailCount(runningJob.getPullFailCount() + 1);
            }
        } catch (Exception e) {
            runningJob.setPullFailCount(runningJob.getPullFailCount() + 1);
        }
    }
    
    private void stopLogPolling(RunningJobGroup runningJob, String message) {
        runningJob.cleanup();
        
        Long jobId = runningJob.getJobId();
        String jobName = runningJob.getJobName();
        String randomId = runningJob.getRandomId();
        
        Platform.runLater(() -> {
            logPanel.success(jobId, "✓ " + jobName + " " + message);
            runningJobs.remove(jobId);
            navigationBar.updateTaskGroupRunningStatus(jobId, false);
            updateToolBarRunningJobs();
            canvas.setAllConnectionsRunning(false);
            canvas.syncPendingNodeStatus();
        });
        
        // 异步断开SSE连接，确保使用正确的randomId
        // 使用线程池异步执行，避免阻塞
        new Thread(() -> {
            try {
                // 短暂延迟，确保UI更新完成
                Thread.sleep(100);
                // 断开当前randomId对应的SSE连接
                SSEService.getInstance().disconnect(jobId, randomId);
                logger.debug("已断开SSE连接 - jobId: {}, randomId: {}", jobId, randomId);
            } catch (Exception e) {
                logger.error("异步断开SSE连接失败 - jobId: {}, randomId: {}", jobId, randomId, e);
            }
        }, "SSE-Disconnect-" + jobId + "-" + randomId).start();
    }
    
    private void updateToolBarRunningJobs() {
        Map<Long, RunningJobGroup> activeJobs = new HashMap<>();
        for (Map.Entry<Long, RunningJobGroup> entry : runningJobs.entrySet()) {
            if (entry.getValue().isRunning()) {
                activeJobs.put(entry.getKey(), entry.getValue());
            }
        }
        toolBar.updateRunningJobs(activeJobs);
        navigationBar.updateRunningJobs(activeJobs);
    }
    
    private void ensurePredictedTimeCache() {
        if (predictedNodeTimes == null) {
            predictedNodeTimes = new ConcurrentHashMap<>();
        }
    }
    
    public Map<String, String[]> getPredictedNodeTimes() {
        return predictedNodeTimes;
    }
    
    /**
     * 检查任务组是否正在运行
     * @param taskGroupId 任务组ID
     * @return 是否正在运行
     */
    public boolean isTaskGroupRunning(Long taskGroupId) {
        if (taskGroupId == null) {
            return false;
        }
        RunningJobGroup runningJob = runningJobs.get(taskGroupId);
        return runningJob != null && runningJob.isRunning();
    }
    
    public void cleanup() {
        for (Map.Entry<Long, RunningJobGroup> entry : new HashMap<>(runningJobs).entrySet()) {
            RunningJobGroup runningJob = entry.getValue();
            try {
                runningJob.cleanup();
                SSEService.getInstance().disconnect(runningJob.getJobId(), runningJob.getRandomId());
            } catch (Exception e) {
                logger.error("清理任务组失败 - jobId: {}", runningJob.getJobId(), e);
            }
        }
        runningJobs.clear();
    }
}

