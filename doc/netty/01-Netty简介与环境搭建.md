# 01 - Netty简介与环境搭建

## 1. 什么是Netty？

### 1.1 官方定义

Netty是一个**异步事件驱动**的网络应用框架，用于快速开发可维护的高性能协议服务器和客户端。

简单来说：**Netty是一个NIO（Non-blocking I/O）框架，让你更容易地开发网络程序。**

### 1.2 为什么需要Netty？

#### 传统Java网络编程的问题

让我们先看一个传统的Java Socket编程例子：

```java
// 传统的BIO服务器（阻塞IO）
public class TraditionalServer {
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(8080);
        System.out.println("服务器启动，等待连接...");
        
        while (true) {
            // 阻塞等待客户端连接
            Socket socket = serverSocket.accept();
            System.out.println("新客户端连接：" + socket.getRemoteSocketAddress());
            
            // 为每个连接创建一个新线程
            new Thread(() -> {
                try {
                    BufferedReader reader = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println("收到消息：" + line);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }
}
```

**这段代码的问题：**
1. **一个连接一个线程**：10000个连接就需要10000个线程，资源消耗巨大
2. **线程切换开销**：大量线程导致频繁的上下文切换
3. **阻塞等待**：线程大部分时间在等待IO，CPU利用率低
4. **代码复杂**：需要手动管理线程、缓冲区、异常等

#### Java NIO的问题

Java提供了NIO来解决这些问题，但是：

```java
// Java原生NIO代码（复杂且容易出错）
public class NIOServer {
    public static void main(String[] args) throws IOException {
        Selector selector = Selector.open();
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.bind(new InetSocketAddress(8080));
        serverChannel.configureBlocking(false);
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        
        while (true) {
            selector.select();
            Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
            
            while (keys.hasNext()) {
                SelectionKey key = keys.next();
                keys.remove();
                
                if (key.isAcceptable()) {
                    // 处理连接
                    ServerSocketChannel server = (ServerSocketChannel) key.channel();
                    SocketChannel client = server.accept();
                    client.configureBlocking(false);
                    client.register(selector, SelectionKey.OP_READ);
                } else if (key.isReadable()) {
                    // 处理读取
                    SocketChannel client = (SocketChannel) key.channel();
                    ByteBuffer buffer = ByteBuffer.allocate(1024);
                    client.read(buffer);
                    // ... 更多复杂的处理
                }
            }
        }
    }
}
```

**NIO的问题：**
1. **API复杂**：Selector、Channel、Buffer等概念难以理解
2. **容易出错**：需要处理各种边界情况
3. **没有开箱即用的协议支持**：HTTP、WebSocket等需要自己实现
4. **Bug多**：比如臭名昭著的epoll空轮询bug

### 1.3 Netty的优势

Netty解决了以上所有问题：

| 特性 | 传统BIO | Java NIO | Netty |
|------|---------|----------|-------|
| 开发难度 | 简单 | 困难 | 简单 |
| 性能 | 差（C10K问题） | 好 | 优秀 |
| 可靠性 | 一般 | 需要自己处理 | 高（久经考验） |
| 社区支持 | - | 官方 | 活跃社区 |
| 协议支持 | 需要自己实现 | 需要自己实现 | 内置多种协议 |

#### Netty的核心优势

1. **API简单易用**：统一的异步API
2. **功能强大**：内置多种协议支持（HTTP、WebSocket、SSL等）
3. **高性能**：
   - 零拷贝（Zero-Copy）
   - 内存池
   - 高效的Reactor线程模型
4. **高可靠性**：
   - 解决了NIO的各种bug
   - 久经考验（Apache Cassandra、Elasticsearch、Dubbo等都在使用）
5. **社区活跃**：持续维护和更新

### 1.4 谁在使用Netty？

- **阿里巴巴**：Dubbo RPC框架
- **Apache**：Cassandra、Spark、Flink
- **Elastic**：Elasticsearch
- **苹果**：Apple Push Notification Service
- **Twitter**：Finagle
- **Google**：gRPC（基于Netty）
- **Facebook**：Nifty

## 2. Netty架构概览

### 2.1 Netty的核心组件

在深入学习之前，我们先了解Netty的几个核心概念：

```
┌─────────────────────────────────────────────────────────┐
│                    Netty 架构图                          │
├─────────────────────────────────────────────────────────┤
│                                                          │
│   ┌──────────┐         ┌──────────┐                    │
│   │  Channel │◄────────┤ Pipeline │                     │
│   └────┬─────┘         └────┬─────┘                     │
│        │                    │                           │
│        │              ┌─────▼──────┐                    │
│        │              │  Handler1  │                    │
│        │              └─────┬──────┘                    │
│        │              ┌─────▼──────┐                    │
│        │              │  Handler2  │                    │
│        │              └─────┬──────┘                    │
│        │              ┌─────▼──────┐                    │
│        │              │  Handler3  │                    │
│        │              └────────────┘                    │
│        │                                                │
│   ┌────▼─────────┐                                     │
│   │  EventLoop   │                                     │
│   └──────────────┘                                     │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

**核心组件说明：**

1. **Channel（通道）**
   - 代表一个网络连接
   - 类似于传统的Socket
   
2. **EventLoop（事件循环）**
   - 处理IO事件的线程
   - 一个EventLoop可以处理多个Channel
   
3. **ChannelPipeline（管道）**
   - 拦截器链
   - 处理或拦截Channel的输入输出
   
4. **ChannelHandler（处理器）**
   - 具体的业务逻辑处理
   - 可以串联多个Handler

### 2.2 Netty的线程模型

Netty采用了Reactor线程模型：

```
                    ┌─────────────────┐
                    │   Boss Group    │
                    │  (接收连接)      │
                    └────────┬────────┘
                             │
                    接受新连接并注册到Worker
                             │
                    ┌────────▼────────┐
                    │  Worker Group   │
                    │  (处理IO事件)    │
                    │                 │
                    │  EventLoop-1    │
                    │  EventLoop-2    │
                    │  EventLoop-3    │
                    │      ...        │
                    └─────────────────┘
