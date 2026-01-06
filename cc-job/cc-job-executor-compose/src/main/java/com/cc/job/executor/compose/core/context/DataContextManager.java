package com.cc.job.executor.compose.core.context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据上下文管理器
 *
 * 职责：
 * 1. 管理所有任务组执行的数据上下文
 * 2. 提供上下文的创建、获取、销毁接口
 * 3. 自动清理过期的上下文（防止内存泄漏）
 *
 * @author cc-job-team
 */
@Component
public class DataContextManager {
    private final Logger logger = LoggerFactory.getLogger(DataContextManager.class);

    /** 所有活跃的数据上下文 */
    private final Map<String, DataContext> contextMap = new ConcurrentHashMap<>();

    /** 上下文创建时间记录 */
    private final Map<String, Long> contextCreateTimeMap = new ConcurrentHashMap<>();

    /** 上下文最大存活时间（毫秒），默认24小时 */
    private static final long MAX_CONTEXT_AGE_MS = 24 * 60 * 60 * 1000;

    /**
     * 创建数据上下文
     */
    public DataContext createContext(String executionBatchId) {
        DataContext context = new DataContext(executionBatchId);
        contextMap.put(executionBatchId, context);
        contextCreateTimeMap.put(executionBatchId, System.currentTimeMillis());

        logger.info("[DataContext] 创建上下文: {}", executionBatchId);
        return context;
    }

    /**
     * 获取数据上下文
     */
    public DataContext getContext(String executionBatchId) {
        DataContext context = contextMap.get(executionBatchId);
        if (context == null) {
            // 如果上下文不存在，自动创建
            context = createContext(executionBatchId);
        }
        return context;
    }

    /**
     * 销毁数据上下文
     */
    public void destroyContext(String executionBatchId) {
        DataContext context = contextMap.remove(executionBatchId);
        contextCreateTimeMap.remove(executionBatchId);

        if (context != null) {
            context.clear();
            logger.info("[DataContext] 销毁上下文: {}", executionBatchId);
        }
    }

    /**
     * 清理过期的上下文
     *
     * 通过定时任务定期调用
     */
    @Scheduled(fixedDelay = 3600000) // 每小时执行一次
    public void cleanupExpiredContexts() {
        long now = System.currentTimeMillis();
        int cleanupCount = 0;

        for (Map.Entry<String, Long> entry : contextCreateTimeMap.entrySet()) {
            long age = now - entry.getValue();
            if (age > MAX_CONTEXT_AGE_MS) {
                String batchId = entry.getKey();
                destroyContext(batchId);
                cleanupCount++;
            }
        }

        if (cleanupCount > 0) {
            logger.info("[DataContext] 清理 {} 个过期上下文", cleanupCount);
        }
    }

    /**
     * 获取所有活跃的上下文数量
     */
    public int getActiveContextCount() {
        return contextMap.size();
    }
}
