# 04 - Channel详解

Channel是Netty网络操作的抽象，代表一个与网络socket的连接。理解Channel是掌握Netty的关键。

## 1. 什么是Channel？

### 1.1 概念理解

```
传统Java Socket           Netty Channel
     │                         │
     │                         │
┌────▼─────┐            ┌──────▼──────┐
│  Socket  │            │   Channel   │
│          │            │             │
│ - read() │            │ - 异步API    │
│ - write()│            │ - 事件驱动   │
│ - 阻塞   │            │ - 功能更强   │
└──────────┘            └─────────────┘
```

**Channel可以理解为：**
- 传统Socket的增强版
- 网络IO操作的统一接口
- 异步和事件驱动的抽象

### 1.2 Channel的特点

1. **双向**：可以读也可以写
2. **异步**：所有IO操作都是异步的
3. **有状态**：有生命周期（注册→活跃→不活跃→注销）
4. **层次结构**：有父Channel（ServerChannel）

## 2. Channel的类型

### 2.1 常用Channel类型

```
Channel (interface)
    │
    ├── NioSocketChannel          ← TCP客户端
    ├── NioServerSocketChannel    ← TCP服务器
    ├── NioDatagramChannel        ← UDP
    ├── EpollSocketChannel        ← Linux优化版TCP客户端
    ├── EpollServerSocketChannel  ← Linux优化版TCP服务器
    └── OioSocketChannel          ← 阻塞IO（已废弃）
```

### 2.2 不同传输方式对比

| 传输方式 | Channel类型 | 优点 | 缺点 | 适用场景 |
|---------|------------|------|------|---------|
| NIO | NioSocketChannel | 跨平台 | 性能稍逊 | 通用场景 |
| Epoll | EpollSocketChannel | 性能最优 | 仅Linux | Linux服务器 |
| KQueue | KQueueSocketChannel | 性能优 | 仅MacOS/BSD | Mac开发 |
| OIO | OioSocketChannel | 简单 | 性能差 | 已废弃 |

### 2.3 选择合适的Channel

```java
// 方式1：跨平台（推荐）
EventLoopGroup group = new NioEventLoopGroup();
ServerBootstrap b = new ServerBootstrap();
b.channel(NioServerSocketChannel.class);

// 方式2：Linux优化（生产环境推荐）
EventLoopGroup group = new EpollEventLoopGroup();
ServerBootstrap b = new ServerBootstrap();
b.channel(EpollServerSocketChannel.class);

// 方式3：自动选择（推荐！）
public static Class<? extends ServerChannel> serverChannelClass() {
    if (Epoll.isAvailable()) {
        System.out.println("使用Epoll传输");
        return EpollServerSocketChannel.class;
    }
    System.out.println("使用NIO传输");
    return NioServerSocketChannel.class;
}

public static EventLoopGroup newEventLoopGroup() {
    if (Epoll.isAvailable()) {
        return new EpollEventLoopGroup();
    }
    return new NioEventLoopGroup();
}
```

## 3. Channel的生命周期

### 3.1 状态转换图

```
         ┌──────────────┐
         │ Unregistered │ ← 初始状态
         └──────┬───────┘
                │ channelRegistered()
         ┌──────▼───────┐
         │  Registered  │ ← 注册到EventLoop
         └──────┬───────┘
                │ channelActive()
         ┌──────▼───────┐
         │    Active    │ ← 连接建立，可以收发数据
         └──────┬───────┘
                │ channelInactive()
         ┌──────▼───────┐
         │   Inactive   │ ← 连接断开
         └──────┬───────┘
                │ channelUnregistered()
         ┌──────▼───────┐
         │ Unregistered │ ← 注销
         └──────────────┘
```

### 3.2 生命周期示例

