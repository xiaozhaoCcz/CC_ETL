package com.cc.job.executor.compose.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 任务编排配置属性
 * 
 * <p>任务编排引擎的配置参数
 *
 * @author cc-job-team
 * @since 2025-12-02
 */
@Data
@Component
@ConfigurationProperties(prefix = "cc-job.orchestration")
public class OrchestrationProperties {

    /**
     * 默认任务超时时间（秒）
     */
    private long defaultTimeout = 300L;

    /**
     * 线程池核心线程数
     */
    private int corePoolSize = 10;

    /**
     * 线程池最大线程数
     */
    private int maxPoolSize = 100;

    /**
     * 队列容量
     */
    private int queueCapacity = 10000;

    /**
     * 线程存活时间（秒）
     */
    private long keepAliveTime = 60L;

    /**
     * 状态轮询间隔（毫秒）
     */
    private long statusPollInterval = 50L;

    /**
     * 最大等待时间（毫秒）
     */
    private long maxWaitTime = 5000L;

    /**
     * 是否启用任务预测
     */
    private boolean enablePrediction = true;

    /**
     * 是否启用详细日志
     */
    private boolean enableDetailedLogging = false;
}

