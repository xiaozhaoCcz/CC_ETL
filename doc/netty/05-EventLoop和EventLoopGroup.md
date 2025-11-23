# 05 - EventLoop和EventLoopGroup

EventLoop是Netty的心脏，理解它对于掌握Netty的线程模型至关重要。

## 1. 什么是EventLoop？

### 1.1 概念理解

**EventLoop（事件循环）** 可以理解为：
- 一个永不停止的循环
- 不断检查是否有事件发生
- 有事件就处理，没事件就等待

```java
// EventLoop的伪代码
while (!stopped) {
    // 1. 检查是否有IO事件
    List<Event> events = selector.select();
    
    // 2. 处理IO事件
    for (Event event : events) {
        if (event.isReadable()) {
            handleRead(event);
        } else if (event.isWritable()) {
            handleWrite(event);
        }
    }
    
    // 3. 处理定时任务
    runScheduledTasks();
    
    // 4. 处理普通任务
    runTasks();
}
```

### 1.2 EventLoop vs Thread

```
传统多线程模型：
┌────────┐ ┌────────┐ ┌────────┐
│ Thread1│ │ Thread2│ │ Thread3│
│  处理  │ │  处理  │ │  处理  │
│Connection1│ Connection2│ Connection3│
└────────┘ └────────┘ └────────┘
 一个连接    一个连接    一个连接
  一个线程    一个线程    一个线程

Netty EventLoop模型：
┌─────────────────────────────┐
│        EventLoop            │
│         (一个线程)            │
├─────────────────────────────┤
│  处理多个Connection:         │
│  - Connection1              │
│  - Connection2              │
│  - Connection3              │
│  - Connection4              │
│  - ...                      │
└─────────────────────────────┘
   一个EventLoop处理多个连接
```

**关系总结：**
- 1个EventLoop = 1个Thread
- 1个EventLoop可以处理多个Channel
- 1个Channel只属于1个EventLoop（线程安全！）

## 2. EventLoop的层次结构

```
EventLoopGroup (interface)
    │
    ├── MultithreadEventLoopGroup (abstract)
    │       │
    │       ├── NioEventLoopGroup        ← 常用
    │       ├── EpollEventLoopGroup      ← Linux优化
    │       └── KQueueEventLoopGroup     ← MacOS
    │
    └── DefaultEventLoopGroup

EventLoop (interface)
    │
    ├── SingleThreadEventLoop (abstract)
    │       │
    │       ├── NioEventLoop
    │       ├── EpollEventLoop
    │       └── KQueueEventLoop
    │
    └── DefaultEventLoop
```

## 3. EventLoopGroup详解

### 3.1 创建EventLoopGroup

```java
// 1. 默认线程数（推荐）
// 线程数 = CPU核心数 * 2
EventLoopGroup group1 = new NioEventLoopGroup();

// 2. 指定线程数
EventLoopGroup group2 = new NioEventLoopGroup(4);  // 4个线程

// 3. 自定义线程工厂
EventLoopGroup group3 = new NioEventLoopGroup(4, new ThreadFactory() {
    private AtomicInteger index = new AtomicInteger(0);
    
    @Override
    public Thread newThread(Runnable r) {
        return new Thread(r, "MyEventLoop-" + index.getAndIncrement());
    }
});

// 4. 使用Lambda简化
EventLoopGroup group4 = new NioEventLoopGroup(4, 
    r -> new Thread(r, "Worker-" + threadId++));
```

### 3.2 BossGroup vs WorkerGroup

```
                     BossGroup (1个线程)
                          │
                    接受新连接
                          │
         ┌────────────────┼────────────────┐
         │                │                │
    Channel1          Channel2         Channel3
         │                │                │
         └────────────────┼────────────────┘
                          │
                    WorkerGroup (N个线程)
                          │
              ┌───────────┼───────────┐
              │           │           │
         EventLoop1   EventLoop2  EventLoop3
              │           │           │
         处理IO事件   处理IO事件   处理IO事件
```

