package com.cc.job.admin.task.websocket;

import com.cc.job.admin.task.websocket.model.Message;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.websocket.EncodeException;
import jakarta.websocket.Encoder;
import jakarta.websocket.EndpointConfig;

/**
 * WebSocket消息编码器
 * 使用单例ObjectMapper提高序列化性能
 * 
 * @author xiaozhao
 */
public class ServerEncoder implements Encoder.Text<Message> {

    // 使用单例ObjectMapper，避免重复创建
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public void destroy() {
        // 清理资源
    }

    @Override
    public void init(EndpointConfig config) {
        // 初始化配置
    }

    /**
     * 将Message对象编码为JSON字符串
     * 
     * @param message 要编码的消息对象
     * @return JSON字符串
     * @throws EncodeException 编码异常
     */
    @Override
    public String encode(Message message) throws EncodeException {
        try {
            return OBJECT_MAPPER.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new EncodeException(message, "Failed to encode message", e);
        }
    }
}
