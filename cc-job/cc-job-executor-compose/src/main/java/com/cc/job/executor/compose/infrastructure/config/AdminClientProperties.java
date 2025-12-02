package com.cc.job.executor.compose.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Admin 客户端配置属性
 * 
 * <p>集中管理与 Admin 模块通信的配置
 *
 * @author cc-job-team
 * @since 2025-12-02
 */
@Data
@Component
@ConfigurationProperties(prefix = "cc-job.admin.client")
public class AdminClientProperties {

    /**
     * Admin 地址
     */
    private String address = "http://127.0.0.1:8989";

    /**
     * 访问令牌
     */
    private String accessToken;

    /**
     * 连接超时时间（毫秒）
     */
    private int connectTimeout = 10000;

    /**
     * 读取超时时间（毫秒）
     */
    private int readTimeout = 30000;

    /**
     * 重试次数
     */
    private int retryCount = 3;

    /**
     * 重试间隔（毫秒）
     */
    private int retryInterval = 1000;
}

