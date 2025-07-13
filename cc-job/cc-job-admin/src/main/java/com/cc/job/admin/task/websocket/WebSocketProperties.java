package com.cc.job.admin.task.websocket;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * WebSocket配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "cc.job.websocket")
public class WebSocketProperties {

    /**
     * 是否启用异步发送
     */
    private boolean asyncEnabled = true;

    /**
     * 消息发送线程池核心线程数
     */
    private int corePoolSize = 10;

    /**
     * 消息发送线程池最大线程数
     */
    private int maxPoolSize = 20;

    /**
     * 消息发送线程池队列大小
     */
    private int queueCapacity = 1000;

    /**
     * 心跳检测间隔（毫秒）
     */
    private long heartbeatInterval = 30000;

    /**
     * 批量发送间隔（毫秒）
     */
    private long batchInterval = 100;

    /**
     * 最大批量大小
     */
    private int maxBatchSize = 10;

    /**
     * 是否启用消息队列
     */
    private boolean messageQueueEnabled = true;

    /**
     * 连接超时时间（毫秒）
     */
    private long connectionTimeout = 60000;

    /**
     * 最大消息大小（字节）
     */
    private int maxMessageSize = 8192;
}