package com.cc.job.admin.task.websocket;

import com.cc.job.admin.task.websocket.model.Message;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CompletableFuture;

/**
 * 优化后的WebSocket服务器
 * 
 * @author xiaozhao
 */
@ServerEndpoint(value = "/ccJobWs/{id}", encoders = { ServerEncoder.class })
@Component
@Slf4j
public class WebSocketServer {

    // 静态变量，用来记录当前在线连接数
    private static final AtomicInteger ONLINE_NUM = new AtomicInteger();

    // 连接池，使用ConcurrentHashMap保证线程安全
    private static final ConcurrentHashMap<String, Session> SESSION_POOLS = new ConcurrentHashMap<>();

    // 消息发送线程池，专门用于异步发送消息
    private static final ExecutorService MESSAGE_EXECUTOR = new ThreadPoolExecutor(
            10, 20, 60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(1000),
            r -> new Thread(r, "websocket-sender-" + System.currentTimeMillis()),
            new ThreadPoolExecutor.CallerRunsPolicy());

    // 消息计数器
    private static final AtomicLong MESSAGE_COUNTER = new AtomicLong(0);

    // 连接心跳检测间隔（毫秒）
    private static final long HEARTBEAT_INTERVAL = 30000;

    // 异步发送消息，避免阻塞主线程
    public void sendMessageAsync(Session session, Message message) {
        if (session != null && session.isOpen()) {
            MESSAGE_EXECUTOR.submit(() -> {
                try {
                    long startTime = System.currentTimeMillis();
                    session.getBasicRemote().sendObject(message);
                    long endTime = System.currentTimeMillis();
                    log.debug("消息发送成功，耗时: {}ms, 消息ID: {}", endTime - startTime, MESSAGE_COUNTER.incrementAndGet());
                } catch (Exception e) {
                    log.error("发送消息失败: {}", e.getMessage(), e);
                    // 发送失败时移除无效连接
                    removeInvalidSession(session);
                }
            });
        }
    }

    // 同步发送消息（保留原有方法）
    public void sendMessage(Session session, Message message) {
        if (session != null && session.isOpen()) {
            try {
                session.getBasicRemote().sendObject(message);
                log.debug("同步发送消息成功: {}", message);
            } catch (Exception e) {
                log.error("同步发送消息失败: {}", e.getMessage(), e);
                removeInvalidSession(session);
            }
        }
    }

    // 给指定用户发送信息（优化版本）
    public void sendInfo(Message message) {
        String sessionKey = message.getParentJobId() + ":" + message.getRandomId();
        Session session = SESSION_POOLS.get(sessionKey);

        if (session != null && session.isOpen()) {
            // 使用异步发送提高响应速度
            sendMessageAsync(session, message);
            log.info("异步发送消息到会话: {}, 消息: {}", sessionKey, message);
        } else {
            log.warn("会话不存在或已关闭: {}", sessionKey);
        }
    }

    // 批量发送消息
    public void sendBatchInfo(List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }

        // 按会话分组消息
        Map<String, List<Message>> sessionMessages = new ConcurrentHashMap<>();
        for (Message message : messages) {
            String sessionKey = message.getParentJobId() + ":" + message.getRandomId();
            sessionMessages.computeIfAbsent(sessionKey, k -> new CopyOnWriteArrayList<>()).add(message);
        }

