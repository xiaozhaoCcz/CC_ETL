package com.cc.job.admin.task.controller;

import com.cc.job.admin.task.websocket.WebSocketManager;
import com.cc.job.admin.task.websocket.WebSocketServer;
import com.cc.job.xo.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * WebSocket监控控制器
 * 提供WebSocket连接状态和性能统计的REST API
 * 
 * @author xiaozhao
 */
@RestController
@RequestMapping("/api/websocket")
@RequiredArgsConstructor
public class WebSocketMonitorController {

    private static final Logger log = LoggerFactory.getLogger(WebSocketMonitorController.class);

    private final WebSocketManager webSocketManager;

    /**
     * 获取WebSocket连接统计信息
     */
    @GetMapping("/stats")
    public Result<WebSocketServer.ConnectionStats> getConnectionStats() {
        try {
            WebSocketServer.ConnectionStats stats = WebSocketServer.getConnectionStats();
            return Result.success(stats);
        } catch (Exception e) {
            log.error("Failed to get WebSocket connection stats", e);
            return Result.failed("获取WebSocket连接统计信息失败");
        }
    }

    /**
     * 获取WebSocket性能统计信息
     */
    @GetMapping("/performance")
    public Result<WebSocketManager.PerformanceStats> getPerformanceStats() {
        try {
            WebSocketManager.PerformanceStats stats = webSocketManager.getPerformanceStats();
            return Result.success(stats);
        } catch (Exception e) {
            log.error("Failed to get WebSocket performance stats", e);
            return Result.failed("获取WebSocket性能统计信息失败");
        }
    }

    /**
     * 获取WebSocket健康状态
     */
    @GetMapping("/health")
    public Result<WebSocketHealthInfo> getHealthInfo() {
        try {
            WebSocketServer.ConnectionStats connectionStats = WebSocketServer.getConnectionStats();
            WebSocketManager.PerformanceStats performanceStats = webSocketManager.getPerformanceStats();

            WebSocketHealthInfo healthInfo = new WebSocketHealthInfo();
            healthInfo.setOnline(connectionStats.getOnlineCount());
            healthInfo.setMaxConnections(connectionStats.getMaxConnections());
            healthInfo.setSuccessRate(performanceStats.getSuccessRate());
            healthInfo.setMessageCount(connectionStats.getMessageCount());
            healthInfo.setStatus("UP");

            // 检查健康状态
            if (connectionStats.getOnlineCount() > connectionStats.getMaxConnections() * 0.9) {
                healthInfo.setStatus("WARNING");
                healthInfo.setMessage("连接数接近限制");
            } else if (performanceStats.getSuccessRate() < 95.0) {
                healthInfo.setStatus("WARNING");
                healthInfo.setMessage("消息发送成功率较低");
            }

            return Result.success(healthInfo);
        } catch (Exception e) {
            log.error("Failed to get WebSocket health info", e);
            return Result.failed("获取WebSocket健康状态失败");
        }
    }

    /**
     * WebSocket健康信息
     */
    public static class WebSocketHealthInfo {
        private int online;
        private int maxConnections;
        private double successRate;
        private long messageCount;
        private String status;
        private String message;

        // Getters and Setters
        public int getOnline() {
            return online;
        }

        public void setOnline(int online) {
            this.online = online;
        }

        public int getMaxConnections() {
            return maxConnections;
        }

        public void setMaxConnections(int maxConnections) {
            this.maxConnections = maxConnections;
        }

        public double getSuccessRate() {
            return successRate;
        }

        public void setSuccessRate(double successRate) {
            this.successRate = successRate;
        }

        public long getMessageCount() {
            return messageCount;
        }

        public void setMessageCount(long messageCount) {
            this.messageCount = messageCount;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}