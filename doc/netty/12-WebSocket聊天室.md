# 12 - WebSocket聊天室实战

本章我们将使用Netty实现一个功能完整的WebSocket聊天室，支持实时消息推送、用户管理、群聊私聊等功能。

## 1. WebSocket协议基础

### 1.1 什么是WebSocket？

WebSocket是一种在单个TCP连接上进行全双工通信的协议，相比HTTP轮询，它有以下优势：

```
HTTP轮询：
客户端 ───请求1──→ 服务器
客户端 ←──响应1─── 服务器
客户端 ───请求2──→ 服务器  (不断轮询，浪费资源)
客户端 ←──响应2─── 服务器

WebSocket：
客户端 ═══连接═══ 服务器  (持久连接)
客户端 ←──消息1─── 服务器  (服务器主动推送)
客户端 ───消息2──→ 服务器  (双向通信)
```

**WebSocket优势：**
- ✅ 实时双向通信
- ✅ 节省带宽（无HTTP头开销）
- ✅ 服务器主动推送
- ✅ 保持连接状态

### 1.2 WebSocket握手

```
客户端请求：
GET /chat HTTP/1.1
Host: localhost:8080
Upgrade: websocket
Connection: Upgrade
Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==
Sec-WebSocket-Version: 13

服务器响应：
HTTP/1.1 101 Switching Protocols
Upgrade: websocket
Connection: Upgrade
Sec-WebSocket-Accept: s3pPLMBiTxaQ9kYGzzhZRbK+xOo=
```

## 2. 简单的WebSocket服务器

### 2.1 基本实现

```java
package com.example.netty.websocket;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.stream.ChunkedWriteHandler;

/**
 * WebSocket服务器
 */
public class WebSocketServer {
    
    private final int port;
    
    public WebSocketServer(int port) {
        this.port = port;
    }
    
    public void start() throws Exception {
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
                     
                     // HTTP编解码器
                     pipeline.addLast(new HttpServerCodec());
                     pipeline.addLast(new HttpObjectAggregator(65536));
                     pipeline.addLast(new ChunkedWriteHandler());
                     
                     // WebSocket处理器
                     pipeline.addLast(new WebSocketServerHandler());
                 }
             });
            
            ChannelFuture f = b.bind(port).sync();
            System.out.println("WebSocket服务器启动: ws://localhost:" + port + "/ws");
            
            f.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
    
    public static void main(String[] args) throws Exception {
        new WebSocketServer(8080).start();
    }
}

/**
 * WebSocket处理器
 */
class WebSocketServerHandler extends SimpleChannelInboundHandler<Object> {
    
    private WebSocketServerHandshaker handshaker;
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FullHttpRequest) {
            handleHttpRequest(ctx, (FullHttpRequest) msg);
        } else if (msg instanceof WebSocketFrame) {
            handleWebSocketFrame(ctx, (WebSocketFrame) msg);
        }
    }
    
    /**
     * 处理HTTP请求（WebSocket握手）
     */
    private void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest request) {
        // 检查是否是WebSocket升级请求
        if (!request.decoderResult().isSuccess() || 
            !"websocket".equals(request.headers().get("Upgrade"))) {
            sendHttpResponse(ctx, request, new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST));
            return;
        }
        
        // 进行WebSocket握手
        WebSocketServerHandshakerFactory factory = new WebSocketServerHandshakerFactory(
            "ws://localhost:8080/ws", null, true);
        handshaker = factory.newHandshaker(request);
        
        if (handshaker == null) {
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
        } else {
            handshaker.handshake(ctx.channel(), request);
            System.out.println("WebSocket握手成功: " + ctx.channel().remoteAddress());
        }
    }
    
    /**
     * 处理WebSocket帧
     */
    private void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {
        // Close帧
        if (frame instanceof CloseWebSocketFrame) {
            handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
            return;
        }
        
        // Ping帧
        if (frame instanceof PingWebSocketFrame) {
            ctx.write(new PongWebSocketFrame(frame.content().retain()));
            return;
        }
        
        // Pong帧
        if (frame instanceof PongWebSocketFrame) {
            return;
        }
        
        // 文本帧
        if (frame instanceof TextWebSocketFrame) {
            String text = ((TextWebSocketFrame) frame).text();
            System.out.println("收到消息: " + text);
            
            // 回复消息
            ctx.channel().writeAndFlush(
                new TextWebSocketFrame("服务器回复: " + text));
        }
    }
    
    /**
     * 发送HTTP响应
     */
    private void sendHttpResponse(ChannelHandlerContext ctx, 
                                  FullHttpRequest request, 
                                  FullHttpResponse response) {
        if (response.status().code() != 200) {
            response.content().writeBytes(response.status().toString().getBytes());
            HttpUtil.setContentLength(response, response.content().readableBytes());
        }
        
        ChannelFuture f = ctx.channel().writeAndFlush(response);
        if (!HttpUtil.isKeepAlive(request) || response.status().code() != 200) {
            f.addListener(ChannelFutureListener.CLOSE);
        }
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
```

