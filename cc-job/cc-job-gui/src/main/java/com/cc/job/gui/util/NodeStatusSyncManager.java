package com.cc.job.gui.util;

import com.cc.job.gui.service.JobInfoService;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

/**
 * 节点状态同步管理器
 * 负责批量更新节点状态，避免频繁调用后端API
 */
public class NodeStatusSyncManager {
    
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
                System.out.println("\n════════════════════════════════");
                System.out.println("🚨 JVM关闭钩子触发 - 检测到应用异常退出");
                System.out.println("⚠️ 正在紧急同步 " + pendingUpdates.size() + " 个节点状态...");
                
                // 直接调用同步方法（不通过线程池）
                Map<Long, Integer> updatesToSync = new HashMap<>(pendingUpdates);
                try {
                    jobInfoService.batchUpdateNodeStatus(updatesToSync);
                    System.out.println("✅ 紧急同步成功");
                } catch (Exception e) {
                    System.err.println("❌ 紧急同步失败: " + e.getMessage());
                }
                System.out.println("════════════════════════════════\n");
            }
        }, "NodeStatusSync-ShutdownHook"));
        
        System.out.println("✓ JVM关闭钩子已注册（确保异常退出时同步节点状态）");
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
        System.out.println("📝 添加待更新节点状态: jobId=" + jobId + ", triggerStatus=" + triggerStatus +
                " (待更新数量: " + pendingUpdates.size() + ")");
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
        System.out.println("✓ 节点状态定时同步任务已启动 (间隔: " + SYNC_INTERVAL_SECONDS + "秒)");
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

        System.out.println("🔄 开始批量同步节点状态，共 " + updatesToSync.size() + " 个节点");

        try {
            jobInfoService.batchUpdateNodeStatus(updatesToSync);
            System.out.println("✅ 批量同步节点状态成功: " + updatesToSync.size() + " 个节点");
        } catch (Exception e) {
            System.err.println("❌ 批量同步节点状态失败: " + e.getMessage());
            e.printStackTrace();
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
        System.out.println("✓ 已清空所有待更新的节点状态");
    }
    
    /**
     * 停止同步任务
     * 确保所有待更新的节点状态都已同步到数据库
     */
    public void shutdown() {
        System.out.println("════════════════════════════════");
        System.out.println("🛑 节点状态同步管理器正在关闭...");
        
        // 先同步剩余的更新
        if (!pendingUpdates.isEmpty()) {
            int count = pendingUpdates.size();
            System.out.println("⚠️ 检测到 " + count + " 个待同步的节点状态");
            System.out.println("⏳ 正在同步到数据库...");
            
            // 立即同步（阻塞执行，确保完成）
            syncPendingUpdatesInternal();
            
            // 等待一小段时间确保API调用完成
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // 再次检查是否还有未同步的（同步失败的会重新加入）
            if (!pendingUpdates.isEmpty()) {
                System.out.println("⚠️ 仍有 " + pendingUpdates.size() + " 个节点状态未同步成功");
                System.out.println("🔄 进行第二次尝试...");
                syncPendingUpdatesInternal();
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        } else {
            System.out.println("✓ 没有待同步的节点状态");
        }
        
        // 取消定时任务
        if (syncTask != null) {
            syncTask.cancel(false);
            System.out.println("✓ 定时同步任务已取消");
        }
        
        // 关闭线程池
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                System.out.println("⚠️ 线程池未能在5秒内正常关闭，强制关闭");
                executorService.shutdownNow();
            } else {
                System.out.println("✓ 线程池已正常关闭");
            }
        } catch (InterruptedException e) {
            System.out.println("⚠️ 等待线程池关闭时被中断，强制关闭");
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        if (pendingUpdates.isEmpty()) {
            System.out.println("✅ 所有节点状态已成功同步到数据库");
        } else {
            System.out.println("❌ 警告: 仍有 " + pendingUpdates.size() + " 个节点状态未同步");
        }
        
        System.out.println("✓ 节点状态同步管理器已关闭");
        System.out.println("════════════════════════════════");
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

