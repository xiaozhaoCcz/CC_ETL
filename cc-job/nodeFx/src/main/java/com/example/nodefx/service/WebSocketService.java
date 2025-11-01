package com.example.nodefx.service;

import com.example.nodefx.util.AppConfig;
import com.google.gson.Gson;
import javafx.application.Platform;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * WebSocket服务类，用于接收后端推送的节点状态更新消息
 */
public class WebSocketService {
    
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
                System.out.println("⚠️ WebSocket已经连接，跳过重复连接: " + connectionId);
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
            
            System.out.println("🔌 开始连接WebSocket");
            System.out.println("   连接ID: " + connectionId);
            System.out.println("   基础URL: " + baseUrl);
            System.out.println("   WebSocket URL: " + wsUrl);
            
            try {
                URI uri = new URI(wsUrl);
                client = new WebSocketClient(uri) {
                    @Override
                    public void onOpen(ServerHandshake handshake) {
                        isConnected = true;
                        System.out.println("✅ WebSocket连接已建立: " + connectionId);
                        System.out.println("   连接URL: " + wsUrl);
                        System.out.println("   握手状态码: " + handshake.getHttpStatus());
                        System.out.println("   握手状态消息: " + handshake.getHttpStatusMessage());
                    }
                    
                    @Override
                    public void onMessage(String message) {
                        System.out.println("========================================");
                        System.out.println("📨 WebSocket收到原始消息: " + message);
                        System.out.println("   连接ID: " + connectionId);
                        
                        // 注意：这里不能直接使用logPanel，因为WebSocketService没有logPanel引用
                        // 消息会通过messageHandler传递到MainView，在那里记录到logPanel
                        
                        try {
                            WebSocketMessage wsMessage = gson.fromJson(message, WebSocketMessage.class);
                            
                            System.out.println("✅ 消息解析成功:");
                            System.out.println("   jobId: " + wsMessage.getJobId());
                            System.out.println("   status: " + wsMessage.getStatus());
                            System.out.println("   randomId: " + wsMessage.getRandomId());
                            System.out.println("   result: " + wsMessage.getResult());
                            
                            Platform.runLater(() -> {
                                if (messageHandler != null) {
                                    System.out.println("🔄 调用消息处理器");
                                    messageHandler.accept(wsMessage);
                                } else {
                                    System.err.println("❌ 消息处理器为null！");
                                }
                            });
                        } catch (Exception e) {
                            System.err.println("❌ 解析WebSocket消息失败: " + e.getMessage());
                            e.printStackTrace();
                        }
                        
                        System.out.println("========================================");
                    }
                    
                    @Override
                    public void onClose(int code, String reason, boolean remote) {
                        isConnected = false;
                        System.out.println("🔌 WebSocket连接已关闭: " + connectionId + " (code: " + code + ", reason: " + reason + ")");
                    }
                    
                    @Override
                    public void onError(Exception ex) {
                        String errorMsg = "❌ WebSocket错误: " + ex.getMessage();
                        System.err.println(errorMsg);
                        System.err.println("   连接ID: " + connectionId);
                        System.err.println("   连接URL: " + wsUrl);
                        
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
                        
                        ex.printStackTrace();
                        isConnected = false;
                    }
                };
                
                // 在后台线程中连接
                new Thread(() -> {
                    try {
                        System.out.println("🔄 尝试连接WebSocket: " + wsUrl);
                        boolean connected = client.connectBlocking();
                        if (connected) {
                            System.out.println("✅ WebSocket连接成功（connectBlocking返回true）: " + connectionId);
                        } else {
                            System.err.println("❌ WebSocket连接失败（connectBlocking返回false）: " + connectionId);
                        }
                    } catch (InterruptedException e) {
                        System.err.println("❌ WebSocket连接被中断: " + e.getMessage());
                        System.err.println("   连接ID: " + connectionId);
                        Thread.currentThread().interrupt();
                    } catch (Exception e) {
                        System.err.println("❌ WebSocket连接异常: " + e.getMessage());
                        System.err.println("   连接ID: " + connectionId);
                        e.printStackTrace();
                    }
                }).start();
                
            } catch (Exception e) {
                System.err.println("❌ 创建WebSocket连接失败: " + e.getMessage());
                System.err.println("   连接ID: " + connectionId);
                System.err.println("   WebSocket URL: " + wsUrl);
                e.printStackTrace();
                isConnected = false;
            }
        }
        
        public void disconnect() {
            isConnected = false;
            if (client != null) {
                try {
                    client.close();
                } catch (Exception e) {
                    System.err.println("关闭WebSocket连接失败: " + e.getMessage());
                }
                client = null;
            }
            System.out.println("🔌 断开WebSocket连接: " + connectionId);
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
        
        System.out.println("🔌 WebSocket连接已创建: " + connectionId);
        
        // 等待一段时间后检查连接状态
        new Thread(() -> {
            try {
                Thread.sleep(2000); // 等待2秒
                if (!wrapper.isConnected()) {
                    System.err.println("⚠️ WebSocket连接超时，可能未成功建立: " + connectionId);
                    System.err.println("   请检查后端WebSocket服务是否正常运行");
                    System.err.println("   请检查URL是否正确: ws://.../ws/" + connectionId);
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
            System.out.println("🔌 WebSocket连接已断开: " + connectionId);
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
        System.out.println("🔌 所有WebSocket连接已断开");
    }
}