        // 异步批量发送
        sessionMessages.forEach((sessionKey, msgList) -> {
            Session session = SESSION_POOLS.get(sessionKey);
            if (session != null && session.isOpen()) {
                MESSAGE_EXECUTOR.submit(() -> {
                    for (Message msg : msgList) {
                        try {
                            session.getBasicRemote().sendObject(msg);
                        } catch (Exception e) {
                            log.error("批量发送消息失败: {}", e.getMessage(), e);
                            break;
                        }
                    }
                });
            }
        });
    }

    // 群发消息（优化版本）
    public void broadcast(Message message) {
        List<Session> validSessions = SESSION_POOLS.values().stream()
                .filter(Session::isOpen)
                .toList();

        if (validSessions.isEmpty()) {
            return;
        }

        // 使用CompletableFuture并发发送
        List<CompletableFuture<Void>> futures = validSessions.stream()
                .map(session -> CompletableFuture.runAsync(() -> {
                    try {
                        session.getBasicRemote().sendObject(message);
                    } catch (Exception e) {
                        log.error("广播消息失败: {}", e.getMessage(), e);
                        removeInvalidSession(session);
                    }
                }, MESSAGE_EXECUTOR))
                .toList();

        // 等待所有发送完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .exceptionally(throwable -> {
                    log.error("广播消息异常: {}", throwable.getMessage(), throwable);
                    return null;
                });
    }

    // 移除无效会话
    private void removeInvalidSession(Session session) {
        SESSION_POOLS.entrySet().removeIf(entry -> entry.getValue().equals(session));
        subOnlineCount();
        log.info("移除无效会话，当前连接数: {}", ONLINE_NUM.get());
    }

    // 建立连接成功调用
    @OnOpen
    public void onOpen(Session session, @PathParam(value = "id") String id) {
        // 设置会话参数
        session.setMaxTextMessageBufferSize(8192);
        session.setMaxBinaryMessageBufferSize(8192);

        SESSION_POOLS.put(id, session);
        addOnlineCount();
        log.info("{}加入webSocket！当前人数为={}", id, ONLINE_NUM.get());

        // 启动心跳检测
        startHeartbeat(session, id);
    }

    // 关闭连接时调用
    @OnClose
    public void onClose(@PathParam(value = "id") String id) {
        SESSION_POOLS.remove(id);
        subOnlineCount();
        log.info("{}断开webSocket连接！当前人数为={}", id, ONLINE_NUM.get());
    }

    // 收到客户端信息后，根据接收人的username把消息推下去或者群发
    @OnMessage
    public void onMessage(String message, Session session) {
        log.info("收到客户端消息: {}", message);
        // 可以在这里处理客户端的心跳响应
        if ("ping".equals(message)) {
            try {
                session.getBasicRemote().sendText("pong");
            } catch (Exception e) {
                log.error("发送心跳响应失败: {}", e.getMessage(), e);
            }
        }
    }

    // 错误时调用
    @OnError
    public void onError(Session session, Throwable throwable) {
        log.error("WebSocket发生错误", throwable);
        removeInvalidSession(session);
    }

    // 启动心跳检测
    private void startHeartbeat(Session session, String id) {
        MESSAGE_EXECUTOR.submit(() -> {
            while (session.isOpen()) {
                try {
                    Thread.sleep(HEARTBEAT_INTERVAL);
                    if (session.isOpen()) {
                        session.getBasicRemote().sendText("ping");
                    }
                } catch (Exception e) {
                    log.error("心跳检测失败: {}", e.getMessage(), e);
                    break;
                }
            }
            log.info("会话 {} 心跳检测结束", id);
        });
    }

    public static void addOnlineCount() {
        ONLINE_NUM.incrementAndGet();
    }

    public static void subOnlineCount() {
        ONLINE_NUM.decrementAndGet();
    }

    public static AtomicInteger getOnlineNumber() {
        return ONLINE_NUM;
    }

    public static ConcurrentHashMap<String, Session> getSessionPools() {
        return SESSION_POOLS;
    }

    // 获取连接统计信息
    public static String getConnectionStats() {
        return String.format("在线连接数: %d, 消息计数: %d", ONLINE_NUM.get(), MESSAGE_COUNTER.get());
    }

    // 关闭资源
    public static void shutdown() {
        MESSAGE_EXECUTOR.shutdown();
        try {
            if (!MESSAGE_EXECUTOR.awaitTermination(5, TimeUnit.SECONDS)) {
                MESSAGE_EXECUTOR.shutdownNow();
            }
        } catch (InterruptedException e) {
            MESSAGE_EXECUTOR.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
