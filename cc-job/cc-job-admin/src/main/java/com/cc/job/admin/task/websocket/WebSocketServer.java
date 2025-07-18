package com.cc.job.admin.task.websocket;

import com.cc.job.admin.task.websocket.model.Message;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 优化后的WebSocket服务器
 * 支持连接池管理、消息队列、连接超时清理等特性
 * 
 * @author xiaozhao
 */
@ServerEndpoint(value = "/ccJobWs/{id}", encoders = { ServerEncoder.class })
@Component
@Slf4j
public class WebSocketServer {

    // 连接池管理
    private static final ConcurrentHashMap<String, WebSocketSession> SESSION_POOLS = new ConcurrentHashMap<>();

    // 在线连接数统计
    private static final AtomicInteger ONLINE_NUM = new AtomicInteger(0);

    // 消息发送统计
    private static final AtomicLong MESSAGE_COUNT = new AtomicLong(0);

    // 消息队列，用于异步发送
    private static final ExecutorService MESSAGE_EXECUTOR = new ThreadPoolExecutor(
            32, 128, 60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(2000),
            new ThreadPoolExecutor.DiscardOldestPolicy());

    // 连接清理定时器
    private static final ScheduledExecutorService CLEANUP_EXECUTOR = Executors.newSingleThreadScheduledExecutor();

    // JSON序列化器（单例）
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // 连接超时时间（毫秒）
    private static final long CONNECTION_TIMEOUT = 30 * 60 * 1000L; // 30分钟

    // 最大连接数限制
    private static final int MAX_CONNECTIONS = 10000;

    // 消息发送超时时间
    private static final long MESSAGE_TIMEOUT = 5000L; // 5秒

