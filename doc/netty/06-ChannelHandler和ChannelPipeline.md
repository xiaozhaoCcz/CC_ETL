# 06 - ChannelHandler和ChannelPipeline

ChannelHandler是Netty业务逻辑的核心，ChannelPipeline是Handler的容器。理解它们的工作机制是掌握Netty的关键。

## 1. ChannelHandler概述

### 1.1 什么是ChannelHandler？

**ChannelHandler**是处理IO事件或拦截IO操作的接口，可以：
- 处理入站数据（channelRead）
- 处理出站数据（write）
- 处理异常（exceptionCaught）
- 自定义业务逻辑

### 1.2 Handler的类型

```
ChannelHandler (interface)
    │
    ├── ChannelInboundHandler          ← 入站处理器
    │       │
    │       ├── ChannelInboundHandlerAdapter
    │       └── SimpleChannelInboundHandler<T>
    │
    └── ChannelOutboundHandler         ← 出站处理器
            │
            └── ChannelOutboundHandlerAdapter
```

**入站 vs 出站：**

```
客户端                          服务器
  │                              │
  │─────── 数据 ──────────────>  │  入站(Inbound)
  │                              │  read, channelActive等
  │                              │
  │  <────── 数据 ──────────────│  出站(Outbound)
  │                              │  write, close等
```

## 2. ChannelInboundHandler详解

### 2.1 入站事件

```java
public interface ChannelInboundHandler extends ChannelHandler {
    // 1. Handler被添加到Pipeline
    void handlerAdded(ChannelHandlerContext ctx);
    
    // 2. Handler从Pipeline移除
    void handlerRemoved(ChannelHandlerContext ctx);
    
    // 3. Channel注册到EventLoop
    void channelRegistered(ChannelHandlerContext ctx);
    
    // 4. Channel从EventLoop注销
    void channelUnregistered(ChannelHandlerContext ctx);
    
    // 5. Channel激活（连接建立）
    void channelActive(ChannelHandlerContext ctx);
    
    // 6. Channel不活跃（连接断开）
    void channelInactive(ChannelHandlerContext ctx);
    
    // 7. 读取到数据
    void channelRead(ChannelHandlerContext ctx, Object msg);
    
    // 8. 读取完成
    void channelReadComplete(ChannelHandlerContext ctx);
    
    // 9. 用户自定义事件
    void userEventTriggered(ChannelHandlerContext ctx, Object evt);
    
    // 10. Channel可写状态改变
    void channelWritabilityChanged(ChannelHandlerContext ctx);
    
    // 11. 异常捕获
    void exceptionCaught(ChannelHandlerContext ctx, Throwable cause);
}
```

### 2.2 入站Handler示例

```java
/**
 * 完整的入站Handler示例
 */
public class CompleteInboundHandler extends ChannelInboundHandlerAdapter {
    
    private static final Logger logger = LoggerFactory.getLogger(CompleteInboundHandler.class);
    
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        logger.info("Handler被添加: {}", ctx.channel().id().asShortText());
    }
    
    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        logger.info("Channel注册: {}", ctx.channel().id());
        super.channelRegistered(ctx);
    }
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        logger.info("连接建立: {}", ctx.channel().remoteAddress());
        
        // 连接建立时，可以发送欢迎消息
        ctx.writeAndFlush("欢迎连接!\n");
        
        super.channelActive(ctx);
    }
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // 处理接收到的数据
        if (msg instanceof ByteBuf) {
            ByteBuf buf = (ByteBuf) msg;
            String message = buf.toString(CharsetUtil.UTF_8);
            logger.info("收到消息: {}", message);
            
            // 处理业务逻辑
            String response = processMessage(message);
            
            // 传递给下一个Handler
            ctx.fireChannelRead(response);
        }
    }
    
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        logger.info("读取完成");
        
        // 刷新缓冲区
        ctx.flush();
        
        super.channelReadComplete(ctx);
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.info("连接断开: {}", ctx.channel().remoteAddress());
        super.channelInactive(ctx);
    }
    
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        // 处理用户自定义事件（如心跳超时）
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.READER_IDLE) {
                logger.warn("读空闲，关闭连接");
                ctx.close();
            }
        }
        super.userEventTriggered(ctx, evt);
    }
    
    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        // 当Channel的可写状态改变时调用
        if (ctx.channel().isWritable()) {
            logger.info("Channel可写");
        } else {
            logger.warn("Channel不可写，写缓冲区已满");
        }
        super.channelWritabilityChanged(ctx);
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("发生异常", cause);
        ctx.close();
    }
    
    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        logger.info("Handler被移除");
    }
    
    private String processMessage(String message) {
        // 业务逻辑处理
        return "处理结果: " + message;
    }
}
```

