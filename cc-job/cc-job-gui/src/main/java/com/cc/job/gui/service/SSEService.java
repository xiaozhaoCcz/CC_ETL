package com.cc.job.gui.service;

import com.cc.job.gui.util.AppConfig;
import com.google.gson.Gson;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * SSE服务类，用于接收后端推送的节点状态更新消息
 * 替代WebSocket实现
 */
public class SSEService {
    
    private static final Logger logger = LoggerFactory.getLogger(SSEService.class);
    
    private static SSEService instance;
    private final Map<String, SSEConnection> connections = new ConcurrentHashMap<>();
    private final Gson gson = new Gson();
    
    /**
     * SSE消息接口
     */
    public static class SSEMessage {
        private Long jobId;
        private String randomId;
        private Integer status; // 0=失败, 1=成功, 2=运行中
        private String result;
        private Long parentJobId;
        
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
        
        public Long getParentJobId() {
            return parentJobId;
        }
        
        public void setParentJobId(Long parentJobId) {
            this.parentJobId = parentJobId;
        }
    }
    
    /**
     * SSE连接包装类
     */
    private class SSEConnection {
        private final String connectionId;
        private final Consumer<SSEMessage> messageHandler;
        private Thread connectionThread;
        private volatile boolean running = false;
        private String sseUrl;
        private HttpURLConnection connection;
        
        public SSEConnection(String connectionId, Consumer<SSEMessage> messageHandler) {
            this.connectionId = connectionId;
            this.messageHandler = messageHandler;
        }
        
        public void connect() {
            if (running) {
                return;
            }
            
            // 构建SSE URL
            String baseUrl = AppConfig.getBaseUrl();
            // 移除尾部的 /api 等路径
            if (baseUrl.endsWith("/api")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 4);
            }
            
            // 解析连接ID获取parentJobId和randomId
            String[] parts = connectionId.split(":");
            if (parts.length != 2) {
                logger.error("❌ 连接ID格式错误，应为 parentJobId:randomId，实际: {}", connectionId);
                return;
            }
            
            String parentJobId = parts[0];
            String randomId = parts[1];
            sseUrl = baseUrl + "/api/v1/sse/nodeStatus/" + parentJobId + "/" + randomId;
            
            
            running = true;
            connectionThread = new Thread(() -> {
                connectAndListen();
            }, "SSE-Connection-" + connectionId);
            connectionThread.setDaemon(true);
            connectionThread.start();
        }
        