### 2.2 HTML客户端

创建 `src/main/resources/static/websocket.html`：

```html
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>WebSocket测试</title>
    <style>
        #messages {
            height: 300px;
            overflow-y: scroll;
            border: 1px solid #ccc;
            padding: 10px;
            margin-bottom: 10px;
        }
        .message {
            margin: 5px 0;
            padding: 5px;
            border-radius: 3px;
        }
        .sent {
            background-color: #e3f2fd;
            text-align: right;
        }
        .received {
            background-color: #f5f5f5;
        }
    </style>
</head>
<body>
    <h1>WebSocket聊天测试</h1>
    
    <div id="status">状态: 未连接</div>
    <div id="messages"></div>
    
    <input type="text" id="messageInput" placeholder="输入消息" style="width: 300px;">
    <button onclick="sendMessage()">发送</button>
    <button onclick="connect()">连接</button>
    <button onclick="disconnect()">断开</button>
    
    <script>
        let ws = null;
        const messagesDiv = document.getElementById('messages');
        const statusDiv = document.getElementById('status');
        const messageInput = document.getElementById('messageInput');
        
        function connect() {
            if (ws != null) {
                console.log('已经连接');
                return;
            }
            
            ws = new WebSocket('ws://localhost:8080/ws');
            
            ws.onopen = function(event) {
                console.log('WebSocket连接成功');
                statusDiv.textContent = '状态: 已连接';
                statusDiv.style.color = 'green';
                addMessage('系统消息: 连接成功', 'received');
            };
            
            ws.onmessage = function(event) {
                console.log('收到消息: ' + event.data);
                addMessage('服务器: ' + event.data, 'received');
            };
            
            ws.onerror = function(event) {
                console.error('WebSocket错误');
                statusDiv.textContent = '状态: 错误';
                statusDiv.style.color = 'red';
            };
            
            ws.onclose = function(event) {
                console.log('WebSocket连接关闭');
                statusDiv.textContent = '状态: 已断开';
                statusDiv.style.color = 'gray';
                addMessage('系统消息: 连接关闭', 'received');
                ws = null;
            };
        }
        
        function disconnect() {
            if (ws != null) {
                ws.close();
            }
        }
        
        function sendMessage() {
            if (ws == null) {
                alert('请先连接');
                return;
            }
            
            const message = messageInput.value;
            if (message.trim() === '') {
                return;
            }
            
            ws.send(message);
            addMessage('我: ' + message, 'sent');
            messageInput.value = '';
        }
        
        function addMessage(text, className) {
            const div = document.createElement('div');
            div.className = 'message ' + className;
            div.textContent = text;
            messagesDiv.appendChild(div);
            messagesDiv.scrollTop = messagesDiv.scrollHeight;
        }
        
        // 回车发送
        messageInput.addEventListener('keypress', function(event) {
            if (event.key === 'Enter') {
                sendMessage();
            }
        });
        
        // 页面加载时自动连接
        window.onload = function() {
            connect();
        };
    </script>
</body>
</html>
```

