package com.cc.job.admin.task.sse;

import com.cc.job.admin.task.websocket.model.Message;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * SSE服务类
 * 用于管理SSE连接和推送消息
 * 
 * @author xiaozhao
 */
@Service
@Slf4j
public class SSEService {

    // SSE连接池：key = parentJobId:randomId, value = SseEmitter
    private static final Map<String, SseEmitter> SSE_CONNECTIONS = new ConcurrentHashMap<>();

    // JSON序列化器
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // 连接超时时间（毫秒）
    private static final long CONNECTION_TIMEOUT = 30 * 60 * 1000L; // 30分钟

    // 清理过期连接的定时器
    private static final ScheduledExecutorService CLEANUP_EXECUTOR = 
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "sse-cleanup");
                t.setDaemon(true);
                return t;
            });

    static {
        // 启动连接清理任务，每5分钟清理一次过期连接
        CLEANUP_EXECUTOR.scheduleWithFixedDelay(
                SSEService::cleanupExpiredConnections,
                5, 5, TimeUnit.MINUTES);

        // 添加JVM关闭钩子
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            CLEANUP_EXECUTOR.shutdown();
            try {
                if (!CLEANUP_EXECUTOR.awaitTermination(10, TimeUnit.SECONDS)) {
                    CLEANUP_EXECUTOR.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }));
    }

    /**
     * 创建SSE连接
     * 
     * @param parentJobId 父任务ID
     * @param randomId 随机ID
     * @return SseEmitter
     */
    public SseEmitter createConnection(Long parentJobId, String randomId) {
        String connectionKey = parentJobId + ":" + randomId;
        
        // 如果已存在连接，先关闭旧的
        SseEmitter existingEmitter = SSE_CONNECTIONS.remove(connectionKey);
        if (existingEmitter != null) {
            try {
                existingEmitter.complete();
            } catch (Exception e) {
                log.warn("关闭已存在的SSE连接失败: {}", connectionKey, e);
            }
        }

        // 创建新的SSE连接
        SseEmitter emitter = new SseEmitter(CONNECTION_TIMEOUT);
        SSE_CONNECTIONS.put(connectionKey, emitter);

        // 设置完成和超时回调
        emitter.onCompletion(() -> {
            SSE_CONNECTIONS.remove(connectionKey);
            log.info("[SSE] 连接完成并移除 - key: {}", connectionKey);
        });

        emitter.onTimeout(() -> {
            SSE_CONNECTIONS.remove(connectionKey);
            log.info("[SSE] 连接超时并移除 - key: {}", connectionKey);
        });

        emitter.onError((ex) -> {
            SSE_CONNECTIONS.remove(connectionKey);
            log.error("[SSE] 连接错误并移除 - key: {}", connectionKey, ex);
        });

        log.info("[SSE] ========== 创建SSE连接 ==========");
        log.info("[SSE] connectionKey: {}", connectionKey);
        log.info("[SSE] 当前连接数: {}", SSE_CONNECTIONS.size());
        log.info("[SSE] ==========================================");

        return emitter;
    }

    /**
     * 发送消息到指定连接
     * 
     * @param message 消息对象
     */
    public void sendMessage(Message message) {
        if (message == null || message.getParentJobId() == null || message.getRandomId() == null) {
            log.warn("[SSE] 消息参数不完整，跳过发送: {}", message);
            return;
        }

        String connectionKey = message.getParentJobId() + ":" + message.getRandomId();
        log.info("[SSE] ========== 发送消息 ==========");
        log.info("[SSE] connectionKey: {}", connectionKey);
        log.info("[SSE] message: jobId={}, status={}, randomId={}, parentJobId={}", 
                message.getJobId(), message.getStatus(), message.getRandomId(), message.getParentJobId());
        log.info("[SSE] 当前连接数: {}", SSE_CONNECTIONS.size());

        SseEmitter emitter = SSE_CONNECTIONS.get(connectionKey);
        
        if (emitter != null) {
            try {
                String messageJson = OBJECT_MAPPER.writeValueAsString(message);
                emitter.send(SseEmitter.event()
                        .name("nodeStatus")
                        .data(messageJson));
                
                log.info("[SSE] ✅ 消息已发送 - key: {}", connectionKey);
            } catch (IOException e) {
                log.error("[SSE] ❌ 消息发送失败 - key: {}", connectionKey, e);
                // 移除失效的连接
                SSE_CONNECTIONS.remove(connectionKey);
                try {
                    emitter.completeWithError(e);
                } catch (Exception ex) {
                    log.error("[SSE] 完成连接时出错", ex);
                }
            }
        } else {
            log.warn("[SSE] ⚠️ 未找到连接 - key: {}, 当前连接数: {}", connectionKey, SSE_CONNECTIONS.size());
            // 打印所有连接key用于调试
            log.debug("[SSE] 当前所有连接key: {}", SSE_CONNECTIONS.keySet());
        }
        log.info("[SSE] ==========================================");
    }

    /**
     * 关闭指定连接
     * 
     * @param parentJobId 父任务ID
     * @param randomId 随机ID
     */
    public void closeConnection(Long parentJobId, String randomId) {
        String connectionKey = parentJobId + ":" + randomId;
        SseEmitter emitter = SSE_CONNECTIONS.remove(connectionKey);
        
        if (emitter != null) {
            try {
                emitter.complete();
                log.info("[SSE] 连接已关闭 - key: {}", connectionKey);
            } catch (Exception e) {
                log.error("[SSE] 关闭连接失败 - key: {}", connectionKey, e);
            }
        }
    }

    /**
     * 清理过期连接
     */
    private static void cleanupExpiredConnections() {
        int removedCount = 0;
        for (Map.Entry<String, SseEmitter> entry : SSE_CONNECTIONS.entrySet()) {
            SseEmitter emitter = entry.getValue();
            try {
                // 尝试发送心跳，如果失败则说明连接已失效
                emitter.send(SseEmitter.event().name("ping").data("ping"));
            } catch (Exception e) {
                // 连接已失效，移除
                SSE_CONNECTIONS.remove(entry.getKey());
                removedCount++;
                try {
                    emitter.complete();
                } catch (Exception ex) {
                    // 忽略
                }
            }
        }
        
        if (removedCount > 0) {
            log.info("[SSE] 清理了 {} 个过期连接", removedCount);
        }
    }

    /**
     * 获取连接统计信息
     * 
     * @return 连接数
     */
    public int getConnectionCount() {
        return SSE_CONNECTIONS.size();
    }

    /**
     * 获取所有连接key
     * 
     * @return 连接key集合
     */
    public java.util.Set<String> getAllConnectionKeys() {
        return SSE_CONNECTIONS.keySet();
    }
}