**完整示例：**

```java
public class EventLoopGroupExample {
    
    public static void main(String[] args) throws Exception {
        // Boss: 只负责接受连接，1个线程足够
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        
        // Worker: 负责处理IO，线程数 = CPU核心数 * 2
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        
        System.out.println("Boss线程数: 1");
        System.out.println("Worker线程数: " + 
            ((NioEventLoopGroup)workerGroup).executorCount());
        
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)  // 设置两个group
             .channel(NioServerSocketChannel.class)
             .childHandler(new ChannelInitializer<SocketChannel>() {
                 @Override
                 protected void initChannel(SocketChannel ch) {
                     System.out.println("新连接分配到: " + 
                         Thread.currentThread().getName());
                     ch.pipeline().addLast(new MyHandler());
                 }
             });
            
            ChannelFuture f = b.bind(8080).sync();
            System.out.println("服务器启动成功");
            
            f.channel().closeFuture().sync();
        } finally {
            // 优雅关闭
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
```

### 3.3 单个EventLoopGroup（不推荐）

```java
// 也可以只使用一个EventLoopGroup
EventLoopGroup group = new NioEventLoopGroup();

ServerBootstrap b = new ServerBootstrap();
b.group(group)  // boss和worker使用同一个group
 .channel(NioServerSocketChannel.class)
 .childHandler(...);
```

**缺点：**
- Boss和Worker混在一起，职责不清
- 高并发时Boss可能成为瓶颈

## 4. EventLoop的工作原理

### 4.1 EventLoop的任务类型

EventLoop处理三种任务：

```
EventLoop
    │
    ├── 1. IO任务（优先级最高）
    │      ├── Accept连接
    │      ├── Read数据
    │      └── Write数据
    │
    ├── 2. 普通任务
    │      └── 通过execute()提交的任务
    │
    └── 3. 定时任务
           └── 通过schedule()提交的任务
```

### 4.2 任务执行示例

```java
public class EventLoopTaskExample extends ChannelInboundHandlerAdapter {
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        EventLoop eventLoop = channel.eventLoop();
        
        System.out.println("当前线程: " + Thread.currentThread().getName());
        System.out.println("EventLoop线程: " + eventLoop.toString());
        
        // 1. 提交普通任务
        eventLoop.execute(() -> {
            System.out.println("执行普通任务: " + Thread.currentThread().getName());
            try {
                Thread.sleep(1000);
                System.out.println("普通任务完成");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        
        // 2. 提交定时任务（延迟3秒执行）
        eventLoop.schedule(() -> {
            System.out.println("执行定时任务: " + Thread.currentThread().getName());
            ctx.writeAndFlush("定时消息\n");
        }, 3, TimeUnit.SECONDS);
        
        // 3. 提交周期任务（每5秒执行一次）
        eventLoop.scheduleAtFixedRate(() -> {
            System.out.println("执行周期任务: " + Thread.currentThread().getName());
            if (channel.isActive()) {
                ctx.writeAndFlush("心跳\n");
            }
        }, 0, 5, TimeUnit.SECONDS);
        
        // 4. 检查是否在EventLoop线程中
        if (eventLoop.inEventLoop()) {
            System.out.println("当前在EventLoop线程中");
            // 直接执行
            doSomething();
        } else {
            System.out.println("不在EventLoop线程中");
            // 提交到EventLoop执行
            eventLoop.execute(() -> doSomething());
        }
    }
    
    private void doSomething() {
        System.out.println("执行业务逻辑");
    }
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // 这个方法一定在EventLoop线程中执行
        EventLoop eventLoop = ctx.channel().eventLoop();
        assert eventLoop.inEventLoop();  // true
        
        System.out.println("处理数据: " + Thread.currentThread().getName());
        super.channelRead(ctx, msg);
    }
}
```

