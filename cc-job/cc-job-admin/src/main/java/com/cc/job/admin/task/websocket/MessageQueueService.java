package com.cc.job.admin.task.websocket;

import com.cc.job.admin.task.websocket.model.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 消息队列服务
 * 用于缓存和批量发送消息，提高WebSocket响应速度
 */
@Service
@Slf4j
public class MessageQueueService {

    // 消息队列，按会话分组
    private static final ConcurrentHashMap<String, BlockingQueue<Message>> MESSAGE_QUEUES = new ConcurrentHashMap<>();

    // 消息发送线程池
    private static final ExecutorService SENDER_EXECUTOR = new ThreadPoolExecutor(
            5, 10, 60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(500),
            r -> new Thread(r, "message-sender-" + System.currentTimeMillis()),
            new ThreadPoolExecutor.CallerRunsPolicy());

    // 消息计数器
    private static final AtomicLong MESSAGE_COUNTER = new AtomicLong(0);

    // 批量发送间隔（毫秒）
    private static final long BATCH_INTERVAL = 100;

    // 最大批量大小
    private static final int MAX_BATCH_SIZE = 10;

    /**
     * 添加消息到队列
     */
    public void addMessage(Message message) {
        String sessionKey = message.getParentJobId() + ":" + message.getRandomId();

        MESSAGE_QUEUES.computeIfAbsent(sessionKey, k -> {
            // 为新会话启动发送线程
            startSenderThread(sessionKey);
            return new LinkedBlockingQueue<>();
        }).offer(message);

        log.debug("消息已添加到队列: {}, 消息ID: {}", sessionKey, MESSAGE_COUNTER.incrementAndGet());
    }

    /**
     * 批量添加消息
     */
    public void addBatchMessages(List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }

        // 按会话分组
        ConcurrentHashMap<String, List<Message>> sessionMessages = new ConcurrentHashMap<>();
        for (Message message : messages) {
            String sessionKey = message.getParentJobId() + ":" + message.getRandomId();
            sessionMessages.computeIfAbsent(sessionKey, k -> new CopyOnWriteArrayList<>()).add(message);
        }

        // 批量添加到队列
        sessionMessages.forEach((sessionKey, msgList) -> {
            BlockingQueue<Message> queue = MESSAGE_QUEUES.computeIfAbsent(sessionKey, k -> {
                startSenderThread(sessionKey);
                return new LinkedBlockingQueue<>();
            });

            for (Message msg : msgList) {
                queue.offer(msg);
            }
        });

        log.info("批量添加消息完成，会话数: {}, 消息总数: {}", sessionMessages.size(), messages.size());
    }

    /**
     * 启动发送线程
     */
    private void startSenderThread(String sessionKey) {
        SENDER_EXECUTOR.submit(() -> {
            BlockingQueue<Message> queue = MESSAGE_QUEUES.get(sessionKey);
            if (queue == null) {
                return;
            }

            List<Message> batch = new CopyOnWriteArrayList<>();
            long lastSendTime = System.currentTimeMillis();

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // 尝试获取消息，最多等待批量间隔时间
                    Message message = queue.poll(BATCH_INTERVAL, TimeUnit.MILLISECONDS);

                    if (message != null) {
                        batch.add(message);
                    }

                    long currentTime = System.currentTimeMillis();
                    boolean shouldSend = !batch.isEmpty() && (batch.size() >= MAX_BATCH_SIZE ||
                            (currentTime - lastSendTime >= BATCH_INTERVAL && !batch.isEmpty()));

                    if (shouldSend) {
                        sendBatchMessages(sessionKey, batch);
                        batch.clear();
                        lastSendTime = currentTime;
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.error("发送线程异常: {}", e.getMessage(), e);
                }
            }

            // 发送剩余消息
            if (!batch.isEmpty()) {
                sendBatchMessages(sessionKey, batch);
            }

            log.info("发送线程结束: {}", sessionKey);
        });
    }

    /**
     * 发送批量消息
     */
    private void sendBatchMessages(String sessionKey, List<Message> messages) {
        if (messages.isEmpty()) {
            return;
        }

        try {
            // 使用WebSocket发送消息
            WebSocketServer webSocketServer = new WebSocketServer();
            webSocketServer.sendBatchInfo(messages);

            log.debug("批量发送消息成功: {}, 消息数量: {}", sessionKey, messages.size());
        } catch (Exception e) {
            log.error("批量发送消息失败: {}, 错误: {}", sessionKey, e.getMessage(), e);
        }
    }

    /**
     * 移除会话队列
     */
    public void removeSession(String sessionKey) {
        MESSAGE_QUEUES.remove(sessionKey);
        log.info("移除会话队列: {}", sessionKey);
    }

    /**
     * 获取队列统计信息
     */
    public String getQueueStats() {
        StringBuilder stats = new StringBuilder();
        stats.append("队列数量: ").append(MESSAGE_QUEUES.size()).append("\n");

        MESSAGE_QUEUES.forEach((sessionKey, queue) -> {
            stats.append("会话: ").append(sessionKey)
                    .append(", 队列大小: ").append(queue.size())
                    .append("\n");
        });

        return stats.toString();
    }

    /**
     * 关闭服务
     */
    public void shutdown() {
        SENDER_EXECUTOR.shutdown();
        try {
            if (!SENDER_EXECUTOR.awaitTermination(5, TimeUnit.SECONDS)) {
                SENDER_EXECUTOR.shutdownNow();
            }
        } catch (InterruptedException e) {
            SENDER_EXECUTOR.shutdownNow();
            Thread.currentThread().interrupt();
        }

        // 清空所有队列
        MESSAGE_QUEUES.clear();
        log.info("消息队列服务已关闭");
    }
}