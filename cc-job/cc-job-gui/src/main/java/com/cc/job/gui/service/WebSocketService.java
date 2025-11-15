package com.cc.job.gui.service;

import com.cc.job.gui.util.AppConfig;
import com.google.gson.Gson;
import javafx.application.Platform;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * WebSocket服务类，用于接收后端推送的节点状态更新消息
 */
public class WebSocketService {
    
    private static final Logger logger = LoggerFactory.getLogger(WebSocketService.class);
    
    private static WebSocketService instance;
    private final Map<String, WebSocketClientWrapper> connections = new ConcurrentHashMap<>();
    private final Gson gson = new Gson();
    
    /**
     * WebSocket消息接口
     */
    public static class WebSocketMessage {
        private Long jobId;
        private String randomId;
        private Integer status; // 0=失败, 1=成功, 2=运行中
        private String result;
        
        public Long getJobId() {
            return jobId;
        }
        
        public void setJobId(Long jobId) {
            this.jobId = jobId;
        }
        
        public String getRandomId() {
            return randomId;
        }
        
        public void setRandomId(String randomId) {
            this.randomId = randomId;
        }
        
        public Integer getStatus() {
            return status;
        }
        
        public void setStatus(Integer status) {
            this.status = status;
        }
        
        public String getResult() {
            return result;
        }
        
        public void setResult(String result) {
            this.result = result;
        }
    }
    
    /**
     * WebSocket客户端包装类
     */
    private class WebSocketClientWrapper {
        private final String connectionId;
        private final Consumer<WebSocketMessage> messageHandler;
        private WebSocketClient client;
        private boolean isConnected = false;
        private String wsUrl; // 保存WebSocket URL用于日志输出
        
        public WebSocketClientWrapper(String connectionId, Consumer<WebSocketMessage> messageHandler) {
            this.connectionId = connectionId;
            this.messageHandler = messageHandler;
        }
        
        public void connect() {
            if (client != null && isConnected) {
                logger.warn("⚠️ WebSocket已经连接，跳过重复连接: {}", connectionId);
                return; // 已经连接
            }
            
            // 构建WebSocket URL
            String baseUrl = AppConfig.getBaseUrl();
            // 将 http:// 或 https:// 替换为 ws:// 或 wss://
            wsUrl = baseUrl.replace("http://", "ws://").replace("https://", "wss://");
            // 移除尾部的 /api 等路径
            if (wsUrl.endsWith("/api")) {
                wsUrl = wsUrl.substring(0, wsUrl.length() - 4);
            }
            // 使用正确的WebSocket端点路径（与后端@ServerEndpoint匹配）
            wsUrl += "/ccJobWs/" + connectionId;
            
            logger.debug("🔌 开始连接WebSocket");
            logger.debug("   连接ID: {}", connectionId);
            logger.debug("   基础URL: {}", baseUrl);
            logger.debug("   WebSocket URL: {}", wsUrl);
            
            try {
                URI uri = new URI(wsUrl);
                client = new WebSocketClient(uri) {
                    @Override
                    public void onOpen(ServerHandshake handshake) {
                        isConnected = true;
                        logger.info("✅ WebSocket连接已建立: {}", connectionId);
                        logger.debug("   连接URL: {}", wsUrl);
                        logger.debug("   握手状态码: {}", handshake.getHttpStatus());
                        logger.debug("   握手状态消息: {}", handshake.getHttpStatusMessage());
                    }
                    
                    @Override
                    public void onMessage(String message) {
                        logger.debug("========================================");
                        logger.debug("📨 WebSocket收到原始消息: {}", message);
                        logger.debug("   连接ID: {}", connectionId);
                        
                        // 注意：这里不能直接使用logPanel，因为WebSocketService没有logPanel引用
                        // 消息会通过messageHandler传递到MainView，在那里记录到logPanel
                        
                        try {
                            WebSocketMessage wsMessage = gson.fromJson(message, WebSocketMessage.class);
                            
                            logger.debug("✅ 消息解析成功:");
                            logger.debug("   jobId: {}", wsMessage.getJobId());
                            logger.debug("   status: {}", wsMessage.getStatus());
                            logger.debug("   randomId: {}", wsMessage.getRandomId());
                            logger.debug("   result: {}", wsMessage.getResult());
                            
                            Platform.runLater(() -> {
                                if (messageHandler != null) {
                                    logger.debug("🔄 调用消息处理器");
                                    messageHandler.accept(wsMessage);
                                } else {
                                    logger.error("❌ 消息处理器为null！");
                                }
                            });
                        } catch (Exception e) {
                            logger.error("❌ 解析WebSocket消息失败: {}", e.getMessage(), e);
                        }
                        
                        logger.debug("========================================");
                    }
                    
                    @Override
                    public void onClose(int code, String reason, boolean remote) {
                        isConnected = false;
                        logger.debug("🔌 WebSocket连接已关闭: {} (code: {}, reason: {})", connectionId, code, reason);
                    }
                    
                    @Override
                    public void onError(Exception ex) {
                        String errorMsg = "❌ WebSocket错误: " + ex.getMessage();
                        logger.error(errorMsg);
                        logger.error("   连接ID: {}", connectionId);
                        logger.error("   连接URL: {}", wsUrl);
                        
                        // 尝试通过messageHandler传递错误信息（如果可能）
                        if (messageHandler != null) {
                            Platform.runLater(() -> {
                                // 创建一个错误消息对象
                                WebSocketMessage errorMessage = new WebSocketMessage();
                                errorMessage.setJobId(null);
                                errorMessage.setStatus(-1); // 使用-1表示连接错误
                                errorMessage.setRandomId(connectionId.split(":")[1]);
                                errorMessage.setResult("WebSocket连接错误: " + ex.getMessage() + " (URL: " + wsUrl + ")");
                                messageHandler.accept(errorMessage);
                            });
                        }
                        
                        isConnected = false;
                    }
                };
                
                // 在后台线程中连接
                new Thread(() -> {
                    try {
                        logger.debug("🔄 尝试连接WebSocket: {}", wsUrl);
                        boolean connected = client.connectBlocking();
                        if (connected) {
                            logger.info("✅ WebSocket连接成功（connectBlocking返回true）: {}", connectionId);
                        } else {
                            logger.error("❌ WebSocket连接失败（connectBlocking返回false）: {}", connectionId);
                        }
                    } catch (InterruptedException e) {
                        logger.error("❌ WebSocket连接被中断: {}", e.getMessage());
                        logger.error("   连接ID: {}", connectionId);
                        Thread.currentThread().interrupt();
                    } catch (Exception e) {
                        logger.error("❌ WebSocket连接异常: {}", e.getMessage(), e);
                        logger.error("   连接ID: {}", connectionId);
                    }
                }).start();
                
            } catch (Exception e) {
                logger.error("❌ 创建WebSocket连接失败: {}", e.getMessage(), e);
                logger.error("   连接ID: {}", connectionId);
                logger.error("   WebSocket URL: {}", wsUrl);
                isConnected = false;
            }
        }
        