### 2.3 SimpleChannelInboundHandler

**推荐使用！** 自动处理ByteBuf释放。

```java
/**
 * 使用泛型的Handler
 * 自动类型转换和内存释放
 */
public class StringHandler extends SimpleChannelInboundHandler<String> {
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        // msg已经是String类型，不需要手动转换
        // 方法结束后自动释放，不需要手动release
        
        System.out.println("收到消息: " + msg);
        
        // 处理业务逻辑
        String response = "Echo: " + msg;
        ctx.writeAndFlush(response + "\n");
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
```

**对比：**

```java
// ChannelInboundHandlerAdapter（需要手动管理）
public class ManualHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf buf = (ByteBuf) msg;
        try {
            String str = buf.toString(CharsetUtil.UTF_8);
            System.out.println(str);
        } finally {
            buf.release();  // 必须手动释放！
        }
    }
}

// SimpleChannelInboundHandler（自动管理）
public class AutoHandler extends SimpleChannelInboundHandler<String> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) {
        System.out.println(msg);
        // 自动释放，不需要手动管理
    }
}
```

## 3. ChannelOutboundHandler详解

### 3.1 出站事件

```java
public interface ChannelOutboundHandler extends ChannelHandler {
    // 1. 绑定地址
    void bind(ChannelHandlerContext ctx, SocketAddress localAddress, ChannelPromise promise);
    
    // 2. 连接远程地址
    void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress,
                SocketAddress localAddress, ChannelPromise promise);
    
    // 3. 断开连接
    void disconnect(ChannelHandlerContext ctx, ChannelPromise promise);
    
    // 4. 关闭Channel
    void close(ChannelHandlerContext ctx, ChannelPromise promise);
    
    // 5. 注销Channel
    void deregister(ChannelHandlerContext ctx, ChannelPromise promise);
    
    // 6. 读取数据
    void read(ChannelHandlerContext ctx);
    
    // 7. 写入数据（最常用）
    void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise);
    
    // 8. 刷新缓冲区
    void flush(ChannelHandlerContext ctx);
}
```

### 3.2 出站Handler示例

```java
/**
 * 出站Handler示例：添加消息头
 */
public class MessageEncoderHandler extends ChannelOutboundHandlerAdapter {
    
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) 
            throws Exception {
        
        if (msg instanceof String) {
            String message = (String) msg;
            
            // 添加消息头
            String encoded = "[HEADER]" + message + "[TAIL]";
            System.out.println("编码: " + message + " -> " + encoded);
            
            // 转换为ByteBuf
            ByteBuf buf = Unpooled.copiedBuffer(encoded, CharsetUtil.UTF_8);
            
            // 传递给下一个Handler
            ctx.write(buf, promise);
        } else {
            ctx.write(msg, promise);
        }
    }
}

/**
 * 出站Handler示例：日志记录
 */
public class LoggingOutboundHandler extends ChannelOutboundHandlerAdapter {
    
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) 
            throws Exception {
        
        System.out.println("发送数据: " + msg);
        System.out.println("时间: " + System.currentTimeMillis());
        System.out.println("Channel: " + ctx.channel().id().asShortText());
        
        // 传递给下一个Handler
        ctx.write(msg, promise);
    }
    
    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        System.out.println("关闭连接: " + ctx.channel().remoteAddress());
        ctx.close(promise);
    }
}
```

## 4. ChannelPipeline详解

### 4.1 什么是ChannelPipeline？

**ChannelPipeline**是Handler的容器，管理Handler链。

```
Channel -> ChannelPipeline -> Handler Chain

                   Pipeline
┌────────────────────────────────────────────────────┐
│                                                    │
│  Head → Handler1 → Handler2 → Handler3 → Tail     │
│   ↑                                          ↓     │
│   │                                          │     │
│  出站 ←──────────────────────────────────── 入站   │
│                                                    │
└────────────────────────────────────────────────────┘
```

### 4.2 Pipeline的工作流程

```
入站事件流（从Head到Tail）：
Socket → Head → InboundHandler1 → InboundHandler2 → InboundHandler3 → Tail

出站事件流（从Tail到Head）：
Tail → OutboundHandler3 → OutboundHandler2 → OutboundHandler1 → Head → Socket
```

**完整示例：**

