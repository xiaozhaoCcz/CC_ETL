# 03 - 第一个Netty程序

现在，让我们动手编写第一个完整的Netty程序！我们将实现一个经典的Echo服务器：客户端发送什么，服务器就回复什么。

## 1. Echo服务器概述

### 1.1 功能需求

- 服务器监听8080端口
- 接收客户端消息
- 将收到的消息原样返回给客户端
- 记录连接和消息日志

### 1.2 架构图

```
┌─────────────┐                    ┌─────────────┐
│   客户端     │  ─── "Hello" ───>  │  服务器      │
│             │                    │             │
│             │  <── "Hello" ────  │  (Echo回去)  │
└─────────────┘                    └─────────────┘
```

## 2. 服务器端实现

### 2.1 创建服务器启动类

```java
package com.example.netty.echo;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 * Echo服务器
 * 功能：将客户端发送的消息原样返回
 */
public class EchoServer {
    
    private final int port;
    
    public EchoServer(int port) {
        this.port = port;
    }
    
    public void start() throws Exception {
        // 1. 创建两个EventLoopGroup
        // bossGroup：处理连接请求
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        // workerGroup：处理IO操作
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        
        try {
            // 2. 创建服务器启动类
            ServerBootstrap bootstrap = new ServerBootstrap();
            
            // 3. 配置服务器
            bootstrap.group(bossGroup, workerGroup)
                    // 指定使用NIO的传输Channel
                    .channel(NioServerSocketChannel.class)
                    // 设置服务器连接队列大小
                    .option(ChannelOption.SO_BACKLOG, 128)
                    // 保持连接活动状态
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    // 配置Channel的处理器
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            // 向Pipeline添加处理器
                            ch.pipeline().addLast(new EchoServerHandler());
                        }
                    });
            
            System.out.println("Echo服务器启动成功，监听端口：" + port);
            
            // 4. 绑定端口，开始接收连接
            ChannelFuture future = bootstrap.bind(port).sync();
            
            // 5. 等待服务器Socket关闭
            future.channel().closeFuture().sync();
            
        } finally {
            // 6. 优雅关闭
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }
    
    public static void main(String[] args) throws Exception {
        int port = 8080;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        }
        new EchoServer(port).start();
    }
}
```

**代码详解：**

#### EventLoopGroup
```java
EventLoopGroup bossGroup = new NioEventLoopGroup(1);
EventLoopGroup workerGroup = new NioEventLoopGroup();
```
- `bossGroup`：主Reactor，只需要1个线程，负责接收客户端连接
- `workerGroup`：从Reactor，默认线程数=CPU核心数*2，负责处理IO

#### ServerBootstrap
服务器启动辅助类，用于配置服务器。

#### ChannelInitializer
Channel初始化器，在Channel注册后会调用`initChannel`方法。

### 2.2 创建服务器处理器

```java
package com.example.netty.echo;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;

/**
 * Echo服务器处理器
 * 处理客户端的消息
 */
public class EchoServerHandler extends ChannelInboundHandlerAdapter {
    
    /**
     * 当Channel注册到EventLoop时调用
     */
    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Channel注册: " + ctx.channel().id());
        super.channelRegistered(ctx);
    }
    
    /**
     * 当Channel激活时调用（连接建立）
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("客户端连接成功: " + ctx.channel().remoteAddress());
        super.channelActive(ctx);
    }
    
    /**
     * 当接收到客户端消息时调用
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // msg是ByteBuf类型
        ByteBuf in = (ByteBuf) msg;
        
        // 读取消息内容
        String message = in.toString(CharsetUtil.UTF_8);
        System.out.println("服务器收到消息: " + message);
        
        // 将消息原样返回（Echo）
        ctx.write(in);
    }
    
    /**
     * 当读取完成时调用
     */
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        // 将待发送的消息刷新到网络
        ctx.flush();
        System.out.println("消息发送完成");
    }
    
    /**
     * 当Channel不再活跃时调用（连接断开）
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("客户端断开连接: " + ctx.channel().remoteAddress());
        super.channelInactive(ctx);
    }
    
    /**
     * 当发生异常时调用
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        // 打印异常
        System.err.println("发生异常: " + cause.getMessage());
        cause.printStackTrace();
        
        // 关闭连接
        ctx.close();
    }
}
```

