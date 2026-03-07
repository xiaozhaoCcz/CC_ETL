package com.cc.job.admin.task.sse;

import com.cc.job.admin.task.websocket.model.Message;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.catalina.connector.ClientAbortException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * SSE服务类
 * 用于管理SSE连接和推送消息
 * 支持多个客户端同时连接同一个任务组
 * 
 * @author xiaozhao
 */
@Service
public class SSEService {

    private static final Logger log = LoggerFactory.getLogger(SSEService.class);

    /** 多实例时使用数据库广播，可选注入；无则仅本机推送 */
    @Autowired(required = false)
    private SseBroadcastProvider sseBroadcastProvider;

    // SSE连接池：key = parentJobId:randomId, value = List<SseEmitter>（支持多个连接）
    private static final Map<String, CopyOnWriteArrayList<SseEmitter>> SSE_CONNECTIONS = new ConcurrentHashMap<>();

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
     * 支持多个客户端同时连接同一个任务组
     * 
     * @param parentJobId 父任务ID
     * @param randomId 随机ID
     * @return SseEmitter
     */
    public SseEmitter createConnection(Long parentJobId, String randomId) {
        String connectionKey = parentJobId + ":" + randomId;
        
        // 创建新的SSE连接
        SseEmitter emitter = new SseEmitter(CONNECTION_TIMEOUT);
        
        // 添加到连接列表（支持多个连接）
        CopyOnWriteArrayList<SseEmitter> emitters = SSE_CONNECTIONS.computeIfAbsent(
            connectionKey, 
            k -> new CopyOnWriteArrayList<>()
        );
        emitters.add(emitter);

        // 设置完成和超时回调
        emitter.onCompletion(() -> {
            removeConnection(connectionKey, emitter);
        });

        emitter.onTimeout(() -> {
            removeConnection(connectionKey, emitter);
        });

        emitter.onError((ex) -> {
            removeConnection(connectionKey, emitter);
            // 客户端主动关闭连接是正常情况，只记录debug级别
            if (isClientClosedException(ex)) {
                log.debug("[SSE] 客户端关闭连接 - key: {}", connectionKey);
            } else {
                log.warn("[SSE] 连接错误并移除 - key: {}", connectionKey, ex);
            }
        });

        return emitter;
    }
    
    /**
     * 移除指定的连接
     * 
     * @param connectionKey 连接key
     * @param emitter 要移除的emitter
     */
    private void removeConnection(String connectionKey, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> emitters = SSE_CONNECTIONS.get(connectionKey);
        if (emitters != null) {
            emitters.remove(emitter);
            // 如果列表为空，移除整个key
            if (emitters.isEmpty()) {
                SSE_CONNECTIONS.remove(connectionKey);
            }
        }
    }
    
    /**
     * 判断是否是客户端主动关闭连接的异常
     * 
     * @param ex 异常
     * @return true表示是客户端主动关闭
     */
    private boolean isClientClosedException(Throwable ex) {
        if (ex == null) {
            return false;
        }
        
        // 检查IOException: Broken pipe
        if (ex instanceof IOException) {
            String message = ex.getMessage();
            if (message != null && (message.contains("Broken pipe") || 
                                    message.contains("Connection reset") ||
                                    message.contains("Connection closed"))) {
                return true;
            }
        }
        
        // 检查ClientAbortException
        if (ex instanceof ClientAbortException) {
            return true;
        }
        
        // 检查cause
        return isClientClosedException(ex.getCause());
    }

    /**
     * 发送消息到指定连接
     * 若启用了广播（存在 SseBroadcastProvider），则只发布到广播表，由各实例轮询后在本机推送；
     * 否则仅在本机连接中推送。
     *
     * @param message 消息对象
     */
    public void sendMessage(Message message) {
        if (message == null || message.getParentJobId() == null || message.getRandomId() == null) {
            log.warn("[SSE] 消息参数不完整，跳过发送: {}", message);
            return;
        }

        String connectionKey = message.getParentJobId() + ":" + message.getRandomId();
        String messageJson;
        try {
            messageJson = OBJECT_MAPPER.writeValueAsString(message);
            if (messageJson == null) {
                log.error("[SSE] 消息序列化结果为null - key: {}", connectionKey);
                return;
            }
        } catch (Exception e) {
            log.error("[SSE] 消息序列化失败 - key: {}", connectionKey, e);
            return;
        }

        if (sseBroadcastProvider != null) {
            sseBroadcastProvider.publishNodeStatus(connectionKey, messageJson);
            return;
        }

        sendMessageToLocalOnly(connectionKey, messageJson);
    }