## 3. 完整的聊天室实现

### 3.1 用户管理

```java
/**
 * 用户信息
 */
public class User {
    private String id;
    private String nickname;
    private Channel channel;
    private long loginTime;
    
    public User(String id, String nickname, Channel channel) {
        this.id = id;
        this.nickname = nickname;
        this.channel = channel;
        this.loginTime = System.currentTimeMillis();
    }
    
    // Getter和Setter
    public String getId() { return id; }
    public String getNickname() { return nickname; }
    public Channel getChannel() { return channel; }
    public long getLoginTime() { return loginTime; }
}

/**
 * 聊天室管理器
 */
public class ChatRoomManager {
    
    // 所有在线用户
    private static final Map<String, User> onlineUsers = new ConcurrentHashMap<>();
    
    // Channel到User的映射
    private static final Map<Channel, User> channelUsers = new ConcurrentHashMap<>();
    
    /**
     * 用户加入
     */
    public static void join(User user) {
        onlineUsers.put(user.getId(), user);
        channelUsers.put(user.getChannel(), user);
        
        System.out.println("用户加入: " + user.getNickname() + ", 在线人数: " + onlineUsers.size());
        
        // 广播用户加入消息
        broadcastMessage(buildSystemMessage(user.getNickname() + " 加入聊天室"));
        
        // 发送欢迎消息
        sendToUser(user.getChannel(), buildSystemMessage("欢迎 " + user.getNickname()));
        
        // 发送在线用户列表
        sendUserList(user.getChannel());
    }
    
    /**
     * 用户离开
     */
    public static void leave(Channel channel) {
        User user = channelUsers.remove(channel);
        if (user != null) {
            onlineUsers.remove(user.getId());
            System.out.println("用户离开: " + user.getNickname() + ", 在线人数: " + onlineUsers.size());
            
            // 广播用户离开消息
            broadcastMessage(buildSystemMessage(user.getNickname() + " 离开聊天室"));
        }
    }
    
    /**
     * 广播消息
     */
    public static void broadcastMessage(String message) {
        TextWebSocketFrame frame = new TextWebSocketFrame(message);
        for (User user : onlineUsers.values()) {
            user.getChannel().writeAndFlush(frame.retain());
        }
        frame.release();
    }
    
    /**
     * 发送给指定用户
     */
    public static void sendToUser(Channel channel, String message) {
        channel.writeAndFlush(new TextWebSocketFrame(message));
    }
    
    /**
     * 发送给指定用户（通过ID）
     */
    public static boolean sendToUser(String userId, String message) {
        User user = onlineUsers.get(userId);
        if (user != null) {
            sendToUser(user.getChannel(), message);
            return true;
        }
        return false;
    }
    
    /**
     * 获取用户
     */
    public static User getUser(Channel channel) {
        return channelUsers.get(channel);
    }
    
    /**
     * 获取在线用户数
     */
    public static int getOnlineCount() {
        return onlineUsers.size();
    }
    
    /**
     * 发送在线用户列表
     */
    private static void sendUserList(Channel channel) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"type\":\"userlist\",\"users\":[");
        boolean first = true;
        for (User user : onlineUsers.values()) {
            if (!first) sb.append(",");
            sb.append("{\"id\":\"").append(user.getId()).append("\",");
            sb.append("\"nickname\":\"").append(user.getNickname()).append("\"}");
            first = false;
        }
        sb.append("]}");
        sendToUser(channel, sb.toString());
    }
    
    /**
     * 构建系统消息
     */
    private static String buildSystemMessage(String content) {
        return "{\"type\":\"system\",\"content\":\"" + content + "\",\"time\":" + 
               System.currentTimeMillis() + "}";
    }
}
```

### 3.2 消息处理