### 4.3 线程安全

**重要特性：** 一个Channel的所有IO操作都在同一个EventLoop中执行，天然线程安全！

```java
public class ThreadSafetyExample extends ChannelInboundHandlerAdapter {
    
    private int messageCount = 0;  // 不需要volatile或synchronized！
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // 这个方法总是在同一个EventLoop线程中调用
        // 所以messageCount的操作是线程安全的
        messageCount++;
        System.out.println("收到第 " + messageCount + " 条消息");
        
        // 如果要在多个Channel间共享数据，才需要加锁
        // 但不推荐这样做，应该使用无锁设计
    }
}
```

## 5. EventLoop的高级用法

### 5.1 提交耗时任务

```java
public class LongRunningTaskHandler extends ChannelInboundHandlerAdapter {
    
    // 创建业务线程池
    private static final ExecutorService businessExecutor = 
        Executors.newFixedThreadPool(10);
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // 方式1：直接在EventLoop中执行（❌ 不推荐）
        // 会阻塞EventLoop，影响其他Channel
        // doLongRunningTask();  // 耗时操作
        
        // 方式2：提交到业务线程池（✅ 推荐）
        businessExecutor.submit(() -> {
            try {
                // 执行耗时操作（数据库查询、复杂计算等）
                String result = doLongRunningTask();
                
                // 结果写回需要在EventLoop中执行
                ctx.channel().eventLoop().execute(() -> {
                    ctx.writeAndFlush(result);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
    
    private String doLongRunningTask() throws InterruptedException {
        // 模拟耗时操作
        Thread.sleep(5000);
        return "任务完成";
    }
}
```

**原则：**
- ✅ IO操作在EventLoop中执行
- ❌ 耗时操作不要在EventLoop中执行
- ✅ 耗时操作提交到业务线程池

### 5.2 多个EventLoopGroup协作

```java
public class MultiGroupExample {
    
    public static void main(String[] args) throws Exception {
        // 1. Boss Group：接受连接
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        
        // 2. Worker Group：处理IO
        EventLoopGroup workerGroup = new NioEventLoopGroup(4);
        
        // 3. Business Group：处理业务逻辑
        EventLoopGroup businessGroup = new DefaultEventLoopGroup(8);
        
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class)
             .childHandler(new ChannelInitializer<SocketChannel>() {
                 @Override
                 protected void initChannel(SocketChannel ch) {
                     ch.pipeline()
                       // IO Handler在workerGroup中执行
                       .addLast(new StringDecoder(CharsetUtil.UTF_8))
                       .addLast(new StringEncoder(CharsetUtil.UTF_8))
                       
                       // 业务Handler在businessGroup中执行
                       .addLast(businessGroup, new BusinessHandler());
                 }
             });
            
            ChannelFuture f = b.bind(8080).sync();
            System.out.println("服务器启动");
            System.out.println("Boss线程: 1");
            System.out.println("Worker线程: 4");
            System.out.println("Business线程: 8");
            
            f.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            businessGroup.shutdownGracefully();
        }
    }
    
    static class BusinessHandler extends SimpleChannelInboundHandler<String> {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, String msg) {
            // 这个方法在businessGroup的线程中执行
            System.out.println("业务线程: " + Thread.currentThread().getName());
            
            // 模拟耗时业务
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            
            ctx.writeAndFlush("处理完成\n");
        }
    }
}
```

**线程模型：**

```
请求 → BossGroup (1线程)
         │
         └→ 分配Channel
              │
              └→ WorkerGroup (4线程)
                    │
                    ├→ IO操作(读写)
                    │
                    └→ BusinessGroup (8线程)
                          │
                          └→ 业务逻辑(耗时操作)
```

### 5.3 EventLoop的关闭