**Handler生命周期：**

```
handlerAdded
     ↓
channelRegistered
     ↓
channelActive          ← 连接建立
     ↓
channelRead           ← 接收数据
     ↓
channelReadComplete   ← 读取完成
     ↓
channelInactive       ← 连接断开
     ↓
channelUnregistered
     ↓
handlerRemoved
```

**关键方法说明：**

1. **channelActive**：连接建立时调用，可以在这里发送欢迎消息
2. **channelRead**：接收到数据时调用，处理业务逻辑
3. **channelReadComplete**：一次读取完成时调用，通常在这里flush
4. **exceptionCaught**：发生异常时调用，需要处理异常并关闭连接

## 3. 客户端实现

### 3.1 创建客户端启动类

```java
package com.example.netty.echo;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.util.Scanner;

/**
 * Echo客户端
 */
public class EchoClient {
    
    private final String host;
    private final int port;
    
    public EchoClient(String host, int port) {
        this.host = host;
        this.port = port;
    }
    
    public void start() throws Exception {
        // 1. 创建EventLoopGroup（客户端只需要一个）
        EventLoopGroup group = new NioEventLoopGroup();
        
        try {
            // 2. 创建客户端启动类
            Bootstrap bootstrap = new Bootstrap();
            
            // 3. 配置客户端
            bootstrap.group(group)
                    // 指定使用NIO的传输Channel
                    .channel(NioSocketChannel.class)
                    // 配置TCP参数
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    // 配置Channel的处理器
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new EchoClientHandler());
                        }
                    });
            
            System.out.println("正在连接服务器: " + host + ":" + port);
            
            // 4. 连接服务器
            ChannelFuture future = bootstrap.connect(host, port).sync();
            System.out.println("连接成功！输入消息发送给服务器（输入'quit'退出）：");
            
            // 5. 从控制台读取输入并发送
            Scanner scanner = new Scanner(System.in);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if ("quit".equalsIgnoreCase(line)) {
                    break;
                }
                // 发送消息
                future.channel().writeAndFlush(line + "\n");
            }
            
            // 6. 等待连接关闭
            future.channel().closeFuture().sync();
            
        } finally {
            // 7. 优雅关闭
            group.shutdownGracefully();
        }
    }
    
    public static void main(String[] args) throws Exception {
        String host = "localhost";
        int port = 8080;
        
        if (args.length == 2) {
            host = args[0];
            port = Integer.parseInt(args[1]);
        }
        
        new EchoClient(host, port).start();
    }
}
```

**客户端与服务器的区别：**

| 项目 | 服务器 | 客户端 |
|------|--------|--------|
| 启动类 | ServerBootstrap | Bootstrap |
| EventLoopGroup | 2个（boss+worker） | 1个 |
| Channel类型 | NioServerSocketChannel | NioSocketChannel |
| 操作 | bind(port) | connect(host, port) |

### 3.2 创建客户端处理器

```java
package com.example.netty.echo;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;

/**
 * Echo客户端处理器
 */
public class EchoClientHandler extends ChannelInboundHandlerAdapter {
    
    /**
     * 当连接建立时调用
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("已连接到服务器: " + ctx.channel().remoteAddress());
    }
    
    /**
     * 当接收到服务器消息时调用
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf in = (ByteBuf) msg;
        String message = in.toString(CharsetUtil.UTF_8);
        System.out.println("收到服务器回复: " + message);
        
        // 释放ByteBuf（重要！）
        in.release();
    }
    
    /**
     * 当发生异常时调用
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.err.println("发生异常: " + cause.getMessage());
        cause.printStackTrace();
        ctx.close();
    }
}
```