```java
/**
 * 聊天室Handler
 */
public class ChatRoomHandler extends SimpleChannelInboundHandler<Object> {
    
    private WebSocketServerHandshaker handshaker;
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FullHttpRequest) {
            handleHttpRequest(ctx, (FullHttpRequest) msg);
        } else if (msg instanceof WebSocketFrame) {
            handleWebSocketFrame(ctx, (WebSocketFrame) msg);
        }
    }
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("新连接: " + ctx.channel().remoteAddress());
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("断开连接: " + ctx.channel().remoteAddress());
        ChatRoomManager.leave(ctx.channel());
    }
    
    /**
     * 处理HTTP请求（握手）
     */
    private void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest request) 
            throws Exception {
        
        // WebSocket升级
        if (!request.decoderResult().isSuccess() || 
            !"websocket".equals(request.headers().get("Upgrade"))) {
            sendHttpResponse(ctx, request, 
                new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST));
            return;
        }
        
        // 握手
        WebSocketServerHandshakerFactory factory = new WebSocketServerHandshakerFactory(
            getWebSocketLocation(request), null, true, 65536);
        handshaker = factory.newHandshaker(request);
        
        if (handshaker == null) {
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
        } else {
            handshaker.handshake(ctx.channel(), request);
            
            // 解析昵称（从URL参数）
            String uri = request.uri();
            String nickname = parseNickname(uri);
            if (nickname == null) {
                nickname = "游客" + System.currentTimeMillis() % 10000;
            }
            
            // 创建用户并加入聊天室
            String userId = ctx.channel().id().asShortText();
            User user = new User(userId, nickname, ctx.channel());
            ChatRoomManager.join(user);
        }
    }
    
    /**
     * 处理WebSocket帧
     */
    private void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {
        // Close帧
        if (frame instanceof CloseWebSocketFrame) {
            handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
            ChatRoomManager.leave(ctx.channel());
            return;
        }
        
        // Ping帧
        if (frame instanceof PingWebSocketFrame) {
            ctx.write(new PongWebSocketFrame(frame.content().retain()));
            return;
        }
        
        // 文本帧
        if (frame instanceof TextWebSocketFrame) {
            String message = ((TextWebSocketFrame) frame).text();
            handleMessage(ctx, message);
        }
    }
    
    /**
     * 处理消息
     */
    private void handleMessage(ChannelHandlerContext ctx, String message) {
        User user = ChatRoomManager.getUser(ctx.channel());
        if (user == null) {
            return;
        }
        
        try {
            // 解析JSON消息（简单起见，这里手动解析）
            if (message.startsWith("{")) {
                // TODO: 使用JSON库解析
                // 这里简化处理
            }
            
            // 构建消息JSON
            String msgJson = String.format(
                "{\"type\":\"chat\",\"from\":\"%s\",\"fromId\":\"%s\",\"content\":\"%s\",\"time\":%d}",
                user.getNickname(), user.getId(), escapeJson(message), System.currentTimeMillis()
            );
            
            // 广播消息
            ChatRoomManager.broadcastMessage(msgJson);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 转义JSON字符串
     */
    private String escapeJson(String str) {
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r");
    }
    
    /**
     * 解析昵称
     */
    private String parseNickname(String uri) {
        if (uri.contains("?nickname=")) {
            String[] parts = uri.split("\\?nickname=");
            if (parts.length == 2) {
                try {
                    return java.net.URLDecoder.decode(parts[1], "UTF-8");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }
    
    /**
     * 获取WebSocket地址
     */
    private String getWebSocketLocation(FullHttpRequest request) {
        String location = request.headers().get(HttpHeaderNames.HOST) + "/ws";
        return "ws://" + location;
    }
    
    /**
     * 发送HTTP响应
     */
    private void sendHttpResponse(ChannelHandlerContext ctx, 
                                  FullHttpRequest request, 
                                  FullHttpResponse response) {
        if (response.status().code() != 200) {
            response.content().writeBytes(response.status().toString().getBytes());
            HttpUtil.setContentLength(response, response.content().readableBytes());
        }
        
        ChannelFuture f = ctx.channel().writeAndFlush(response);
        if (!HttpUtil.isKeepAlive(request) || response.status().code() != 200) {
            f.addListener(ChannelFutureListener.CLOSE);
        }
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ChatRoomManager.leave(ctx.channel());
        ctx.close();
    }
}
```