```java
public class PipelineExample {
    
    public static void main(String[] args) throws Exception {
        EventLoopGroup group = new NioEventLoopGroup();
        
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(group)
             .channel(NioServerSocketChannel.class)
             .childHandler(new ChannelInitializer<SocketChannel>() {
                 @Override
                 protected void initChannel(SocketChannel ch) {
                     ChannelPipeline pipeline = ch.pipeline();
                     
                     // 入站Handler（按添加顺序执行）
                     pipeline.addLast("inbound1", new InboundHandler1());
                     pipeline.addLast("inbound2", new InboundHandler2());
                     pipeline.addLast("inbound3", new InboundHandler3());
                     
                     // 出站Handler（按相反顺序执行）
                     pipeline.addLast("outbound1", new OutboundHandler1());
                     pipeline.addLast("outbound2", new OutboundHandler2());
                     pipeline.addLast("outbound3", new OutboundHandler3());
                 }
             });
            
            ChannelFuture f = b.bind(8080).sync();
            System.out.println("服务器启动");
            f.channel().closeFuture().sync();
        } finally {
            group.shutdownGracefully();
        }
    }
    
    // 入站Handler
    static class InboundHandler1 extends ChannelInboundHandlerAdapter {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            System.out.println("InboundHandler1: 接收数据");
            ctx.fireChannelRead(msg);  // 传递给下一个Handler
        }
    }
    
    static class InboundHandler2 extends ChannelInboundHandlerAdapter {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            System.out.println("InboundHandler2: 处理数据");
            ctx.fireChannelRead(msg);
        }
    }
    
    static class InboundHandler3 extends ChannelInboundHandlerAdapter {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            System.out.println("InboundHandler3: 发送响应");
            ctx.writeAndFlush("响应数据");  // 触发出站事件
        }
    }
    
    // 出站Handler
    static class OutboundHandler1 extends ChannelOutboundHandlerAdapter {
        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
            System.out.println("OutboundHandler1: 编码数据");
            ctx.write(msg, promise);
        }
    }
    
    static class OutboundHandler2 extends ChannelOutboundHandlerAdapter {
        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
            System.out.println("OutboundHandler2: 加密数据");
            ctx.write(msg, promise);
        }
    }
    
    static class OutboundHandler3 extends ChannelOutboundHandlerAdapter {
        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
            System.out.println("OutboundHandler3: 发送到网络");
            ctx.write(msg, promise);
        }
    }
}
```

**输出：**

```
// 接收数据时（入站）
InboundHandler1: 接收数据
InboundHandler2: 处理数据
InboundHandler3: 发送响应

// 发送数据时（出站）
OutboundHandler3: 发送到网络
OutboundHandler2: 加密数据
OutboundHandler1: 编码数据
```

### 4.3 Pipeline的操作

```java
public class PipelineOperations {
    
    public void demonstratePipeline(Channel channel) {
        ChannelPipeline pipeline = channel.pipeline();
        
        // 1. 添加Handler
        pipeline.addFirst("first", new MyHandler());    // 添加到开头
        pipeline.addLast("last", new MyHandler());      // 添加到末尾
        pipeline.addBefore("last", "before", new MyHandler());  // 添加到指定Handler之前
        pipeline.addAfter("first", "after", new MyHandler());   // 添加到指定Handler之后
        
        // 2. 移除Handler
        pipeline.remove("first");                       // 按名称移除
        pipeline.remove(MyHandler.class);               // 按类型移除
        pipeline.removeFirst();                         // 移除第一个
        pipeline.removeLast();                          // 移除最后一个
        
        // 3. 替换Handler
        pipeline.replace("old", "new", new MyHandler());
        
        // 4. 获取Handler
        ChannelHandler handler = pipeline.get("first");
        MyHandler myHandler = pipeline.get(MyHandler.class);
        ChannelHandler first = pipeline.first();
        ChannelHandler last = pipeline.last();
        
        // 5. 获取ChannelHandlerContext
        ChannelHandlerContext ctx = pipeline.context("first");
        ChannelHandlerContext ctx2 = pipeline.context(MyHandler.class);
        
        // 6. 遍历Pipeline
        for (Map.Entry<String, ChannelHandler> entry : pipeline) {
            System.out.println("Handler: " + entry.getKey() + " -> " + entry.getValue());
        }
        
        // 7. 触发事件
        pipeline.fireChannelActive();                   // 触发channelActive事件
        pipeline.fireChannelRead("data");               // 触发channelRead事件
        pipeline.fireChannelReadComplete();             // 触发channelReadComplete事件
        pipeline.fireExceptionCaught(new Exception());  // 触发异常事件
    }
}
```