```java
public class ChannelLifecycleHandler extends ChannelInboundHandlerAdapter {
    
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        System.out.println("1. handlerAdded - Handler被添加到Pipeline");
    }
    
    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        System.out.println("2. channelRegistered - Channel注册到EventLoop");
        super.channelRegistered(ctx);
    }
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("3. channelActive - Channel激活（连接建立）");
        System.out.println("   远程地址: " + ctx.channel().remoteAddress());
        System.out.println("   本地地址: " + ctx.channel().localAddress());
        super.channelActive(ctx);
    }
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println("4. channelRead - 接收到数据");
        super.channelRead(ctx, msg);
    }
    
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        System.out.println("5. channelReadComplete - 数据读取完成");
        super.channelReadComplete(ctx);
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("6. channelInactive - Channel不活跃（连接断开）");
        super.channelInactive(ctx);
    }
    
    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        System.out.println("7. channelUnregistered - Channel从EventLoop注销");
        super.channelUnregistered(ctx);
    }
    
    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        System.out.println("8. handlerRemoved - Handler从Pipeline移除");
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.err.println("X. exceptionCaught - 发生异常: " + cause.getMessage());
        ctx.close();
    }
}
```

**输出示例：**

```
服务器端：
1. handlerAdded - Handler被添加到Pipeline
2. channelRegistered - Channel注册到EventLoop
3. channelActive - Channel激活（连接建立）
   远程地址: /127.0.0.1:54321
   本地地址: /127.0.0.1:8080
4. channelRead - 接收到数据
5. channelReadComplete - 数据读取完成
6. channelInactive - Channel不活跃（连接断开）
7. channelUnregistered - Channel从EventLoop注销
8. handlerRemoved - Handler从Pipeline移除
```

## 4. Channel的核心方法

### 4.1 基本信息获取

```java
Channel channel = ctx.channel();

// 1. 连接信息
SocketAddress remoteAddress = channel.remoteAddress();  // 远程地址
SocketAddress localAddress = channel.localAddress();    // 本地地址
System.out.println("客户端: " + remoteAddress);
System.out.println("服务器: " + localAddress);

// 2. 状态查询
boolean isActive = channel.isActive();        // 是否活跃
boolean isOpen = channel.isOpen();            // 是否打开
boolean isWritable = channel.isWritable();    // 是否可写
System.out.println("连接活跃: " + isActive);
System.out.println("Channel打开: " + isOpen);
System.out.println("可写入: " + isWritable);

// 3. 配置信息
ChannelConfig config = channel.config();
int receiveBufferSize = config.getRecvByteBufAllocator().toString();
System.out.println("接收缓冲区: " + receiveBufferSize);

// 4. 元数据
ChannelMetadata metadata = channel.metadata();
System.out.println("是否有断开连接: " + metadata.hasDisconnect());

// 5. Channel ID（唯一标识）
ChannelId id = channel.id();
System.out.println("Channel ID: " + id.asShortText());
```

### 4.2 数据读写

#### 写入数据的三种方式

```java
// 方式1：write（只写到缓冲区，不发送）
channel.write("Hello");
channel.write("World");
channel.flush();  // 统一发送

// 方式2：writeAndFlush（写入并立即发送，推荐）
channel.writeAndFlush("Hello World");

// 方式3：使用ChannelHandlerContext
ctx.write("Hello");
ctx.flush();
// 或
ctx.writeAndFlush("Hello World");
```

**区别：**

```
channel.write()        →  从Pipeline尾部开始
ctx.write()           →  从当前Handler开始

Pipeline: [Handler1] → [Handler2] → [Handler3] → [Tail]
                           ↑
                         当前位置

channel.write():  [Handler1] → [Handler2] → [Handler3] → [Tail]
ctx.write():                   [Handler2] → [Handler3] → [Tail]
```

#### 完整的读写示例

```java
public class ReadWriteExample extends ChannelInboundHandlerAdapter {
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        
        // 示例1：发送字符串
        ByteBuf buffer = Unpooled.copiedBuffer("Hello Netty!", CharsetUtil.UTF_8);
        ChannelFuture future = channel.writeAndFlush(buffer);
        
        // 添加监听器，处理发送结果
        future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture f) throws Exception {
                if (f.isSuccess()) {
                    System.out.println("消息发送成功");
                } else {
                    System.err.println("消息发送失败: " + f.cause());
                }
            }
        });
        
        // 示例2：使用Lambda简化
        channel.writeAndFlush("Another message\n")
               .addListener(future1 -> {
                   if (future1.isSuccess()) {
                       System.out.println("发送成功");
                   }
               });
        
        // 示例3：关闭Channel
        channel.writeAndFlush("Goodbye!\n")
               .addListener(ChannelFutureListener.CLOSE);  // 发送完成后关闭
    }
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf in = (ByteBuf) msg;
        
        try {
            // 读取数据
            int readableBytes = in.readableBytes();
            System.out.println("收到 " + readableBytes + " 字节");
            
            // 方式1：转为字符串
            String message = in.toString(CharsetUtil.UTF_8);
            System.out.println("内容: " + message);
            
            // 方式2：逐字节读取
            while (in.isReadable()) {
                byte b = in.readByte();
                System.out.print((char) b);
            }
            
        } finally {
            // 重要：释放ByteBuf
            in.release();
        }
    }
}
```