### 3.3 完整的HTML客户端

```html
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Netty聊天室</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { font-family: Arial, sans-serif; background: #f0f0f0; }
        
        .container {
            max-width: 1200px;
            margin: 20px auto;
            background: white;
            border-radius: 8px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
            display: flex;
            height: 600px;
        }
        
        .sidebar {
            width: 250px;
            border-right: 1px solid #e0e0e0;
            padding: 20px;
        }
        
        .main {
            flex: 1;
            display: flex;
            flex-direction: column;
        }
        
        .header {
            padding: 20px;
            border-bottom: 1px solid #e0e0e0;
        }
        
        .messages {
            flex: 1;
            overflow-y: auto;
            padding: 20px;
        }
        
        .message {
            margin-bottom: 15px;
            display: flex;
        }
        
        .message.mine {
            justify-content: flex-end;
        }
        
        .message-content {
            max-width: 60%;
            padding: 10px 15px;
            border-radius: 8px;
        }
        
        .message.mine .message-content {
            background: #007bff;
            color: white;
        }
        
        .message.other .message-content {
            background: #f0f0f0;
        }
        
        .message.system .message-content {
            background: #fffbea;
            color: #856404;
            max-width: 100%;
            text-align: center;
        }
        
        .footer {
            padding: 20px;
            border-top: 1px solid #e0e0e0;
            display: flex;
            gap: 10px;
        }
        
        input[type="text"] {
            flex: 1;
            padding: 10px;
            border: 1px solid #ddd;
            border-radius: 4px;
        }
        
        button {
            padding: 10px 20px;
            background: #007bff;
            color: white;
            border: none;
            border-radius: 4px;
            cursor: pointer;
        }
        
        button:hover {
            background: #0056b3;
        }
        
        .user-list {
            list-style: none;
            margin-top: 10px;
        }
        
        .user-list li {
            padding: 8px;
            margin: 5px 0;
            background: #f5f5f5;
            border-radius: 4px;
        }
        
        .status {
            display: inline-block;
            width: 8px;
            height: 8px;
            border-radius: 50%;
            margin-right: 8px;
        }
        
        .status.online {
            background: #28a745;
        }
        
        .status.offline {
            background: #dc3545;
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="sidebar">
            <h3>在线用户 (<span id="onlineCount">0</span>)</h3>
            <ul class="user-list" id="userList"></ul>
        </div>
        
        <div class="main">
            <div class="header">
                <h2>Netty聊天室</h2>
                <div>
                    <span class="status" id="statusIndicator"></span>
                    <span id="statusText">未连接</span>
                </div>
            </div>
            
            <div class="messages" id="messages"></div>
            
            <div class="footer">
                <input type="text" id="messageInput" placeholder="输入消息..." />
                <button onclick="sendMessage()">发送</button>
            </div>
        </div>
    </div>
    
    <script>
        let ws = null;
        let myNickname = null;
        
        function connect() {
            // 获取昵称
            myNickname = prompt('请输入昵称:', '游客' + Math.floor(Math.random() * 10000));
            if (!myNickname) {
                myNickname = '游客' + Math.floor(Math.random() * 10000);
            }
            
            ws = new WebSocket('ws://localhost:8080/ws?nickname=' + encodeURIComponent(myNickname));
            
            ws.onopen = function() {
                updateStatus('online', '已连接');
                console.log('连接成功');
            };
            
            ws.onmessage = function(event) {
                const data = JSON.parse(event.data);
                handleMessage(data);
            };
            
            ws.onerror = function() {
                updateStatus('offline', '连接错误');
            };
            
            ws.onclose = function() {
                updateStatus('offline', '已断开');
                ws = null;
            };
        }
        
        function handleMessage(data) {
            if (data.type === 'system') {
                addSystemMessage(data.content);
            } else if (data.type === 'chat') {
                addChatMessage(data.from, data.content, data.from === myNickname);
            } else if (data.type === 'userlist') {
                updateUserList(data.users);
            }
        }
        
        function addSystemMessage(content) {
            const div = document.createElement('div');
            div.className = 'message system';
            div.innerHTML = '<div class="message-content">' + content + '</div>';
            document.getElementById('messages').appendChild(div);
            scrollToBottom();
        }
        
        function addChatMessage(from, content, isMine) {
            const div = document.createElement('div');
            div.className = 'message ' + (isMine ? 'mine' : 'other');
            div.innerHTML = '<div class="message-content">' +
                           (isMine ? '' : '<strong>' + from + '</strong><br>') +
                           content + '</div>';
            document.getElementById('messages').appendChild(div);
            scrollToBottom();
        }
        
        function updateUserList(users) {
            const list = document.getElementById('userList');
            list.innerHTML = '';
            users.forEach(user => {
                const li = document.createElement('li');
                li.textContent = user.nickname;
                list.appendChild(li);
            });
            document.getElementById('onlineCount').textContent = users.length;
        }
        
        function updateStatus(status, text) {
            const indicator = document.getElementById('statusIndicator');
            const statusText = document.getElementById('statusText');
            indicator.className = 'status ' + status;
            statusText.textContent = text;
        }
        
        function sendMessage() {
            const input = document.getElementById('messageInput');
            const message = input.value.trim();
            
            if (!message || !ws) return;
            
            ws.send(message);
            input.value = '';
        }
        
        function scrollToBottom() {
            const messages = document.getElementById('messages');
            messages.scrollTop = messages.scrollHeight;
        }
        
        document.getElementById('messageInput').addEventListener('keypress', function(e) {
            if (e.key === 'Enter') {
                sendMessage();
            }
        });
        
        window.onload = function() {
            connect();
        };
        
        window.onbeforeunload = function() {
            if (ws) {
                ws.close();
            }
        };
    </script>
</body>
</html>
```