    /**
     * 仅在本机 SSE 连接中推送消息（供轮询任务调用，或单实例直推）
     *
     * @param connectionKey 连接键 parentJobId:randomId
     * @param messageJson   消息体 JSON
     * @return 是否向至少一个连接推送成功
     */
    public boolean sendMessageToLocalOnly(String connectionKey, String messageJson) {
        if (connectionKey == null || messageJson == null) {
            return false;
        }
        CopyOnWriteArrayList<SseEmitter> emitters = SSE_CONNECTIONS.get(connectionKey);
        if (emitters == null || emitters.isEmpty()) {
            log.debug("[SSE] ⚠️ 未找到连接 - key: {}, 当前连接数: {}", connectionKey, SSE_CONNECTIONS.size());
            return false;
        }

        List<SseEmitter> deadEmitters = new ArrayList<>();
        boolean anySent = false;
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("nodeStatus")
                        .data(messageJson));
                anySent = true;
            } catch (Exception e) {
                if (isClientClosedException(e)) {
                    log.debug("[SSE] 客户端已关闭连接，移除 - key: {}", connectionKey);
                } else {
                    log.warn("[SSE] 消息发送失败 - key: {}", connectionKey, e);
                }
                deadEmitters.add(emitter);
                try {
                    emitter.complete();
                } catch (Exception ex) {
                    // 忽略
                }
            }
        }
        if (!deadEmitters.isEmpty()) {
            emitters.removeAll(deadEmitters);
            if (emitters.isEmpty()) {
                SSE_CONNECTIONS.remove(connectionKey);
            }
        }
        return anySent;
    }

    /**
     * 关闭指定连接（关闭该key下的所有连接）
     * 
     * @param parentJobId 父任务ID
     * @param randomId 随机ID
     */
    public void closeConnection(Long parentJobId, String randomId) {
        String connectionKey = parentJobId + ":" + randomId;
        CopyOnWriteArrayList<SseEmitter> emitters = SSE_CONNECTIONS.remove(connectionKey);
        
        if (emitters != null && !emitters.isEmpty()) {
            for (SseEmitter emitter : emitters) {
                try {
                    emitter.complete();
                } catch (Exception e) {
                    log.debug("[SSE] 关闭连接时出错 - key: {}", connectionKey, e);
                }
            }
        }
    }

    /**
     * 清理过期连接
     * 遍历所有连接，尝试发送心跳，移除失效的连接
     */
    private static void cleanupExpiredConnections() {
        int removedCount = 0;
        List<String> keysToRemove = new ArrayList<>();
        
        for (Map.Entry<String, CopyOnWriteArrayList<SseEmitter>> entry : SSE_CONNECTIONS.entrySet()) {
            String connectionKey = entry.getKey();
            CopyOnWriteArrayList<SseEmitter> emitters = entry.getValue();
            
            if (emitters == null || emitters.isEmpty()) {
                keysToRemove.add(connectionKey);
                continue;
            }
            
            List<SseEmitter> deadEmitters = new ArrayList<>();
            
            for (SseEmitter emitter : emitters) {
                try {
                    // 尝试发送心跳，如果失败则说明连接已失效
                    emitter.send(SseEmitter.event().name("ping").data("ping"));
                } catch (Exception e) {
                    // 连接已失效，标记为待移除
                    deadEmitters.add(emitter);
                    try {
                        emitter.complete();
                    } catch (Exception ex) {
                        // 忽略完成时的异常
                    }
                }
            }
            
            // 移除失效的连接
            if (!deadEmitters.isEmpty()) {
                emitters.removeAll(deadEmitters);
                removedCount += deadEmitters.size();
            }
            
            // 如果列表为空，标记为待移除
            if (emitters.isEmpty()) {
                keysToRemove.add(connectionKey);
            }
        }
        
        // 移除空的key
        for (String key : keysToRemove) {
            SSE_CONNECTIONS.remove(key);
        }
        
        if (removedCount > 0) {
            log.debug("[SSE] 清理过期连接完成 - 移除连接数: {}, 剩余连接数: {}", removedCount, SSE_CONNECTIONS.size());
        }
    }

    /**
     * 获取连接统计信息
     * 
     * @return 连接数（key的数量）
     */
    public int getConnectionCount() {
        return SSE_CONNECTIONS.size();
    }
    
    /**
     * 获取总连接数（所有emitter的数量）
     * 
     * @return 总连接数
     */
    public int getTotalEmitterCount() {
        return SSE_CONNECTIONS.values().stream()
                .mapToInt(List::size)
                .sum();
    }

    /**
     * 获取所有连接key
     * 
     * @return 连接key集合
     */
    public java.util.Set<String> getAllConnectionKeys() {
        return SSE_CONNECTIONS.keySet();
    }

    /**
     * 发送任务状态更新消息（指定 parentJobId）
     * 
     * @param parentJobId 父任务ID（任务组ID）
     * @param jobId 任务ID（子任务ID）
     * @param randomId 批次ID
     * @param status 状态（0=失败，1=成功，2=执行中，5=完成）
     * @param message 状态消息
     */
    public void sendJobStatus(Long parentJobId, Long jobId, String randomId, Integer status, String message) {
        if (parentJobId == null || jobId == null || randomId == null || status == null) {
            log.warn("[SSE] 发送任务状态失败：参数不完整 - parentJobId: {}, jobId: {}, randomId: {}, status: {}", 
                    parentJobId, jobId, randomId, status);
            return;
        }

        // 创建消息对象
        Message sseMessage = new Message();
        sseMessage.setParentJobId(parentJobId);
        sseMessage.setJobId(jobId);
        sseMessage.setRandomId(randomId);
        sseMessage.setStatus(status);
        sseMessage.setResult(message);

        log.debug("[SSE] 准备发送任务状态 - parentJobId: {}, jobId: {}, randomId: {}, status: {}", 
                parentJobId, jobId, randomId, status);

        // 发送消息
        sendMessage(sseMessage);
    }
}