## 5. ChannelHandlerContext详解

### 5.1 什么是ChannelHandlerContext？

**ChannelHandlerContext**是Handler与Pipeline之间的桥梁。

```
┌─────────────────────────────────────┐
│         ChannelPipeline             │
├─────────────────────────────────────┤
│                                     │
│  ┌──────────────────────┐          │
│  │ ChannelHandlerContext│          │
│  ├──────────────────────┤          │
│  │   - prev             │          │
│  │   - next             │          │
│  │   - handler          │          │
│  │   - pipeline         │          │
│  │   - channel          │          │
│  └──────────────────────┘          │
│            ↓                        │
│  ┌──────────────────────┐          │
│  │   ChannelHandler     │          │
│  └──────────────────────┘          │
│                                     │
└─────────────────────────────────────┘
```

### 5.2 Context vs Channel

```java
public class ContextVsChannel {
    
    static class MyHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            // 方式1：通过ctx触发事件（从当前Handler开始）
            ctx.fireChannelRead(msg);
            
            // 方式2：通过pipeline触发事件（从Head开始）
            ctx.pipeline().fireChannelRead(msg);
            
            // 方式3：写数据（通过ctx，从当前Handler向前传递）
            ctx.write(msg);
            
            // 方式4：写数据（通过channel，从Tail向前传递）
            ctx.channel().write(msg);
        }
    }
}
```

**区别：**

```
Pipeline: [H1] → [H2] → [H3] → [H4] → [Tail]
                   ↑
                 当前Handler

ctx.write():           [H2] → [H1] → [Head]
channel.write():  [H4] → [H3] → [H2] → [H1] → [Head]
```

### 5.3 Context的常用方法

```java
public class ContextMethods extends ChannelInboundHandlerAdapter {
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        // 1. 获取关联对象
        Channel channel = ctx.channel();
        ChannelPipeline pipeline = ctx.pipeline();
        ChannelHandler handler = ctx.handler();
        EventExecutor executor = ctx.executor();
        
        // 2. 触发入站事件
        ctx.fireChannelActive();
        ctx.fireChannelRead(msg);
        ctx.fireChannelReadComplete();
        ctx.fireChannelInactive();
        ctx.fireExceptionCaught(new Exception());
        ctx.fireUserEventTriggered("event");
        
        // 3. 触发出站事件
        ctx.write(msg);
        ctx.writeAndFlush(msg);
        ctx.flush();
        ctx.read();
        ctx.close();
        ctx.disconnect();
        
        // 4. 获取属性
        AttributeKey<String> key = AttributeKey.valueOf("userId");
        ctx.channel().attr(key).set("user123");
        String userId = ctx.channel().attr(key).get();
        
        // 5. 判断状态
        boolean removed = ctx.isRemoved();
        
        // 6. 获取名称
        String name = ctx.name();
    }
}
```

## 6. 实战：构建完整的Handler链