        private void connectAndListen() {
            BufferedReader reader = null;
            try {
                URL url = new URL(sseUrl);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept", "text/event-stream");
                connection.setRequestProperty("Cache-Control", "no-cache");
                connection.setConnectTimeout(10000); // 10秒连接超时
                // ⭐ 设置读取超时为30秒，这样readLine()会在超时时抛出SocketTimeoutException，可以被中断
                // 如果不设置超时，readLine()会无限阻塞，无法被interrupt()中断
                connection.setReadTimeout(30000); // 30秒读取超时
                
                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    logger.error("❌ SSE连接失败，HTTP状态码: {}", responseCode);
                    running = false;
                    return;
                }
                
                
                reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
                String line;
                StringBuilder eventData = new StringBuilder();
                String eventType = null;
                
                // 使用可中断的读取方式
                while (running && !Thread.currentThread().isInterrupted()) {
                    try {
                        line = reader.readLine();
                        if (line == null) {
                            // 流结束
                            break;
                        }
                    } catch (java.net.SocketTimeoutException e) {
                        // 读取超时，检查running状态，如果还在运行则继续等待
                        if (!running || Thread.currentThread().isInterrupted()) {
                            break;
                        }
                        // 继续循环，等待下一次读取
                        continue;
                    } catch (java.io.IOException e) {
                        // IO异常，可能是连接断开
                        if (running) {
                            logger.debug("SSE读取数据时发生IO异常: {}", e.getMessage());
                        }
                        break;
                    }
                    
                    // 处理读取到的行
                    if (line.isEmpty()) {
                        // 空行表示事件结束，处理事件
                        if (eventData.length() > 0 && eventType != null) {
                            processEvent(eventType, eventData.toString());
                            eventData.setLength(0);
                            eventType = null;
                        }
                    } else if (line.startsWith("event:")) {
                        eventType = line.substring(6).trim();
                    } else if (line.startsWith("data:")) {
                        String data = line.substring(5).trim();
                        if (eventData.length() > 0) {
                            eventData.append("\n");
                        }
                        eventData.append(data);
                    } else if (line.startsWith("id:")) {
                        // 忽略事件ID
                    } else if (line.startsWith("retry:")) {
                        // 忽略重试间隔
                    }
                }
            } catch (Exception e) {
                if (running) {
                    String errorMsg = "❌ SSE连接错误: " + e.getMessage();
                    logger.error(errorMsg);
                    logger.error("   连接ID: {}", connectionId);
                    logger.error("   连接URL: {}", sseUrl);
                    
                    // 通过messageHandler传递错误信息
                    if (messageHandler != null) {
                        Platform.runLater(() -> {
                            SSEMessage errorMessage = new SSEMessage();
                            errorMessage.setJobId(null);
                            errorMessage.setStatus(-1); // 使用-1表示连接错误
                            String[] parts = connectionId.split(":");
                            if (parts.length == 2) {
                                errorMessage.setRandomId(parts[1]);
                            }
                            errorMessage.setResult("SSE连接错误: " + e.getMessage() + " (URL: " + sseUrl + ")");
                            messageHandler.accept(errorMessage);
                        });
                    }
                }
            } finally {
                running = false;
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (Exception e) {
                        // 忽略
                    }
                }
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }
        
        private void processEvent(String eventType, String data) {
            
            try {
                if ("ping".equals(eventType)) {
                    // 心跳消息，忽略
                    return;
                }
                
                if ("connected".equals(eventType)) {
                    // 连接成功消息
                    return;
                }
                
                // 解析JSON消息
                SSEMessage sseMessage = gson.fromJson(data, SSEMessage.class);
                
                
                Platform.runLater(() -> {
                    if (messageHandler != null) {
                        messageHandler.accept(sseMessage);
                    } else {
                        logger.error("❌ 消息处理器为null！");
                    }
                });
            } catch (Exception e) {
                logger.error("❌ 解析SSE消息失败: {}", e.getMessage(), e);
            }
            
        }
        
        public void disconnect() {
            running = false;
            
            // 先中断连接线程
            if (connectionThread != null && connectionThread.isAlive()) {
                connectionThread.interrupt();
            }
            
            // 然后断开HTTP连接
            if (connection != null) {
                try {
                    connection.disconnect();
                } catch (Exception e) {
                    logger.debug("断开SSE连接时发生异常: {}", e.getMessage());
                }
            }
            
            // 等待线程结束（最多等待1秒）
            if (connectionThread != null && connectionThread.isAlive()) {
                try {
                    connectionThread.join(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        
        public boolean isConnected() {
            return running && connection != null;
        }
    }
    
    private SSEService() {
    }
    
    public static SSEService getInstance() {
        if (instance == null) {
            instance = new SSEService();
        }
        return instance;
    }
    
    /**
     * 连接SSE（使用任务组ID和随机ID）
     * @param jobId 任务组ID
     * @param randomId 随机ID
     * @param messageHandler 消息处理回调
     */
    public void connect(Long jobId, String randomId, Consumer<SSEMessage> messageHandler) {
        String connectionId = jobId + ":" + randomId;
        
        // 如果已存在连接，先断开
        disconnect(jobId, randomId);
        
        SSEConnection sseConnection = new SSEConnection(connectionId, messageHandler);
        connections.put(connectionId, sseConnection);
        sseConnection.connect();
        
        
        // 等待一段时间后检查连接状态
        new Thread(() -> {
            try {
                Thread.sleep(2000); // 等待2秒
                if (!sseConnection.isConnected()) {
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
    
    /**
     * 断开SSE连接
     */
    public void disconnect(Long jobId, String randomId) {
        String connectionId = jobId + ":" + randomId;
        SSEConnection sseConnection = connections.remove(connectionId);
        if (sseConnection != null) {
            sseConnection.disconnect();
        }
    }
    
    /**
     * 检查SSE连接状态
     * @param jobId 任务组ID
     * @param randomId 随机ID
     * @return 是否已连接
     */
    public boolean isConnected(Long jobId, String randomId) {
        String connectionId = jobId + ":" + randomId;
        SSEConnection sseConnection = connections.get(connectionId);
        return sseConnection != null && sseConnection.isConnected();
    }
    
    /**
     * 断开所有连接
     */
    public void disconnectAll() {
        connections.values().forEach(SSEConnection::disconnect);
        connections.clear();
    }
}

