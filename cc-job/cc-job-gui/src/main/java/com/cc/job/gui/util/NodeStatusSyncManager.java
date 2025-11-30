package com.cc.job.gui.util;

import com.cc.job.gui.service.JobInfoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

/**
 * 节点状态同步管理器
 * 负责批量更新节点状态，避免频繁调用后端API
 */
public class NodeStatusSyncManager {
    
    private static final Logger logger = LoggerFactory.getLogger(NodeStatusSyncManager.class);
    
    private static NodeStatusSyncManager instance;
    
    // 待更新的节点状态 Map<jobId, triggerStatus>
    private final Map<Long, Integer> pendingUpdates = new ConcurrentHashMap<>();

    // 最近一次收到的节点状态缓存（即使未落库也可读取）
    private final Map<Long, Integer> latestStatusCache = new ConcurrentHashMap<>();
    
    // 线程池
    private final ScheduledExecutorService executorService;
    
    // API服务
    private final JobInfoService jobInfoService;
    
    // 定时同步间隔（秒）
    private static final int SYNC_INTERVAL_SECONDS = 5;
    
    // 批量更新任务
    private ScheduledFuture<?> syncTask;
    
    private NodeStatusSyncManager() {
        this.executorService = Executors.newScheduledThreadPool(1, r -> {
            Thread thread = new Thread(r, "NodeStatusSync-Thread");
            thread.setDaemon(true);
            return thread;
        });
        this.jobInfoService = new JobInfoService();
        
        // 启动定时同步任务
        startPeriodicSync();
        
        // ⭐ 添加JVM关闭钩子，确保异常退出时也能同步节点状态
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (!pendingUpdates.isEmpty()) {
                
                // 直接调用同步方法（不通过线程池）
                Map<Long, Integer> updatesToSync = new HashMap<>(pendingUpdates);
                try {
                    jobInfoService.batchUpdateNodeStatus(updatesToSync);
                } catch (Exception e) {
                    logger.error("❌ 紧急同步失败: {}", e.getMessage(), e);
                }
            }
        }, "NodeStatusSync-ShutdownHook"));
        
    }
    
    public static synchronized NodeStatusSyncManager getInstance() {
        if (instance == null) {
            instance = new NodeStatusSyncManager();
        }
        return instance;
    }
    
    /**
     * 添加待更新的节点状态
     * @param jobId 任务ID
     * @param triggerStatus 运行状态
     */
    public void addPendingUpdate(Long jobId, Integer triggerStatus) {
        if (jobId == null || triggerStatus == null) {
            return;
        }

        pendingUpdates.put(jobId, triggerStatus);
        latestStatusCache.put(jobId, triggerStatus);
    }
    
    /**
     * 启动定时同步任务
     */
    private void startPeriodicSync() {
        syncTask = executorService.scheduleWithFixedDelay(
            this::syncPendingUpdatesInternal,
            SYNC_INTERVAL_SECONDS,
            SYNC_INTERVAL_SECONDS,
            TimeUnit.SECONDS
        );
    }
    
    /**
     * 立即同步所有待更新的节点状态
     */
    public void syncNow() {
        executorService.execute(this::syncPendingUpdatesInternal);
    }

    /**
     * 立即同步所有待更新的节点状态（阻塞当前线程）
     */
    public void syncNowBlocking() {
        syncPendingUpdatesInternal();
    }
    
    /**
     * 同步待更新的节点状态（批量更新）
     */
    private void syncPendingUpdatesInternal() {
        Map<Long, Integer> updatesToSync = drainPendingUpdates();
        if (updatesToSync.isEmpty()) {
            return;
        }


        try {
            jobInfoService.batchUpdateNodeStatus(updatesToSync);
        } catch (Exception e) {
            logger.error("❌ 批量同步节点状态失败: {}", e.getMessage(), e);
            requeuePendingUpdates(updatesToSync);
        }
    }
    
    /**
     * 获取待更新的节点数量
     */
    public int getPendingUpdateCount() {
        return pendingUpdates.size();
    }
    
    /**
     * 清空所有待更新的节点状态
     */
    public void clearPendingUpdates() {
        pendingUpdates.clear();
    }

    /**
     * 清空缓存（用于任务组切换时，避免不同任务组之间的状态冲突）
     */
    public void clearCacheForTaskGroupSwitch() {
        latestStatusCache.clear();
    }
    
    /**
     * 停止同步任务
     * 确保所有待更新的节点状态都已同步到数据库
     * ⭐ 优化：添加超时保护，避免长时间阻塞
     */
    public void shutdown() {
        // 取消定时任务（立即取消，不等待）
        if (syncTask != null) {
            syncTask.cancel(false);
        }
        
        // ⭐ 快速关闭：使用超时保护，避免网络API调用阻塞
        // 先尝试同步一次，但设置超时保护
        if (!pendingUpdates.isEmpty()) {
            try {
                // 使用Future来设置超时
                java.util.concurrent.Future<?> syncFuture = executorService.submit(() -> {
                    syncPendingUpdatesInternal();
                });
                
                // 最多等待1秒
                try {
                    syncFuture.get(1, TimeUnit.SECONDS);
                } catch (java.util.concurrent.TimeoutException e) {
                    // 超时，取消同步任务
                    syncFuture.cancel(true);
                    logger.warn("节点状态同步超时，已取消");
                } catch (Exception e) {
                    logger.error("节点状态同步失败: {}", e.getMessage());
                }
            } catch (Exception e) {
                logger.error("提交同步任务失败: {}", e.getMessage());
            }
        }
        
        // 快速关闭线程池（不等待任务完成）
        executorService.shutdown();
        try {
            // 只等待500毫秒，然后强制关闭
            if (!executorService.awaitTermination(500, TimeUnit.MILLISECONDS)) {
                executorService.shutdownNow();
                // 再等待100毫秒
                if (!executorService.awaitTermination(100, TimeUnit.MILLISECONDS)) {
                    logger.warn("线程池未能正常关闭，已强制关闭");
                }
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private Map<Long, Integer> drainPendingUpdates() {
        if (pendingUpdates.isEmpty()) {
            return java.util.Collections.emptyMap();
        }
        synchronized (pendingUpdates) {
            if (pendingUpdates.isEmpty()) {
                return java.util.Collections.emptyMap();
            }
            Map<Long, Integer> snapshot = new HashMap<>(pendingUpdates);
            pendingUpdates.clear();
            return snapshot;
        }
    }

    private void requeuePendingUpdates(Map<Long, Integer> updates) {
        if (updates == null || updates.isEmpty()) {
            return;
        }
        synchronized (pendingUpdates) {
            pendingUpdates.putAll(updates);
        }
    }

    /**
     * 获取缓存中的节点状态（可能尚未落库）
     */
    public Integer getCachedStatus(Long jobId) {
        if (jobId == null) {
            return null;
        }
        return latestStatusCache.get(jobId);
    }

    /**
     * 记忆某个节点的运行状态（不会加入待同步队列）
     */
    public void rememberStatus(Long jobId, Integer triggerStatus) {
        if (jobId == null || triggerStatus == null) {
            return;
        }
        latestStatusCache.put(jobId, triggerStatus);
    }
}