**注意：** 客户端需要手动释放ByteBuf（`in.release()`），而服务器端由于调用了`ctx.write(in)`，Netty会自动释放。

## 4. 运行程序

### 4.1 启动服务器

```bash
# 编译
mvn clean compile

# 运行服务器
mvn exec:java -Dexec.mainClass="com.example.netty.echo.EchoServer"
```

输出：
```
Echo服务器启动成功，监听端口：8080
```

### 4.2 启动客户端

打开另一个终端：

```bash
# 运行客户端
mvn exec:java -Dexec.mainClass="com.example.netty.echo.EchoClient"
```

输出：
```
正在连接服务器: localhost:8080
连接成功！输入消息发送给服务器（输入'quit'退出）：
```

### 4.3 测试交互

**客户端输入：**
```
Hello Netty!
```

**客户端输出：**
```
收到服务器回复: Hello Netty!
```

**服务器输出：**
```
客户端连接成功: /127.0.0.1:54321
服务器收到消息: Hello Netty!
消息发送完成
```

## 5. 完整流程图

```
客户端                                     服务器
  │                                         │
  │────1. connect()────────────────────────>│
  │                                         │
  │<───2. channelActive()──────────────────│ 连接建立
  │                                         │
  │────3. write("Hello")───────────────────>│
  │                                         │
  │                                         │ 4. channelRead()
  │                                         │    读取消息
  │                                         │
  │                                         │ 5. ctx.write()
  │                                         │    原样返回
  │                                         │
  │<───6. "Hello"──────────────────────────│ 6. channelReadComplete()
  │                                         │    flush发送
  │                                         │
  │ 7. channelRead()                        │
  │    收到回复                              │
  │                                         │
```

## 6. 代码改进

### 6.1 添加字符串编解码器

上面的例子中，我们手动处理ByteBuf。Netty提供了编解码器，可以自动转换。

**改进服务器Handler：**

```java
package com.example.netty.echo;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * 使用泛型的Handler
 * 自动处理String类型，不需要手动操作ByteBuf
 */
public class EchoServerHandlerV2 extends SimpleChannelInboundHandler<String> {
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("客户端连接: " + ctx.channel().remoteAddress());
    }
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        System.out.println("收到消息: " + msg);
        
        // 发送回复
        ctx.writeAndFlush("Echo: " + msg + "\n");
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
```

**修改服务器启动类，添加编解码器：**

```java
.childHandler(new ChannelInitializer<SocketChannel>() {
    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ch.pipeline()
            // 添加字符串解码器（将ByteBuf转为String）
            .addLast(new StringDecoder(CharsetUtil.UTF_8))
            // 添加字符串编码器（将String转为ByteBuf）
            .addLast(new StringEncoder(CharsetUtil.UTF_8))
            // 添加业务Handler
            .addLast(new EchoServerHandlerV2());
    }
});
```

**客户端同样修改：**

```java
.handler(new ChannelInitializer<SocketChannel>() {
    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ch.pipeline()
            .addLast(new StringDecoder(CharsetUtil.UTF_8))
            .addLast(new StringEncoder(CharsetUtil.UTF_8))
            .addLast(new EchoClientHandlerV2());
    }
});
```

**改进后的客户端Handler：**

```java
public class EchoClientHandlerV2 extends SimpleChannelInboundHandler<String> {
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        System.out.println("收到回复: " + msg);
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
```

**优势：**
- ✅ 不需要手动操作ByteBuf
- ✅ 代码更简洁
- ✅ 自动处理内存释放
- ✅ 使用泛型，类型安全

### 6.2 添加日志Handler

Netty提供了LoggingHandler，可以打印详细日志：