### 4.3 Channel关闭

```java
// 方式1：直接关闭
channel.close();

// 方式2：关闭并监听
channel.close().addListener(future -> {
    if (future.isSuccess()) {
        System.out.println("Channel关闭成功");
    }
});

// 方式3：发送完毕后关闭（常用！）
channel.writeAndFlush("Goodbye")
       .addListener(ChannelFutureListener.CLOSE);

// 方式4：等待关闭完成
ChannelFuture closeFuture = channel.close();
closeFuture.sync();  // 阻塞等待关闭完成
```

## 5. ChannelFuture详解

### 5.1 什么是ChannelFuture？

所有Netty的IO操作都是异步的，返回ChannelFuture。

```
同步调用（传统）：
    result = doSomething();  ← 阻塞等待
    use(result);

异步调用（Netty）：
    future = doSomething();  ← 立即返回
    future.addListener(result -> {
        use(result);         ← 回调处理
    });
```

### 5.2 ChannelFuture的使用

```java
public class ChannelFutureExample {
    
    public static void main(String[] args) throws Exception {
        EventLoopGroup group = new NioEventLoopGroup();
        
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
             .channel(NioSocketChannel.class)
             .handler(new ChannelInitializer<SocketChannel>() {
                 @Override
                 protected void initChannel(SocketChannel ch) {
                     // 配置Pipeline
                 }
             });
            
            // 连接服务器（异步）
            ChannelFuture connectFuture = b.connect("localhost", 8080);
            
            // 方式1：添加监听器（推荐）
            connectFuture.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if (future.isSuccess()) {
                        System.out.println("连接成功！");
                        Channel channel = future.channel();
                        channel.writeAndFlush("Hello Server!");
                    } else {
                        System.err.println("连接失败: " + future.cause());
                    }
                }
            });
            
            // 方式2：使用Lambda
            connectFuture.addListener(future -> {
                if (future.isSuccess()) {
                    System.out.println("连接成功（Lambda）");
                }
            });
            
            // 方式3：同步等待（不推荐，失去异步优势）
            connectFuture.sync();  // 阻塞直到连接完成
            if (connectFuture.isSuccess()) {
                System.out.println("连接成功（同步）");
            }
            
            // 方式4：等待一段时间
            boolean connected = connectFuture.await(5, TimeUnit.SECONDS);
            if (connected && connectFuture.isSuccess()) {
                System.out.println("在5秒内连接成功");
            }
            
            // 获取Channel
            Channel channel = connectFuture.channel();
            
            // 等待Channel关闭
            channel.closeFuture().sync();
            
        } finally {
            group.shutdownGracefully();
        }
    }
}
```

### 5.3 ChannelFuture的状态

```
┌─────────────┐
│   未完成     │ ← isDone() = false
└──────┬──────┘
       │
       │ 操作完成
       ↓
┌─────────────┬─────────────┐
│    成功      │    失败      │
│isSuccess()=T │isSuccess()=F │
│              │cause()!=null │
└─────────────┴─────────────┘
```

**状态检查方法：**

```java
ChannelFuture future = channel.writeAndFlush("Hello");

// 检查是否完成
boolean done = future.isDone();

// 检查是否成功
boolean success = future.isSuccess();

// 检查是否可取消
boolean cancellable = future.isCancellable();

// 获取失败原因
if (!future.isSuccess()) {
    Throwable cause = future.cause();
    cause.printStackTrace();
}

// 取消操作
future.cancel(false);
```

### 5.4 内置的ChannelFutureListener

Netty提供了几个常用的监听器：