        public void disconnect() {
            isConnected = false;
            if (client != null) {
                try {
                    client.close();
                } catch (Exception e) {
                    logger.error("关闭WebSocket连接失败: {}", e.getMessage(), e);
                }
                client = null;
            }
            logger.debug("🔌 断开WebSocket连接: {}", connectionId);
        }
        
        public boolean isConnected() {
            return isConnected && client != null && client.isOpen();
        }
    }
    
    private WebSocketService() {
    }
    
    public static WebSocketService getInstance() {
        if (instance == null) {
            instance = new WebSocketService();
        }
        return instance;
    }
    
    /**
     * 连接WebSocket（使用任务组ID和随机ID）
     * @param jobId 任务组ID
     * @param randomId 随机ID
     * @param messageHandler 消息处理回调
     */
    public void connect(Long jobId, String randomId, Consumer<WebSocketMessage> messageHandler) {
        String connectionId = jobId + ":" + randomId;
        
        // 如果已存在连接，先断开
        disconnect(jobId, randomId);
        
        WebSocketClientWrapper wrapper = new WebSocketClientWrapper(connectionId, messageHandler);
        connections.put(connectionId, wrapper);
        wrapper.connect();
        
        logger.debug("🔌 WebSocket连接已创建: {}", connectionId);
        
        // 等待一段时间后检查连接状态
        new Thread(() -> {
            try {
                Thread.sleep(2000); // 等待2秒
                if (!wrapper.isConnected()) {
                    logger.warn("⚠️ WebSocket连接超时，可能未成功建立: {}", connectionId);
                    logger.warn("   请检查后端WebSocket服务是否正常运行");
                    logger.warn("   请检查URL是否正确: ws://.../ws/{}", connectionId);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
    
    /**
     * 断开WebSocket连接
     */
    public void disconnect(Long jobId, String randomId) {
        String connectionId = jobId + ":" + randomId;
        WebSocketClientWrapper wrapper = connections.remove(connectionId);
        if (wrapper != null) {
            wrapper.disconnect();
            logger.debug("🔌 WebSocket连接已断开: {}", connectionId);
        }
    }
    
    /**
     * 检查WebSocket连接状态
     * @param jobId 任务组ID
     * @param randomId 随机ID
     * @return 是否已连接
     */
    public boolean isConnected(Long jobId, String randomId) {
        String connectionId = jobId + ":" + randomId;
        WebSocketClientWrapper wrapper = connections.get(connectionId);
        return wrapper != null && wrapper.isConnected();
    }
    
    /**
     * 断开所有连接
     */
    public void disconnectAll() {
        connections.values().forEach(WebSocketClientWrapper::disconnect);
        connections.clear();
        logger.debug("🔌 所有WebSocket连接已断开");
    }
}

