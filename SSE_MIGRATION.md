# SSE替代WebSocket实现说明

## 概述

本次修改将项目中用于节点状态推送的WebSocket实现替换为SSE（Server-Sent Events），以降低资源消耗和实现复杂度。

## 修改内容

### 1. 后端（admin端）

#### 新增文件
- `cc-job/cc-job-admin/src/main/java/com/cc/job/admin/task/sse/SSEService.java`
  - SSE服务类，管理SSE连接和消息推送
  - 支持连接池管理、自动清理过期连接
  - 提供消息发送、连接关闭等功能

- `cc-job/cc-job-admin/src/main/java/com/cc/job/admin/task/sse/SSEController.java`
  - SSE控制器，提供SSE连接端点
  - 路径：`/api/v1/sse/nodeStatus/{parentJobId}/{randomId}`
  - 支持连接建立和关闭

#### 修改文件
- `cc-job/cc-job-admin/src/main/java/com/cc/job/admin/task/handler/JobGroupXxlJob.java`
  - 将 `WebSocketServer` 替换为 `SSEService`
  - 所有 `webSocketServer.sendInfo()` 调用改为 `sseService.sendMessage()`
  - 连接关闭从 `webSocketServer.onClose()` 改为 `sseService.closeConnection()`

### 2. GUI端（JavaFX）

#### 新增文件
- `cc-job/cc-job-gui/src/main/java/com/cc/job/gui/service/SSEService.java`
  - SSE客户端服务，使用HTTP长连接接收服务器推送
  - 支持自动重连、消息解析、错误处理

#### 修改文件
- `cc-job/cc-job-gui/src/main/java/com/cc/job/gui/view/MainView.java`
  - 将 `WebSocketService` 替换为 `SSEService`
  - 将 `handleWebSocketMessage()` 改为 `handleSSEMessage()`
  - 更新所有连接和断开连接的调用

### 3. 前端（Vue）

#### 修改文件
- `cc-job-web/src/views/task/job-platform/index.vue`
  - 将 `WebSocket` 替换为 `EventSource`
  - `connectWs()` 改为 `connectSSE()`
  - 添加 `disconnectSSE()` 方法
  - 更新消息监听逻辑

- `cc-job-web/src/views/task/task-rank/index.vue`
  - 将 `WebSocket` 替换为 `EventSource`
  - `connectWs()` 改为 `connectSSE()`
  - 添加 `disconnectSSE()` 方法
  - 更新消息监听逻辑

## 技术对比

### WebSocket vs SSE

| 特性 | WebSocket | SSE |
|------|-----------|-----|
| 协议 | 独立协议（ws://） | HTTP长连接 |
| 双向通信 | 支持 | 仅服务器→客户端 |
| 实现复杂度 | 较高 | 较低 |
| 资源消耗 | 较高（需要维护TCP连接和线程） | 较低（HTTP连接） |
| 浏览器支持 | 良好 | 原生支持 |
| 自动重连 | 需手动实现 | 浏览器自动处理 |

### 为什么选择SSE

1. **单向推送场景**：节点状态更新只需要服务器向客户端推送，不需要客户端向服务器发送消息
2. **实现简单**：SSE基于HTTP，无需额外的协议处理
3. **资源消耗低**：相比WebSocket，SSE的资源占用更少
4. **自动重连**：浏览器原生支持自动重连机制

## API变更

### 连接端点
- **旧**：`ws://host/ccJobWs/{parentJobId}:{randomId}`
- **新**：`GET /api/v1/sse/nodeStatus/{parentJobId}/{randomId}`

### 消息格式
保持不变，仍使用 `Message` 类：
```json
{
  "jobId": 123,
  "parentJobId": 100,
  "randomId": "abc-def",
  "status": 2,
  "result": "..."
}
```

### 事件类型
- `connected`：连接成功确认
- `nodeStatus`：节点状态更新
- `ping`：心跳消息（用于连接保活）

## 使用说明

### 前端使用示例

```typescript
// 连接SSE
const eventSource = new EventSource(
  `/api/v1/sse/nodeStatus/${parentJobId}/${randomId}`
);

// 监听节点状态更新
eventSource.addEventListener("nodeStatus", (e) => {
  const message = JSON.parse(e.data);
  updateNodeStatus(message.jobId, message.status);
});

// 监听连接成功
eventSource.addEventListener("connected", (e) => {
  console.log("SSE连接成功");
});

// 关闭连接
eventSource.close();
```

### 后端发送消息示例

```java
Message message = new Message();
message.setJobId(jobId);
message.setParentJobId(parentJobId);
message.setStatus(status);
message.setRandomId(randomId);

sseService.sendMessage(message);
```

## 注意事项

1. **连接超时**：SSE连接默认30分钟超时，超时后会自动清理
2. **重连机制**：前端实现了自动重连，最多重试3次
3. **消息格式**：保持与WebSocket版本一致，确保兼容性
4. **向后兼容**：WebSocket相关代码已保留但不再使用，如需回退可恢复

## 测试建议

1. **功能测试**：
   - 启动任务组，验证节点状态实时更新
   - 测试任务完成后的连接关闭
   - 测试任务停止后的连接关闭

2. **性能测试**：
   - 对比SSE和WebSocket的资源占用
   - 测试多任务组并发时的连接数

3. **异常测试**：
   - 测试网络断开后的自动重连
   - 测试服务器重启后的连接恢复

## 后续优化建议

1. 可以考虑添加SSE连接监控和统计功能
2. 可以优化消息批量发送机制
3. 可以添加连接心跳检测机制