    static {
        // 启动连接清理任务
        CLEANUP_EXECUTOR.scheduleWithFixedDelay(
                WebSocketServer::cleanupExpiredConnections,
                5, 5, TimeUnit.MINUTES);

        // 添加JVM关闭钩子
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            MESSAGE_EXECUTOR.shutdown();
            CLEANUP_EXECUTOR.shutdown();
            try {
                if (!MESSAGE_EXECUTOR.awaitTermination(10, TimeUnit.SECONDS)) {
                    MESSAGE_EXECUTOR.shutdownNow();
                }
                if (!CLEANUP_EXECUTOR.awaitTermination(10, TimeUnit.SECONDS)) {
                    CLEANUP_EXECUTOR.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }));
    }

    /**
     * 异步发送消息（改为队列+线程方式）
     */
    public void sendMessageAsync(Session session, Message message) {
        if (session == null || !session.isOpen()) {
            log.warn("Session is null or closed, message: {}", message);
            return;
        }
        WebSocketSession wsSession = null;
        for (WebSocketSession s : SESSION_POOLS.values()) {
            if (s.getSession().equals(session)) {
                wsSession = s;
                break;
            }
        }
        if (wsSession != null && wsSession.isValid()) {
            try {
                String messageJson = OBJECT_MAPPER.writeValueAsString(message);
                wsSession.enqueueMessage(messageJson);
                MESSAGE_COUNT.incrementAndGet();
            } catch (Exception e) {
                log.error("Failed to enqueue message: {}", message, e);
                removeSession(session);
            }
        } else {
            log.warn("WebSocketSession not found or invalid for session: {}", session);
        }
    }

    /**
     * 同步发送消息（带超时）
     */
    private void sendMessageSync(Session session, Message message) throws IOException {
        if (session == null || !session.isOpen()) {
            return;
        }
        try {
            String messageJson = OBJECT_MAPPER.writeValueAsString(message);
            Callable<Void> sendTask = () -> {
                session.getBasicRemote().sendText(messageJson);
                return null;
            };
            Future<Void> future = MESSAGE_EXECUTOR.submit(sendTask);
            try {
                future.get(MESSAGE_TIMEOUT, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                log.error("Send message timeout: {}", messageJson);
                future.cancel(true);
                throw new IOException("Send message timeout", e);
            } catch (Exception e) {
                log.error("Failed to send message: {}", messageJson, e);
                throw new IOException("Failed to send message", e);
            }
            log.debug("Message sent successfully: {}", messageJson);
        } catch (Exception e) {
            log.error("Failed to send message: {}", message, e);
            throw new IOException("Failed to send message", e);
        }
    }

    /**
     * 给指定用户发送信息（异步）
     */
    public void sendInfo(Message message) {
        String sessionKey = message.getParentJobId() + ":" + message.getRandomId();
        WebSocketSession webSocketSession = SESSION_POOLS.get(sessionKey);
        if (webSocketSession != null && webSocketSession.isValid()) {
            try {
                String messageJson = OBJECT_MAPPER.writeValueAsString(message);
                webSocketSession.enqueueMessage(messageJson);
                MESSAGE_COUNT.incrementAndGet();
            } catch (Exception e) {
                log.error("Failed to enqueue message: {}", message, e);
                removeSession(webSocketSession.getSession());
            }
        } else {
            log.warn("Session not found or invalid for key: {}", sessionKey);
        }
    }

    /**
     * 群发消息（异步）
     */
    public void broadcast(Message message) {
        String msg;
        try {
            msg = OBJECT_MAPPER.writeValueAsString(message);
        } catch (Exception e) {
            log.error("广播消息序列化失败: {}", e.getMessage(), e);
            return;
        }
        SESSION_POOLS.values().parallelStream()
                .filter(WebSocketSession::isValid)
                .forEach(session -> session.enqueueMessage(msg));
    }

    /**
     * 建立连接
     */
    @OnOpen
    public void onOpen(Session session, @PathParam(value = "id") String id) {
        // 检查连接数限制
        if (ONLINE_NUM.get() >= MAX_CONNECTIONS) {
            log.warn("Max connections reached: {}", MAX_CONNECTIONS);
            try {
                session.close();
            } catch (IOException e) {
                log.error("Failed to close session", e);
            }
            return;
        }

        WebSocketSession webSocketSession = new WebSocketSession(session, id);
        SESSION_POOLS.put(id, webSocketSession);
        ONLINE_NUM.incrementAndGet();

        log.info("{} joined WebSocket! Current online count: {}", id, ONLINE_NUM.get());
    }

    /**
     * 关闭连接
     */
    @OnClose
    public void onClose(@PathParam(value = "id") String id) {
        WebSocketSession webSocketSession = SESSION_POOLS.remove(id);
        if (webSocketSession != null) {
            webSocketSession.close();
            ONLINE_NUM.decrementAndGet();
            log.info("{} disconnected from WebSocket! Current online count: {}", id, ONLINE_NUM.get());
        }
    }

    /**
     * 接收客户端消息
     */
    @OnMessage
    public void onMessage(String message, @PathParam(value = "id") String id) {
        log.debug("Received message from {}: {}", id, message);
        // 可以在这里处理客户端消息
    }

    /**
     * 连接错误处理
     */
    @OnError
    public void onError(Session session, Throwable throwable) {
        log.error("WebSocket error occurred", throwable);
        removeSession(session);
    }

    /**
     * 移除无效会话
     */
    private void removeSession(Session session) {
        SESSION_POOLS.entrySet().removeIf(entry -> {
            if (entry.getValue().getSession().equals(session)) {
                ONLINE_NUM.decrementAndGet();
                log.info("Removed invalid session: {}", entry.getKey());
                return true;
            }
            return false;
        });
    }

    /**
     * 清理过期连接
     */
    private static void cleanupExpiredConnections() {
        long currentTime = System.currentTimeMillis();
        final AtomicInteger removedCount = new AtomicInteger(0);

        SESSION_POOLS.entrySet().removeIf(entry -> {
            WebSocketSession session = entry.getValue();
            if (currentTime - session.getLastAccessTime() > CONNECTION_TIMEOUT) {
                session.close();
                ONLINE_NUM.decrementAndGet();
                removedCount.incrementAndGet();
                log.info("Cleaned up expired session: {}", entry.getKey());
                return true;
            }
            return false;
        });

        if (removedCount.get() > 0) {
            log.info("Cleaned up {} expired connections", removedCount.get());
        }
    }

    /**
     * 获取连接统计信息
     */
    public static ConnectionStats getConnectionStats() {
        return new ConnectionStats(
                ONLINE_NUM.get(),
                MESSAGE_COUNT.get(),
                SESSION_POOLS.size(),
                MAX_CONNECTIONS);
    }

    /**
     * 连接统计信息
     */
    public static class ConnectionStats {
        private final int onlineCount;
        private final long messageCount;
        private final int sessionCount;
        private final int maxConnections;

        public ConnectionStats(int onlineCount, long messageCount, int sessionCount, int maxConnections) {
            this.onlineCount = onlineCount;
            this.messageCount = messageCount;
            this.sessionCount = sessionCount;
            this.maxConnections = maxConnections;
        }

        // Getters
        public int getOnlineCount() {
            return onlineCount;
        }

        public long getMessageCount() {
            return messageCount;
        }

        public int getSessionCount() {
            return sessionCount;
        }

        public int getMaxConnections() {
            return maxConnections;
        }
    }

    /**
     * WebSocket会话包装类
     */
    private static class WebSocketSession {
        private final Session session;
        private final String id;
        private final long createTime;
        private volatile long lastAccessTime;
        private final BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();
        private final Thread senderThread;
        private volatile boolean running = true;

        public WebSocketSession(Session session, String id) {
            this.session = session;
            this.id = id;
            this.createTime = System.currentTimeMillis();
            this.lastAccessTime = this.createTime;
            // 启动独立发送线程
            this.senderThread = new Thread(this::processQueue, "ws-sender-" + id);
            this.senderThread.setDaemon(true);
            this.senderThread.start();
        }

        public Session getSession() {
            return session;
        }

        public String getId() {
            return id;
        }

        public long getCreateTime() {
            return createTime;
        }

        public long getLastAccessTime() {
            return lastAccessTime;
        }

        public void updateLastAccessTime() {
            this.lastAccessTime = System.currentTimeMillis();
        }

        public boolean isValid() {
            return session != null && session.isOpen();
        }

        public void enqueueMessage(String text) {
            messageQueue.offer(text);
        }

        private void processQueue() {
            try {
                while (running && session.isOpen()) {
                    String msg = messageQueue.take();
                    try {
                        session.getBasicRemote().sendText(msg);
                    } catch (Exception e) {
                        log.error("WebSocket消息发送失败，id: {}，异常: {}", id, e.getMessage(), e);
                        break;
                    }
                }
            } catch (InterruptedException e) {
                // 线程中断，正常退出
            }
        }

        public void close() {
            running = false;
            try {
                if (session != null && session.isOpen()) {
                    session.close();
                }
            } catch (IOException e) {
                log.error("Failed to close session: {}", id, e);
            }
            senderThread.interrupt();
        }
    }
}
