package com.cc.job.admin.task.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * WebSocket管理器
 * 提供连接监控、性能统计、健康检查等功能
 * 
 * @author xiaozhao
 */
@Component
@Slf4j
public class WebSocketManager {

    @Autowired
    private WebSocketServer webSocketServer;

    // 性能统计
    private final AtomicLong totalMessagesSent = new AtomicLong(0);
    private final AtomicLong totalMessagesFailed = new AtomicLong(0);
    private final AtomicLong totalConnections = new AtomicLong(0);
    private final AtomicLong totalDisconnections = new AtomicLong(0);

    /**
     * 发送消息并统计
     */
    public void sendMessageWithStats(com.cc.job.admin.task.websocket.model.Message message) {
        try {
            webSocketServer.sendInfo(message);
            totalMessagesSent.incrementAndGet();
        } catch (Exception e) {
            totalMessagesFailed.incrementAndGet();
            log.error("Failed to send message: {}", message, e);
        }
    }

    /**
     * 记录连接建立
     */
    public void recordConnection() {
        totalConnections.incrementAndGet();
    }

    /**
     * 记录连接断开
     */
    public void recordDisconnection() {
        totalDisconnections.incrementAndGet();
    }

    /**
     * 获取性能统计信息
     */
    public PerformanceStats getPerformanceStats() {
        WebSocketServer.ConnectionStats connectionStats = WebSocketServer.getConnectionStats();

        return new PerformanceStats(
                connectionStats.getOnlineCount(),
                connectionStats.getMessageCount(),
                connectionStats.getSessionCount(),
                connectionStats.getMaxConnections(),
                totalMessagesSent.get(),
                totalMessagesFailed.get(),
                totalConnections.get(),
                totalDisconnections.get(),
                calculateSuccessRate(),
                calculateAverageMessagesPerMinute());
    }

    /**
     * 计算消息发送成功率
     */
    private double calculateSuccessRate() {
        long total = totalMessagesSent.get() + totalMessagesFailed.get();
        if (total == 0) {
            return 100.0;
        }
        return (double) totalMessagesSent.get() / total * 100.0;
    }

    /**
     * 计算每分钟平均消息数
     */
    private double calculateAverageMessagesPerMinute() {
        // 这里可以根据实际需求计算
        return totalMessagesSent.get() / 60.0;
    }

    /**
     * 健康检查 - 每5分钟执行一次
     */
    @Scheduled(fixedRate = 300000) // 5分钟
    public void healthCheck() {
        WebSocketServer.ConnectionStats stats = WebSocketServer.getConnectionStats();

        // 检查连接数是否接近限制
        if (stats.getOnlineCount() > stats.getMaxConnections() * 0.8) {
            log.warn("WebSocket connections are approaching limit: {}/{}",
                    stats.getOnlineCount(), stats.getMaxConnections());
        }

        // 检查消息发送成功率
        double successRate = calculateSuccessRate();
        if (successRate < 95.0) {
            log.warn("WebSocket message success rate is low: {}%", successRate);
        }

        // 记录性能统计
        log.info("WebSocket Health Check - Online: {}, Messages: {}, Success Rate: {}%",
                stats.getOnlineCount(), stats.getMessageCount(), successRate);
    }

    /**
     * 清理统计信息 - 每天执行一次
     */
    @Scheduled(cron = "0 0 0 * * ?") // 每天0点执行
    public void resetDailyStats() {
        totalMessagesSent.set(0);
        totalMessagesFailed.set(0);
        totalConnections.set(0);
        totalDisconnections.set(0);
        log.info("WebSocket daily statistics reset");
    }

    /**
     * 性能统计信息
     */
    public static class PerformanceStats {
        private final int currentOnlineCount;
        private final long totalMessageCount;
        private final int currentSessionCount;
        private final int maxConnections;
        private final long totalMessagesSent;
        private final long totalMessagesFailed;
        private final long totalConnections;
        private final long totalDisconnections;
        private final double successRate;
        private final double averageMessagesPerMinute;

        public PerformanceStats(int currentOnlineCount, long totalMessageCount,
                int currentSessionCount, int maxConnections,
                long totalMessagesSent, long totalMessagesFailed,
                long totalConnections, long totalDisconnections,
                double successRate, double averageMessagesPerMinute) {
            this.currentOnlineCount = currentOnlineCount;
            this.totalMessageCount = totalMessageCount;
            this.currentSessionCount = currentSessionCount;
            this.maxConnections = maxConnections;
            this.totalMessagesSent = totalMessagesSent;
            this.totalMessagesFailed = totalMessagesFailed;
            this.totalConnections = totalConnections;
            this.totalDisconnections = totalDisconnections;
            this.successRate = successRate;
            this.averageMessagesPerMinute = averageMessagesPerMinute;
        }

        // Getters
        public int getCurrentOnlineCount() {
            return currentOnlineCount;
        }

        public long getTotalMessageCount() {
            return totalMessageCount;
        }

        public int getCurrentSessionCount() {
            return currentSessionCount;
        }

        public int getMaxConnections() {
            return maxConnections;
        }

        public long getTotalMessagesSent() {
            return totalMessagesSent;
        }

        public long getTotalMessagesFailed() {
            return totalMessagesFailed;
        }

        public long getTotalConnections() {
            return totalConnections;
        }

        public long getTotalDisconnections() {
            return totalDisconnections;
        }

        public double getSuccessRate() {
            return successRate;
        }

        public double getAverageMessagesPerMinute() {
            return averageMessagesPerMinute;
        }
    }
}