```java
// 1. 立即关闭（不推荐）
eventLoopGroup.shutdownNow();

// 2. 优雅关闭（推荐）
Future<?> future = eventLoopGroup.shutdownGracefully();
future.sync();  // 等待关闭完成

// 3. 设置超时时间
eventLoopGroup.shutdownGracefully(2, 15, TimeUnit.SECONDS);
// 参数：
// - quietPeriod: 2秒（静默期，如果有新任务则重新开始）
// - timeout: 15秒（最大等待时间）
// - unit: 时间单位

// 4. 添加关闭监听
eventLoopGroup.shutdownGracefully().addListener(f -> {
    if (f.isSuccess()) {
        System.out.println("EventLoopGroup关闭成功");
    } else {
        System.err.println("关闭失败: " + f.cause());
    }
});
```

## 6. 实战：自定义EventLoop任务调度

```java
/**
 * 心跳检测示例
 */
public class HeartbeatHandler extends ChannelInboundHandlerAdapter {
    
    private static final int HEARTBEAT_INTERVAL = 30;  // 30秒
    private ScheduledFuture<?> heartbeatFuture;
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("连接建立，启动心跳: " + ctx.channel().remoteAddress());
        
        // 启动心跳任务
        startHeartbeat(ctx);
        
        super.channelActive(ctx);
    }
    
    private void startHeartbeat(ChannelHandlerContext ctx) {
        heartbeatFuture = ctx.channel().eventLoop().scheduleAtFixedRate(() -> {
            if (ctx.channel().isActive()) {
                System.out.println("发送心跳: " + System.currentTimeMillis());
                ctx.writeAndFlush("PING\n").addListener(future -> {
                    if (!future.isSuccess()) {
                        System.err.println("心跳发送失败");
                        ctx.close();
                    }
                });
            }
        }, HEARTBEAT_INTERVAL, HEARTBEAT_INTERVAL, TimeUnit.SECONDS);
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("连接断开，停止心跳");
        
        // 取消心跳任务
        if (heartbeatFuture != null) {
            heartbeatFuture.cancel(false);
            heartbeatFuture = null;
        }
        
        super.channelInactive(ctx);
    }
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        String message = ((ByteBuf) msg).toString(CharsetUtil.UTF_8);
        
        if ("PONG".equals(message.trim())) {
            System.out.println("收到心跳响应");
        } else {
            super.channelRead(ctx, msg);
        }
    }
}
```

## 7. 性能优化

### 7.1 选择合适的线程数

```java
public class OptimalThreadCount {
    
    /**
     * 计算最优线程数
     */
    public static int calculateThreadCount() {
        int cpuCount = Runtime.getRuntime().availableProcessors();
        
        // IO密集型（如网络通信）：CPU核心数 * 2
        int ioIntensive = cpuCount * 2;
        
        // CPU密集型（如加密计算）：CPU核心数 + 1
        int cpuIntensive = cpuCount + 1;
        
        System.out.println("CPU核心数: " + cpuCount);
        System.out.println("IO密集型建议线程数: " + ioIntensive);
        System.out.println("CPU密集型建议线程数: " + cpuIntensive);
        
        return ioIntensive;  // Netty通常是IO密集型
    }
    
    public static void main(String[] args) {
        int threadCount = calculateThreadCount();
        
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup(threadCount);
        
        // ... 使用
    }
}
```

### 7.2 监控EventLoop状态

```java
public class EventLoopMonitor {
    
    public static void monitor(EventLoopGroup group) {
        if (group instanceof NioEventLoopGroup) {
            NioEventLoopGroup nioGroup = (NioEventLoopGroup) group;
            
            // 获取所有EventLoop
            for (EventExecutor executor : nioGroup) {
                if (executor instanceof SingleThreadEventExecutor) {
                    SingleThreadEventExecutor eventLoop = 
                        (SingleThreadEventExecutor) executor;
                    
                    System.out.println("EventLoop: " + eventLoop);
                    System.out.println("  待处理任务数: " + eventLoop.pendingTasks());
                    System.out.println("  是否关闭: " + eventLoop.isShutdown());
                    System.out.println("  是否终止: " + eventLoop.isTerminated());
                }
            }
        }
    }
    
    /**
     * 定期监控
     */
    public static void startMonitoring(EventLoopGroup group) {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            System.out.println("\n=== EventLoop监控 ===");
            monitor(group);
        }, 0, 10, TimeUnit.SECONDS);
    }
}
```

