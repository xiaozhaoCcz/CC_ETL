package com.cc.job.admin.task.websocket;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

/**
 * WebSocket配置处理器
 * 支持连接池配置、超时设置、消息大小限制等
 * 
 * @author xiaozhao
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig {

    @Value("${websocket.max-connections:10000}")
    private int maxConnections;

    @Value("${websocket.connection-timeout:1800000}")
    private long connectionTimeout;

    @Value("${websocket.message-timeout:5000}")
    private long messageTimeout;

    @Value("${websocket.max-text-message-size:65536}")
    private int maxTextMessageSize;

    @Value("${websocket.max-binary-message-size:65536}")
    private int maxBinaryMessageSize;

    @Value("${websocket.async-send-timeout:5000}")
    private long asyncSendTimeout;

    /**
     * ServerEndpointExporter 作用
     * <p>
     * 这个Bean会自动注册使用@ServerEndpoint注解声明的websocket endpoint
     *
     * @return
     */
    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }

    /**
     * 获取最大连接数配置
     */
    public int getMaxConnections() {
        return maxConnections;
    }

    /**
     * 获取连接超时时间配置
     */
    public long getConnectionTimeout() {
        return connectionTimeout;
    }

    /**
     * 获取消息超时时间配置
     */
    public long getMessageTimeout() {
        return messageTimeout;
    }

    /**
     * 获取最大文本消息大小配置
     */
    public int getMaxTextMessageSize() {
        return maxTextMessageSize;
    }

    /**
     * 获取最大二进制消息大小配置
     */
    public int getMaxBinaryMessageSize() {
        return maxBinaryMessageSize;
    }

    /**
     * 获取异步发送超时时间配置
     */
    public long getAsyncSendTimeout() {
        return asyncSendTimeout;
    }
}