```java
// 1. CLOSE：操作完成后关闭Channel
channel.writeAndFlush("Goodbye")
       .addListener(ChannelFutureListener.CLOSE);

// 2. CLOSE_ON_FAILURE：失败时关闭Channel
channel.writeAndFlush(data)
       .addListener(ChannelFutureListener.CLOSE_ON_FAILURE);

// 3. FIRE_EXCEPTION_ON_FAILURE：失败时触发异常事件
channel.writeAndFlush(data)
       .addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);

// 4. 组合多个监听器
ChannelFutureListener combined = ChannelFutureListener.compose(
    listener1, listener2, listener3
);
channel.writeAndFlush(data).addListener(combined);
```

## 6. Channel配置

### 6.1 常用配置选项

```java
ServerBootstrap b = new ServerBootstrap();
b.group(bossGroup, workerGroup)
 .channel(NioServerSocketChannel.class)
 
 // 服务器Channel配置
 .option(ChannelOption.SO_BACKLOG, 128)        // 连接队列大小
 .option(ChannelOption.SO_REUSEADDR, true)     // 端口复用
 
 // 子Channel配置（客户端连接）
 .childOption(ChannelOption.SO_KEEPALIVE, true)     // 保持连接
 .childOption(ChannelOption.TCP_NODELAY, true)      // 禁用Nagle算法
 .childOption(ChannelOption.SO_SNDBUF, 32 * 1024)   // 发送缓冲区
 .childOption(ChannelOption.SO_RCVBUF, 32 * 1024)   // 接收缓冲区
 .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK,
              new WriteBufferWaterMark(8 * 1024, 32 * 1024));  // 写缓冲区水位
```

### 6.2 配置选项详解

| 选项 | 说明 | 默认值 | 建议 |
|------|------|--------|------|
| SO_BACKLOG | TCP连接队列大小 | 128 | 高并发时增大 |
| SO_REUSEADDR | 端口复用 | false | 建议true |
| SO_KEEPALIVE | TCP保活 | false | 长连接时true |
| TCP_NODELAY | 禁用Nagle算法 | false | 低延迟时true |
| SO_SNDBUF | 发送缓冲区 | 系统默认 | 根据需求调整 |
| SO_RCVBUF | 接收缓冲区 | 系统默认 | 根据需求调整 |

**配置示例：**

```java
public class ChannelConfigExample {
    
    public static void configureServer(ServerBootstrap bootstrap) {
        bootstrap
            // Boss配置
            .option(ChannelOption.SO_BACKLOG, 1024)
            .option(ChannelOption.SO_REUSEADDR, true)
            
            // Worker配置
            .childOption(ChannelOption.SO_KEEPALIVE, true)
            .childOption(ChannelOption.TCP_NODELAY, true)
            
            // 连接超时
            .childOption(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
            
            // 写缓冲区水位（防止OOM）
            .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK,
                new WriteBufferWaterMark(32 * 1024, 64 * 1024))
            
            // ByteBuf分配器
            .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
    }
}
```

### 6.3 动态修改配置

```java
@Override
public void channelActive(ChannelHandlerContext ctx) throws Exception {
    Channel channel = ctx.channel();
    ChannelConfig config = channel.config();
    
    // 修改配置
    config.setAutoRead(false);  // 关闭自动读取
    config.setWriteBufferWaterMark(new WriteBufferWaterMark(16384, 65536));
    
    // 手动触发读取
    channel.read();
}
```

## 7. Channel的高级用法

### 7.1 AttributeKey（存储数据）

```java
// 定义AttributeKey
public class ChannelAttributes {
    public static final AttributeKey<String> USER_ID = 
        AttributeKey.valueOf("userId");
    public static final AttributeKey<Integer> LOGIN_TIME = 
        AttributeKey.valueOf("loginTime");
}

// 存储数据
@Override
public void channelActive(ChannelHandlerContext ctx) {
    Channel channel = ctx.channel();
    
    // 设置属性
    channel.attr(ChannelAttributes.USER_ID).set("user_123");
    channel.attr(ChannelAttributes.LOGIN_TIME).set((int) (System.currentTimeMillis() / 1000));
    
    System.out.println("用户登录: " + channel.attr(ChannelAttributes.USER_ID).get());
}

// 读取数据
@Override
public void channelRead(ChannelHandlerContext ctx, Object msg) {
    Channel channel = ctx.channel();
    
    // 获取属性
    String userId = channel.attr(ChannelAttributes.USER_ID).get();
    Integer loginTime = channel.attr(ChannelAttributes.LOGIN_TIME).get();
    
    System.out.println("处理用户 " + userId + " 的消息");
    System.out.println("登录时间: " + loginTime);
}
```