```

**工作流程：**
1. BossGroup负责接收客户端连接
2. WorkerGroup负责处理已建立连接的IO操作
3. 一个EventLoop可以处理多个连接，避免线程切换开销

## 3. 环境搭建

### 3.1 前置要求

- **JDK**：1.8或更高版本
- **构建工具**：Maven 3.x 或 Gradle 6.x
- **IDE**：IntelliJ IDEA（推荐）或Eclipse

### 3.2 Maven项目配置

#### 创建Maven项目

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>netty-learning</artifactId>
    <version>1.0-SNAPSHOT</version>

    <properties>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <maven.compiler.source>1.8</maven.compiler.source>
        <maven.compiler.target>1.8</maven.compiler.target>
        <netty.version>4.1.100.Final</netty.version>
    </properties>

    <dependencies>
        <!-- Netty核心依赖 -->
        <dependency>
            <groupId>io.netty</groupId>
            <artifactId>netty-all</artifactId>
            <version>${netty.version}</version>
        </dependency>

        <!-- 日志依赖 -->
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
            <version>1.7.36</version>
        </dependency>
        <dependency>
            <groupId>ch.qos.logback</groupId>
            <artifactId>logback-classic</artifactId>
            <version>1.2.11</version>
        </dependency>

        <!-- 单元测试 -->
        <dependency>
            <groupId>junit</groupId>
            <artifactId>junit</artifactId>
            <version>4.13.2</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.8.1</version>
                <configuration>
                    <source>1.8</source>
                    <target>1.8</target>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

#### 验证依赖

在项目根目录执行：

```bash
mvn clean compile
```

如果看到 `BUILD SUCCESS`，说明环境搭建成功！

### 3.3 Gradle项目配置

如果你使用Gradle，`build.gradle`配置如下：

```gradle
plugins {
    id 'java'
}

group 'com.example'
version '1.0-SNAPSHOT'

sourceCompatibility = 1.8
targetCompatibility = 1.8

repositories {
    mavenCentral()
}

dependencies {
    // Netty
    implementation 'io.netty:netty-all:4.1.100.Final'
    
    // 日志
    implementation 'org.slf4j:slf4j-api:1.7.36'
    implementation 'ch.qos.logback:logback-classic:1.2.11'
    
    // 测试
    testImplementation 'junit:junit:4.13.2'
}
```

### 3.4 Hello World验证

创建一个简单的测试类验证环境：

```java
package com.example.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class NettyVersionTest {
    public static void main(String[] args) {
        System.out.println("Netty环境搭建成功！");
        System.out.println("Netty版本验证测试");
        
        // 创建EventLoopGroup
        NioEventLoopGroup bossGroup = new NioEventLoopGroup(1);
        NioEventLoopGroup workerGroup = new NioEventLoopGroup();
        
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            System.out.println("新连接：" + ch.remoteAddress());
                        }
                    });
            
            System.out.println("Netty ServerBootstrap创建成功！");
            System.out.println("环境搭建完成，可以开始学习了！");
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
```

运行这个类，如果看到输出：

```
Netty环境搭建成功！
Netty版本验证测试
Netty ServerBootstrap创建成功！
环境搭建完成，可以开始学习了！
```

恭喜你！环境搭建完成！🎉

## 4. 项目结构建议

```
netty-learning/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── example/
│   │   │           └── netty/
│   │   │               ├── basic/          # 基础示例
│   │   │               ├── codec/          # 编解码器
│   │   │               ├── protocol/       # 自定义协议
│   │   │               ├── http/           # HTTP示例
│   │   │               ├── websocket/      # WebSocket示例
│   │   │               └── rpc/            # RPC示例
│   │   └── resources/
│   │       └── logback.xml                 # 日志配置
│   └── test/
│       └── java/
├── pom.xml                                 # Maven配置
└── README.md
```

## 5. 日志配置（可选）

创建 `src/main/resources/logback.xml`：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="STDOUT" />
    </root>

    <!-- Netty日志级别 -->
    <logger name="io.netty" level="DEBUG" />
</configuration>
```

## 6. 小结

在本章中，我们学习了：

✅ Netty是什么以及为什么需要它  
✅ 传统网络编程的痛点  
✅ Netty的核心优势和应用场景  
✅ Netty的基本架构  
✅ 如何搭建Netty开发环境  

## 7. 下一步

接下来，我们将学习：
- 网络编程的基础知识（IO模型、TCP/IP）
- 编写第一个完整的Netty服务器和客户端

## 8. 练习题

1. 尝试运行本章的验证代码
2. 思考：为什么Netty要设计BossGroup和WorkerGroup两个线程组？
3. 查看Netty的官方文档：https://netty.io/

准备好了吗？让我们继续下一章的学习！🚀
