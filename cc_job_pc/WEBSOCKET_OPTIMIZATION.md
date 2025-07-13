# WebSocket 代码优化总结

## 优化概述

本次优化将原有的WebSocket代码重构为更加模块化、可维护的架构，提供了更好的连接管理、错误处理和消息处理机制。

## 主要优化内容

### 1. 创建了专门的WebSocket管理工具类

#### `src/utils/websocket.ts`
- **WebSocketManager类**: 提供单个WebSocket连接的管理
- **WebSocketPool类**: 管理多个WebSocket连接，支持按任务组ID管理
- **连接状态管理**: 自动重连、心跳检测、错误处理
- **类型安全**: 完整的TypeScript类型定义

#### 主要特性：
- ✅ 自动重连机制（可配置重连次数和延迟）
- ✅ 心跳检测（30秒间隔，5秒超时）
- ✅ 连接状态监控
- ✅ 错误处理和日志记录
- ✅ 手动关闭和自动清理

### 2. 创建了WebSocket消息处理器

#### `src/utils/websocketHandler.ts`
- **WebSocketMessageHandler类**: 专门处理业务逻辑相关的消息
- **消息分类处理**: 根据消息状态码进行不同处理
- **节点状态更新**: 自动更新节点颜色和状态
- **任务组完成检测**: 智能检测任务组是否全部完成

#### 主要功能：
- ✅ 任务完成消息处理（状态码5）
- ✅ 运行时信息处理（状态码9）
- ✅ 节点状态更新和颜色变化
- ✅ 任务组完成状态检测
- ✅ 节点状态统计信息

### 3. 优化了main.vue中的WebSocket使用

#### 主要改进：
- ✅ 使用连接池管理多个WebSocket连接
- ✅ 统一的消息处理机制
- ✅ 更好的错误处理和用户提示
- ✅ 组件卸载时自动清理连接
- ✅ 支持按任务组ID管理连接

## 代码结构对比

### 优化前的问题：
```typescript
// 原始代码问题
const ws = ref(); // 全局单一连接
const reconnectAttempts = ref(0);
const maxReconnectAttempts = ref(3);

const connectWs = (id: string, targetJobId?: number): void => {
  // 手动管理连接状态
  // 重复的重连逻辑
  // 复杂的消息处理逻辑
  // 缺乏类型安全
};
```

### 优化后的架构：
```typescript
// 新的模块化架构
import { webSocketPool, WebSocketMessageHandler } from "@/utils/websocket";

// 连接管理
const connection = webSocketPool.getConnection(id, targetJobId, callbacks);
connection.connect();

// 消息处理
const wsMessageHandler = new WebSocketMessageHandler(/* 参数 */);
wsMessageHandler.handleMessage(message);
```

## 性能优化

### 1. 连接复用
- 使用连接池避免重复创建连接
- 按任务组ID管理连接，避免冲突

### 2. 内存管理
- 自动清理无效连接
- 组件卸载时清理所有连接
- 避免内存泄漏

### 3. 错误恢复
- 智能重连机制
- 连接状态监控
- 用户友好的错误提示

## 使用示例

### 创建WebSocket连接
```typescript
// 使用连接池创建连接
const connection = webSocketPool.getConnection(id, targetJobId, {
  onMessage: (message) => {
    wsMessageHandler.handleMessage(message);
  },
  onError: (event) => {
    console.error("WebSocket错误:", event);
  },
  onReconnect: (attempt) => {
    console.log(`重连尝试 ${attempt}`);
  },
});

connection.connect();
```

### 处理消息
```typescript
// 消息处理器自动处理不同类型的消息
const handler = new WebSocketMessageHandler(
  lfInstances.value,
  usePageStoreHook,
  logTabs,
  runTime,
  jobId
);

// 自动处理节点状态更新、任务完成等
handler.handleMessage(message);
```

### 清理连接
```typescript
// 关闭特定连接
webSocketPool.closeConnection(id, targetJobId);

// 清理所有连接
webSocketPool.closeAllConnections();
```

## 配置选项

### WebSocket配置
```typescript
const config = {
  maxReconnectAttempts: 3,    // 最大重连次数
  reconnectDelay: 3000,       // 重连延迟（毫秒）
  heartbeatInterval: 30000,   // 心跳间隔（毫秒）
  heartbeatTimeout: 5000,     // 心跳超时（毫秒）
};
```

### 环境变量
```env
VITE_APP_WS_ENDPOINT=ws://localhost:8080/websocket/
```

## 向后兼容性

- ✅ 保持了原有的API接口
- ✅ 支持现有的消息格式
- ✅ 兼容现有的业务逻辑
- ✅ 渐进式迁移，可以逐步替换

## 测试建议

### 1. 连接测试
- 测试正常连接建立
- 测试网络断开重连
- 测试连接超时处理

### 2. 消息处理测试
- 测试各种状态码的消息处理
- 测试节点状态更新
- 测试任务组完成检测

### 3. 性能测试
- 测试多连接并发
- 测试内存使用情况
- 测试长时间运行稳定性

## 后续优化建议

1. **监控和日志**: 添加更详细的连接状态监控
2. **配置管理**: 支持动态配置WebSocket参数
3. **消息队列**: 添加消息队列处理机制
4. **断线重连**: 优化断线重连的用户体验
5. **性能监控**: 添加WebSocket性能指标监控

## 总结

通过这次优化，WebSocket代码变得更加：
- 🎯 **模块化**: 职责分离，易于维护
- 🔒 **类型安全**: 完整的TypeScript支持
- 🚀 **高性能**: 连接池和智能重连
- 🛡️ **稳定可靠**: 完善的错误处理
- 📈 **可扩展**: 易于添加新功能

这些改进大大提升了WebSocket连接的稳定性和可维护性，为用户提供了更好的实时通信体验。 