```java
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

// 在服务器端添加
.handler(new LoggingHandler(LogLevel.INFO))
.childHandler(new ChannelInitializer<SocketChannel>() {
    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ch.pipeline()
            .addLast(new LoggingHandler(LogLevel.DEBUG))  // 详细日志
            .addLast(new StringDecoder(CharsetUtil.UTF_8))
            .addLast(new StringEncoder(CharsetUtil.UTF_8))
            .addLast(new EchoServerHandlerV2());
    }
});
```

运行后可以看到详细的网络包信息：

```
[id: 0x12345678, L:/127.0.0.1:8080 - R:/127.0.0.1:54321] READ: 12B
         +-------------------------------------------------+
         |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f |
+--------+-------------------------------------------------+----------------+
|00000000| 48 65 6c 6c 6f 20 4e 65 74 74 79 21             |Hello Netty!    |
+--------+-------------------------------------------------+----------------+
```

## 7. 常见问题

### 7.1 内存泄漏

**问题：** 忘记释放ByteBuf

```java
// ❌ 错误：没有释放ByteBuf
@Override
public void channelRead(ChannelHandlerContext ctx, Object msg) {
    ByteBuf buf = (ByteBuf) msg;
    String str = buf.toString(CharsetUtil.UTF_8);
    System.out.println(str);
    // 忘记释放！
}
```

**解决方案：**

```java
// ✅ 方案1：手动释放
@Override
public void channelRead(ChannelHandlerContext ctx, Object msg) {
    ByteBuf buf = (ByteBuf) msg;
    try {
        String str = buf.toString(CharsetUtil.UTF_8);
        System.out.println(str);
    } finally {
        buf.release();  // 释放
    }
}

// ✅ 方案2：使用SimpleChannelInboundHandler（推荐）
public class MyHandler extends SimpleChannelInboundHandler<String> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) {
        // 自动释放，不需要手动管理
        System.out.println(msg);
    }
}
```

### 7.2 半包和粘包问题

**问题：** TCP是流式协议，可能出现半包和粘包

```
发送：["Hello", "World"]
接收：["HelloWo", "rld"]      ← 半包/粘包
```

**解决方案：** 使用Netty的编解码器（后续章节详细讲解）

```java
// 使用换行符分隔
.addLast(new LineBasedFrameDecoder(1024))
.addLast(new StringDecoder(CharsetUtil.UTF_8))

// 或使用长度字段
.addLast(new LengthFieldBasedFrameDecoder(1024, 0, 4))
```

## 8. 完整项目结构

```
src/main/java/com/example/netty/echo/
├── EchoServer.java              # 服务器启动类
├── EchoServerHandler.java       # 服务器Handler
├── EchoServerHandlerV2.java     # 改进版Handler
├── EchoClient.java              # 客户端启动类
├── EchoClientHandler.java       # 客户端Handler
└── EchoClientHandlerV2.java     # 改进版Handler
```

## 9. 小结

本章我们实现了第一个完整的Netty程序，学习了：

✅ 服务器端的基本结构（ServerBootstrap、EventLoopGroup）  
✅ 客户端的基本结构（Bootstrap、EventLoopGroup）  
✅ ChannelHandler的生命周期  
✅ ByteBuf的基本使用  
✅ 使用编解码器简化代码  
✅ 内存管理的重要性  

**核心要点：**
1. **服务器**：2个EventLoopGroup，使用ServerBootstrap
2. **客户端**：1个EventLoopGroup，使用Bootstrap
3. **Handler**：继承ChannelInboundHandlerAdapter或SimpleChannelInboundHandler
4. **内存**：使用SimpleChannelInboundHandler自动管理，或手动release

## 10. 练习题

1. **基础练习**：运行本章的Echo程序，测试多个客户端同时连接
2. **改进练习**：修改服务器，在回复时加上时间戳
3. **挑战练习**：实现一个简单的聊天室，多个客户端可以互相发消息

## 11. 下一步

接下来我们将深入学习Netty的核心组件：
- Channel的详细用法
- EventLoop的工作机制
- ChannelPipeline的设计原理

继续加油！🚀