## 8. 常见问题和最佳实践

### 8.1 不要阻塞EventLoop

```java
// ❌ 错误示例：阻塞EventLoop
public class BadHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        // 数据库查询（阻塞操作）
        String result = database.query("SELECT ...");  // 可能耗时几百毫秒
        
        // 文件IO（阻塞操作）
        FileUtils.write(file, data);  // 可能耗时几秒
        
        // 会阻塞EventLoop，影响其他Channel！
        ctx.writeAndFlush(result);
    }
}

// ✅ 正确示例：使用线程池
public class GoodHandler extends ChannelInboundHandlerAdapter {
    private static final ExecutorService executor = 
        Executors.newFixedThreadPool(10);
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        // 提交到线程池处理
        executor.submit(() -> {
            String result = database.query("SELECT ...");
            FileUtils.write(file, data);
            
            // 写回时切换到EventLoop
            ctx.channel().eventLoop().execute(() -> {
                ctx.writeAndFlush(result);
            });
        });
    }
}
```

### 8.2 合理使用多个EventLoopGroup

```java
// ✅ 推荐配置
EventLoopGroup bossGroup = new NioEventLoopGroup(1);          // 1个Boss
EventLoopGroup workerGroup = new NioEventLoopGroup();         // N个Worker
EventLoopGroup businessGroup = new DefaultEventLoopGroup();   // M个业务线程

// ❌ 过度使用
EventLoopGroup group1 = new NioEventLoopGroup(100);  // 太多线程，切换开销大
```

### 8.3 EventLoop复用

```java
// 客户端可以共享EventLoopGroup
EventLoopGroup clientGroup = new NioEventLoopGroup();

// 创建多个客户端连接
Bootstrap b1 = new Bootstrap().group(clientGroup);
Bootstrap b2 = new Bootstrap().group(clientGroup);
Bootstrap b3 = new Bootstrap().group(clientGroup);

// 所有连接共享同一个EventLoopGroup，节省资源
```

## 9. 小结

本章我们深入学习了EventLoop和EventLoopGroup：

✅ EventLoop的概念和工作原理  
✅ EventLoopGroup的创建和配置  
✅ BossGroup和WorkerGroup的职责分工  
✅ 三种任务类型（IO、普通、定时）  
✅ 线程模型和线程安全  
✅ 任务调度和性能优化  

**核心要点：**
1. 1个EventLoop = 1个Thread
2. 1个EventLoop处理多个Channel
3. 1个Channel只属于1个EventLoop
4. 不要在EventLoop中执行阻塞操作
5. 合理设置线程数：IO密集型用 CPU核心数*2

## 10. 对比表

| 概念 | EventLoop | EventLoopGroup | Thread | ThreadPool |
|------|-----------|----------------|--------|------------|
| 数量 | 1个 | N个EventLoop | 1个 | N个Thread |
| 任务 | IO+定时+普通 | 管理EventLoop | 执行任务 | 管理Thread |
| 生命周期 | 受Group管理 | 手动管理 | 自动 | 手动管理 |

## 11. 练习题

1. **基础练习**：创建一个服务器，打印每个连接属于哪个EventLoop
2. **进阶练习**：实现心跳机制，每30秒发送一次心跳
3. **挑战练习**：实现EventLoop监控，统计每个EventLoop处理的连接数和任务数

## 12. 下一步

下一章我们将学习ChannelHandler和ChannelPipeline，理解Netty的事件处理机制！🚀