### 7.2 Channel管理

```java
/**
 * Channel管理器
 * 管理所有活跃的Channel
 */
public class ChannelManager {
    
    // 使用ChannelGroup管理所有Channel
    private static final ChannelGroup channels = 
        new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    
    // 用户ID到Channel的映射
    private static final ConcurrentHashMap<String, Channel> userChannels = 
        new ConcurrentHashMap<>();
    
    /**
     * 添加Channel
     */
    public static void add(String userId, Channel channel) {
        channels.add(channel);
        userChannels.put(userId, channel);
        System.out.println("用户上线: " + userId + ", 当前在线: " + channels.size());
    }
    
    /**
     * 移除Channel
     */
    public static void remove(String userId, Channel channel) {
        channels.remove(channel);
        userChannels.remove(userId);
        System.out.println("用户下线: " + userId + ", 当前在线: " + channels.size());
    }
    
    /**
     * 广播消息
     */
    public static void broadcast(String message) {
        channels.writeAndFlush(message + "\n");
        System.out.println("广播消息: " + message + ", 接收者: " + channels.size());
    }
    
    /**
     * 发送给指定用户
     */
    public static void sendToUser(String userId, String message) {
        Channel channel = userChannels.get(userId);
        if (channel != null && channel.isActive()) {
            channel.writeAndFlush(message + "\n");
        } else {
            System.err.println("用户不在线: " + userId);
        }
    }
    
    /**
     * 关闭所有连接
     */
    public static void closeAll() {
        channels.close();
    }
    
    /**
     * 获取在线用户数
     */
    public static int getOnlineCount() {
        return channels.size();
    }
}

// 使用示例
public class ChatServerHandler extends SimpleChannelInboundHandler<String> {
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        String userId = "user_" + channel.id().asShortText();
        channel.attr(ChannelAttributes.USER_ID).set(userId);
        
        ChannelManager.add(userId, channel);
        ChannelManager.broadcast(userId + " 加入聊天室");
    }
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        String userId = ctx.channel().attr(ChannelAttributes.USER_ID).get();
        String message = userId + ": " + msg;
        ChannelManager.broadcast(message);
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        String userId = ctx.channel().attr(ChannelAttributes.USER_ID).get();
        ChannelManager.remove(userId, ctx.channel());
        ChannelManager.broadcast(userId + " 离开聊天室");
    }
}
```

## 8. 实战：简单的聊天室

完整代码示例：

```java
/**
 * 聊天室服务器
 */
public class ChatServer {
    
    public static void main(String[] args) throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class)
             .childHandler(new ChannelInitializer<SocketChannel>() {
                 @Override
                 protected void initChannel(SocketChannel ch) {
                     ch.pipeline()
                       .addLast(new StringDecoder(CharsetUtil.UTF_8))
                       .addLast(new StringEncoder(CharsetUtil.UTF_8))
                       .addLast(new LineBasedFrameDecoder(1024))
                       .addLast(new ChatServerHandler());
                 }
             });
            
            ChannelFuture f = b.bind(8080).sync();
            System.out.println("聊天室服务器启动: 8080");
            
            f.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
```

## 9. 小结

本章我们深入学习了Channel：

✅ Channel的概念和类型  
✅ Channel的生命周期  
✅ 数据读写的多种方式  
✅ ChannelFuture的使用  
✅ Channel配置和管理  
✅ 实战聊天室示例  

**核心要点：**
1. Channel是Netty的核心抽象，代表网络连接
2. 所有IO操作都是异步的，返回ChannelFuture
3. 使用ChannelGroup可以方便地管理多个Channel
4. AttributeKey可以在Channel上存储自定义数据

## 10. 练习题

1. **基础练习**：实现一个Echo服务器，打印Channel的完整生命周期
2. **进阶练习**：实现一个聊天室，支持群发和私聊
3. **挑战练习**：实现Channel连接统计（总连接数、活跃连接数、断开连接数）

## 11. 下一步

下一章我们将学习EventLoop和EventLoopGroup，理解Netty的线程模型！🚀
