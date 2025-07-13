# WebSocket 响应优化方案

## 问题分析

原有的WebSocket实现存在以下问题导致响应延迟：

1. **同步发送机制**：使用`synchronized`锁导致阻塞
2. **单线程发送**：没有异步处理机制
3. **连接管理不当**：缺乏心跳检测和连接状态管理
4. **消息缓冲不足**：没有消息队列机制

## 优化方案

### 方案1：优化现有WebSocket实现

#### 主要改进：

1. **异步发送机制**
   - 使用专门的线程池处理消息发送
   - 避免阻塞主线程
   - 支持批量发送

2. **连接状态管理**
   - 添加心跳检测机制
   - 自动清理无效连接
   - 连接状态监控

3. **消息缓冲**
   - 实现消息队列
   - 批量处理消息
   - 减少网络开销

#### 核心代码：

```java
// 异步发送消息
public void sendMessageAsync(Session session, Message message) {
    if (session != null && session.isOpen()) {
        MESSAGE_EXECUTOR.submit(() -> {
            try {
                session.getBasicRemote().sendObject(message);
            } catch (Exception e) {
                removeInvalidSession(session);
            }
        });
    }
}

// 批量发送消息
public void sendBatchInfo(List<Message> messages) {
    // 按会话分组并异步发送
}
```

### 方案2：Server-Sent Events (SSE)

#### 优势：
- 基于HTTP协议，更稳定
- 自动重连机制
- 浏览器原生支持
- 更简单的实现

#### 使用方式：

```javascript
// 前端连接
const eventSource = new EventSource('/api/sse/123/abc');
eventSource.onmessage = function(event) {
    const data = JSON.parse(event.data);
    // 处理消息
};
```

### 方案3：消息队列 + WebSocket 混合方案

#### 架构：
1. **消息队列服务**：缓存和批量处理消息
2. **WebSocket服务**：实时推送消息
3. **配置管理**：灵活的参数配置

#### 核心组件：

- `MessageQueueService`：消息队列管理
- `WebSocketServer`：优化后的WebSocket服务
- `WebSocketProperties`：配置管理

## 性能优化建议

### 1. 服务器端优化

```yaml
# application-websocket.yml
cc:
  job:
    websocket:
      async-enabled: true
      core-pool-size: 10
      max-pool-size: 20
      heartbeat-interval: 30000
      batch-interval: 100
      max-batch-size: 10
```

### 2. 客户端优化

```javascript
// 使用优化后的WebSocket客户端
const ws = new OptimizedWebSocket(url, {
    reconnectInterval: 2000,
    maxReconnectAttempts: 10,
    heartbeatInterval: 25000,
    messageBufferSize: 50
});
```

### 3. 网络优化

- 使用CDN加速
- 启用HTTP/2
- 配置合适的超时时间
- 启用压缩

## 监控和调试

### 1. 连接状态监控

```java
// 获取连接统计
String stats = WebSocketServer.getConnectionStats();
```

### 2. 消息队列监控

```java
// 获取队列统计
String queueStats = messageQueueService.getQueueStats();
```

### 3. 前端调试

```javascript
// 监听连接状态
ws.on('open', () => console.log('连接已建立'));
ws.on('close', () => console.log('连接已断开'));
ws.on('error', (error) => console.error('连接错误:', error));
```

## 部署建议

### 1. 负载均衡

- 使用Redis存储会话信息
- 配置WebSocket代理
- 启用会话粘性

### 2. 监控告警

- 监控连接数
- 监控消息发送延迟
- 设置异常告警

### 3. 性能测试

- 压力测试连接数
- 测试消息发送延迟
- 测试并发处理能力

## 使用示例

### 1. 启动优化后的WebSocket

```java
// 在JobGroupXxlJob中使用
@Autowired
private MessageQueueService messageQueueService;

// 发送消息
messageQueueService.addMessage(message);
```

### 2. 前端连接

```javascript
// 创建WebSocket连接
const ws = createJobWebSocket(jobId, randomId);

// 监听消息
ws.on('message', (data) => {
    console.log('收到消息:', data);
});
```

## 预期效果

1. **响应时间**：从原来的100-500ms降低到10-50ms
2. **连接稳定性**：自动重连和心跳检测
3. **并发处理**：支持更多并发连接
4. **资源利用**：更高效的线程和内存使用

## 注意事项

1. 确保服务器有足够的内存和CPU资源
2. 监控网络带宽使用情况
3. 定期清理无效连接
4. 根据实际负载调整配置参数 