```java
/**
 * 完整的服务器Handler链示例
 */
public class CompleteServer {
    
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
                     ChannelPipeline pipeline = ch.pipeline();
                     
                     // 1. 日志Handler（最先）
                     pipeline.addLast(new LoggingHandler(LogLevel.INFO));
                     
                     // 2. 空闲检测Handler
                     pipeline.addLast(new IdleStateHandler(60, 30, 0));
                     
                     // 3. 编解码Handler
                     pipeline.addLast(new StringDecoder(CharsetUtil.UTF_8));
                     pipeline.addLast(new StringEncoder(CharsetUtil.UTF_8));
                     pipeline.addLast(new LineBasedFrameDecoder(1024));
                     
                     // 4. 业务Handler
                     pipeline.addLast(new AuthenticationHandler());  // 认证
                     pipeline.addLast(new BusinessHandler());        // 业务逻辑
                     pipeline.addLast(new ExceptionHandler());       // 异常处理
                 }
             });
            
            ChannelFuture f = b.bind(8080).sync();
            System.out.println("服务器启动成功");
            f.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
    
    /**
     * 认证Handler
     */
    static class AuthenticationHandler extends ChannelInboundHandlerAdapter {
        
        private static final AttributeKey<Boolean> AUTH = 
            AttributeKey.valueOf("authenticated");
        
        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            // 初始化为未认证
            ctx.channel().attr(AUTH).set(false);
            ctx.writeAndFlush("请输入密码: ");
        }
        
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            Boolean authenticated = ctx.channel().attr(AUTH).get();
            
            if (!authenticated) {
                // 未认证，检查密码
                String password = ((ByteBuf) msg).toString(CharsetUtil.UTF_8);
                if ("123456".equals(password.trim())) {
                    ctx.channel().attr(AUTH).set(true);
                    ctx.writeAndFlush("认证成功!\n");
                } else {
                    ctx.writeAndFlush("密码错误，请重试: ");
                }
            } else {
                // 已认证，传递给下一个Handler
                ctx.fireChannelRead(msg);
            }
        }
    }
    
    /**
     * 业务Handler
     */
    static class BusinessHandler extends SimpleChannelInboundHandler<String> {
        
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, String msg) {
            System.out.println("处理业务: " + msg);
            
            // 业务逻辑
            String response = processBusinessLogic(msg);
            ctx.writeAndFlush(response + "\n");
        }
        
        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
            if (evt instanceof IdleStateEvent) {
                IdleStateEvent event = (IdleStateEvent) evt;
                if (event.state() == IdleState.READER_IDLE) {
                    System.out.println("读空闲，发送心跳");
                    ctx.writeAndFlush("PING\n");
                } else if (event.state() == IdleState.WRITER_IDLE) {
                    System.out.println("写空闲，关闭连接");
                    ctx.close();
                }
            }
        }
        
        private String processBusinessLogic(String msg) {
            return "处理结果: " + msg.toUpperCase();
        }
    }
    
    /**
     * 异常处理Handler（最后）
     */
    static class ExceptionHandler extends ChannelInboundHandlerAdapter {
        
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            System.err.println("捕获异常: " + cause.getMessage());
            cause.printStackTrace();
            
            // 发送错误消息
            ctx.writeAndFlush("错误: " + cause.getMessage() + "\n");
            
            // 关闭连接
            ctx.close();
        }
    }
}
```

## 7. 最佳实践

### 7.1 Handler的顺序

```java
// ✅ 推荐顺序
pipeline.addLast("logging", new LoggingHandler());          // 1. 日志
pipeline.addLast("idleState", new IdleStateHandler(...));   // 2. 空闲检测
pipeline.addLast("decoder", new StringDecoder());           // 3. 解码
pipeline.addLast("encoder", new StringEncoder());           // 4. 编码
pipeline.addLast("frameDecoder", new LineBasedFrameDecoder());  // 5. 拆包
pipeline.addLast("auth", new AuthHandler());                // 6. 认证
pipeline.addLast("business", new BusinessHandler());        // 7. 业务
pipeline.addLast("exception", new ExceptionHandler());      // 8. 异常
```

### 7.2 共享Handler

```java
// 标注为Sharable的Handler可以被多个Channel共享
@ChannelHandler.Sharable
public class SharedHandler extends ChannelInboundHandlerAdapter {
    // 注意：不能有状态字段！
    // private int count;  // ❌ 错误！多个Channel会并发访问
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        // 只能使用ctx或msg中的数据
        System.out.println("收到消息");
    }
}

// 使用共享Handler
SharedHandler sharedHandler = new SharedHandler();
pipeline1.addLast(sharedHandler);
pipeline2.addLast(sharedHandler);  // 同一个实例
```

### 7.3 Handler的生命周期管理

```java
public class LifecycleHandler extends ChannelInboundHandlerAdapter {
    
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        // 初始化资源
        System.out.println("初始化Handler");
    }
    
    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        // 清理资源
        System.out.println("清理Handler");
    }
}
```

## 8. 小结

本章我们深入学习了ChannelHandler和ChannelPipeline：

✅ ChannelHandler的类型和生命周期  
✅ 入站Handler和出站Handler的区别  
✅ ChannelPipeline的工作机制  
✅ ChannelHandlerContext的作用  
✅ 构建完整的Handler链  
✅ 最佳实践和注意事项  

**核心要点：**
1. Handler是业务逻辑的载体
2. Pipeline是Handler的容器
3. 入站事件从Head到Tail，出站事件从Tail到Head
4. 使用SimpleChannelInboundHandler自动管理内存
5. Handler要职责单一，通过链式组合实现复杂功能

## 9. 练习题

1. **基础练习**：实现一个日志Handler，记录所有事件
2. **进阶练习**：实现一个限流Handler，限制每秒请求数
3. **挑战练习**：实现一个完整的认证授权系统，包含登录、权限检查

## 10. 下一步

下一章我们将学习ByteBuf，Netty的数据容器！🚀