## 4. 功能扩展

### 4.1 私聊功能

```java
// 在ChatRoomHandler中添加私聊处理
private void handlePrivateMessage(ChannelHandlerContext ctx, String targetId, String content) {
    User fromUser = ChatRoomManager.getUser(ctx.channel());
    if (fromUser == null) return;
    
    String msgJson = String.format(
        "{\"type\":\"private\",\"from\":\"%s\",\"content\":\"%s\"}",
        fromUser.getNickname(), escapeJson(content)
    );
    
    boolean sent = ChatRoomManager.sendToUser(targetId, msgJson);
    if (!sent) {
        ChatRoomManager.sendToUser(ctx.channel(), 
            "{\"type\":\"error\",\"content\":\"用户不在线\"}");
    }
}
```

### 4.2 心跳检测

```java
// 添加心跳Handler
pipeline.addLast(new IdleStateHandler(60, 30, 0, TimeUnit.SECONDS));
pipeline.addLast(new HeartbeatHandler());

class HeartbeatHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.READER_IDLE) {
                System.out.println("读空闲，关闭连接");
                ctx.close();
            } else if (event.state() == IdleState.WRITER_IDLE) {
                // 发送ping
                ctx.writeAndFlush(new PingWebSocketFrame());
            }
        }
    }
}
```

## 5. 小结

本章我们实现了一个完整的WebSocket聊天室：

✅ WebSocket协议基础  
✅ 握手处理  
✅ 用户管理  
✅ 群聊功能  
✅ 在线用户列表  
✅ 美观的Web界面  

**核心要点：**
1. 使用WebSocketServerHandshaker处理握手
2. 区分不同类型的WebSocketFrame
3. 使用ChatRoomManager管理用户
4. JSON格式传输消息
5. 添加心跳保持连接

## 6. 练习题

1. **基础练习**：添加消息时间戳显示
2. **进阶练习**：实现私聊功能
3. **挑战练习**：添加聊天室（多房间）功能

## 7. 下一步

下一章我们将学习如何实现RPC框架！🚀
