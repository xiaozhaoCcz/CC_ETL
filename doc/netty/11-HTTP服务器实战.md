# 11 - HTTP服务器实战

本章我们将实现一个功能完整的HTTP服务器，支持GET/POST请求、文件上传下载、静态资源服务等。

## 1. HTTP协议基础

### 1.1 HTTP请求格式

```
GET /index.html HTTP/1.1
Host: localhost:8080
User-Agent: Mozilla/5.0
Accept: text/html
Connection: keep-alive

[请求体]
```

### 1.2 HTTP响应格式

```
HTTP/1.1 200 OK
Content-Type: text/html
Content-Length: 1024
Connection: keep-alive

<html>...</html>
```

## 2. 简单的HTTP服务器

### 2.1 基本实现

```java
package com.example.netty.http;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;

/**
 * 简单的HTTP服务器
 */
public class SimpleHttpServer {
    
    private final int port;
    
    public SimpleHttpServer(int port) {
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
                     ch.pipeline()
                       // HTTP编解码器
                       .addLast("codec", new HttpServerCodec())
                       // 聚合HTTP消息
                       .addLast("aggregator", new HttpObjectAggregator(65536))
                       // 业务Handler
                       .addLast("handler", new HttpServerHandler());
                 }
             });
            
            ChannelFuture f = b.bind(port).sync();
            System.out.println("HTTP服务器启动: http://localhost:" + port);
            
            f.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
    
    public static void main(String[] args) throws Exception {
        new SimpleHttpServer(8080).start();
    }
}

/**
 * HTTP处理器
 */
class HttpServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
        System.out.println("收到请求:");
        System.out.println("  URI: " + request.uri());
        System.out.println("  Method: " + request.method());
        System.out.println("  Headers: " + request.headers());
        
        // 构建响应
        String content = buildHtmlResponse(request);
        FullHttpResponse response = new DefaultFullHttpResponse(
            HttpVersion.HTTP_1_1,
            HttpResponseStatus.OK,
            Unpooled.copiedBuffer(content, CharsetUtil.UTF_8)
        );
        
        // 设置响应头
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        
        // 发送响应
        ctx.writeAndFlush(response);
    }
    
    private String buildHtmlResponse(FullHttpRequest request) {
        return "<!DOCTYPE html>" +
               "<html>" +
               "<head><title>Netty HTTP Server</title></head>" +
               "<body>" +
               "<h1>Hello from Netty!</h1>" +
               "<p>URI: " + request.uri() + "</p>" +
               "<p>Method: " + request.method() + "</p>" +
               "<p>Time: " + System.currentTimeMillis() + "</p>" +
               "</body>" +
               "</html>";
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
```

**测试：** 打开浏览器访问 http://localhost:8080

## 3. 路由处理

### 3.1 实现路由功能

```java
/**
 * 路由处理器
 */
public class RouterHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
        String uri = request.uri();
        HttpMethod method = request.method();
        
        System.out.println("请求: " + method + " " + uri);
        
        // 路由分发
        if ("/".equals(uri) || "/index".equals(uri)) {
            handleIndex(ctx, request);
        } else if ("/api/hello".equals(uri)) {
            handleApiHello(ctx, request);
        } else if (uri.startsWith("/api/user/")) {
            handleApiUser(ctx, request, uri);
        } else if ("/favicon.ico".equals(uri)) {
            handle404(ctx);
        } else {
            handle404(ctx);
        }
    }
    
    /**
     * 首页
     */
    private void handleIndex(ChannelHandlerContext ctx, FullHttpRequest request) {
        String html = "<!DOCTYPE html>" +
                     "<html>" +
                     "<head><title>首页</title></head>" +
                     "<body>" +
                     "<h1>欢迎使用Netty HTTP Server</h1>" +
                     "<ul>" +
                     "<li><a href='/api/hello'>Hello API</a></li>" +
                     "<li><a href='/api/user/123'>User API</a></li>" +
                     "</ul>" +
                     "</body>" +
                     "</html>";
        
        sendResponse(ctx, HttpResponseStatus.OK, html, "text/html");
    }
    
    /**
     * Hello API
     */
    private void handleApiHello(ChannelHandlerContext ctx, FullHttpRequest request) {
        String json = "{\"message\": \"Hello from Netty!\", \"timestamp\": " + 
                     System.currentTimeMillis() + "}";
        
        sendResponse(ctx, HttpResponseStatus.OK, json, "application/json");
    }
    
    /**
     * User API
     */
    private void handleApiUser(ChannelHandlerContext ctx, FullHttpRequest request, String uri) {
        // 提取用户ID
        String userId = uri.substring("/api/user/".length());
        
        String json = "{\"userId\": \"" + userId + "\", " +
                     "\"name\": \"User " + userId + "\", " +
                     "\"email\": \"user" + userId + "@example.com\"}";
        
        sendResponse(ctx, HttpResponseStatus.OK, json, "application/json");
    }
    
    /**
     * 404处理
     */
    private void handle404(ChannelHandlerContext ctx) {
        String html = "<!DOCTYPE html>" +
                     "<html>" +
                     "<head><title>404 Not Found</title></head>" +
                     "<body>" +
                     "<h1>404 - Page Not Found</h1>" +
                     "</body>" +
                     "</html>";
        
        sendResponse(ctx, HttpResponseStatus.NOT_FOUND, html, "text/html");
    }
    
    /**
     * 发送响应
     */
    private void sendResponse(ChannelHandlerContext ctx, HttpResponseStatus status, 
                             String content, String contentType) {
        FullHttpResponse response = new DefaultFullHttpResponse(
            HttpVersion.HTTP_1_1,
            status,
            Unpooled.copiedBuffer(content, CharsetUtil.UTF_8)
        );
        
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType + "; charset=UTF-8");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        
        ctx.writeAndFlush(response);
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
```

## 4. GET和POST请求处理

### 4.1 GET请求参数

```java
/**
 * GET请求处理
 */
public class GetRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
        if (request.method() != HttpMethod.GET) {
            sendError(ctx, HttpResponseStatus.METHOD_NOT_ALLOWED);
            return;
        }
        
        // 解析URI和参数
        QueryStringDecoder decoder = new QueryStringDecoder(request.uri());
        String path = decoder.path();
        Map<String, List<String>> params = decoder.parameters();
        
        System.out.println("Path: " + path);
        System.out.println("Params: " + params);
        
        // 获取参数
        String name = getParam(params, "name", "Guest");
        String age = getParam(params, "age", "0");
        
        // 构建响应
        String html = "<!DOCTYPE html>" +
                     "<html>" +
                     "<body>" +
                     "<h1>GET请求处理</h1>" +
                     "<p>Name: " + name + "</p>" +
                     "<p>Age: " + age + "</p>" +
                     "<form method='get'>" +
                     "  Name: <input name='name' value='" + name + "'><br>" +
                     "  Age: <input name='age' value='" + age + "'><br>" +
                     "  <button type='submit'>Submit</button>" +
                     "</form>" +
                     "</body>" +
                     "</html>";
        
        sendResponse(ctx, HttpResponseStatus.OK, html);
    }
    
    private String getParam(Map<String, List<String>> params, String name, String defaultValue) {
        List<String> values = params.get(name);
        return (values != null && !values.isEmpty()) ? values.get(0) : defaultValue;
    }
    
    private void sendResponse(ChannelHandlerContext ctx, HttpResponseStatus status, String content) {
        FullHttpResponse response = new DefaultFullHttpResponse(
            HttpVersion.HTTP_1_1,
            status,
            Unpooled.copiedBuffer(content, CharsetUtil.UTF_8)
        );
        
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }
    
    private void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
        sendResponse(ctx, status, "Error: " + status.toString());
    }
}
```

### 4.2 POST请求处理

```java
/**
 * POST请求处理
 */
public class PostRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
        if (request.method() != HttpMethod.POST) {
            sendError(ctx, HttpResponseStatus.METHOD_NOT_ALLOWED);
            return;
        }
        
        // 获取Content-Type
        String contentType = request.headers().get(HttpHeaderNames.CONTENT_TYPE);
        
        if (contentType != null && contentType.contains("application/json")) {
            handleJsonPost(ctx, request);
        } else if (contentType != null && contentType.contains("application/x-www-form-urlencoded")) {
            handleFormPost(ctx, request);
        } else {
            sendError(ctx, HttpResponseStatus.UNSUPPORTED_MEDIA_TYPE);
        }
    }
    
    /**
     * 处理JSON POST
     */
    private void handleJsonPost(ChannelHandlerContext ctx, FullHttpRequest request) {
        String json = request.content().toString(CharsetUtil.UTF_8);
        System.out.println("JSON: " + json);
        
        // 这里应该解析JSON（可以使用Jackson、Gson等）
        String response = "{\"status\": \"success\", \"message\": \"JSON received\"}";
        
        sendJsonResponse(ctx, HttpResponseStatus.OK, response);
    }
    
    /**
     * 处理Form POST
     */
    private void handleFormPost(ChannelHandlerContext ctx, FullHttpRequest request) {
        String body = request.content().toString(CharsetUtil.UTF_8);
        QueryStringDecoder decoder = new QueryStringDecoder(body, false);
        Map<String, List<String>> params = decoder.parameters();
        
        System.out.println("Form params: " + params);
        
        String name = getParam(params, "name", "");
        String email = getParam(params, "email", "");
        
        String html = "<!DOCTYPE html>" +
                     "<html>" +
                     "<body>" +
                     "<h1>POST请求处理</h1>" +
                     "<p>Name: " + name + "</p>" +
                     "<p>Email: " + email + "</p>" +
                     "<a href='/'>返回</a>" +
                     "</body>" +
                     "</html>";
        
        sendHtmlResponse(ctx, HttpResponseStatus.OK, html);
    }
    
    private String getParam(Map<String, List<String>> params, String name, String defaultValue) {
        List<String> values = params.get(name);
        return (values != null && !values.isEmpty()) ? values.get(0) : defaultValue;
    }
    
    private void sendHtmlResponse(ChannelHandlerContext ctx, HttpResponseStatus status, String content) {
        FullHttpResponse response = new DefaultFullHttpResponse(
            HttpVersion.HTTP_1_1,
            status,
            Unpooled.copiedBuffer(content, CharsetUtil.UTF_8)
        );
        
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }
    
    private void sendJsonResponse(ChannelHandlerContext ctx, HttpResponseStatus status, String content) {
        FullHttpResponse response = new DefaultFullHttpResponse(
            HttpVersion.HTTP_1_1,
            status,
            Unpooled.copiedBuffer(content, CharsetUtil.UTF_8)
        );
        
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }
    
    private void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
        sendHtmlResponse(ctx, status, "Error: " + status.toString());
    }
}
```

## 5. 静态文件服务器

```java
/**
 * 静态文件服务器
 */
public class StaticFileServer {
    
    private static final String WEB_ROOT = "src/main/resources/static";
    
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
                       .addLast(new HttpServerCodec())
                       .addLast(new HttpObjectAggregator(65536))
                       .addLast(new ChunkedWriteHandler())  // 支持大文件传输
                       .addLast(new StaticFileHandler());
                 }
             });
            
            ChannelFuture f = b.bind(8080).sync();
            System.out.println("静态文件服务器启动: http://localhost:8080");
            System.out.println("Web根目录: " + WEB_ROOT);
            
            f.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}

/**
 * 静态文件处理器
 */
class StaticFileHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    
    private static final String WEB_ROOT = "src/main/resources/static";
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) 
            throws Exception {
        
        if (request.method() != HttpMethod.GET) {
            sendError(ctx, HttpResponseStatus.METHOD_NOT_ALLOWED);
            return;
        }
        
        // 获取请求路径
        String uri = request.uri();
        String path = sanitizeUri(uri);
        
        if (path == null) {
            sendError(ctx, HttpResponseStatus.FORBIDDEN);
            return;
        }
        
        File file = new File(path);
        
        // 检查文件是否存在
        if (!file.exists() || file.isHidden()) {
            sendError(ctx, HttpResponseStatus.NOT_FOUND);
            return;
        }
        
        // 如果是目录，查找index.html
        if (file.isDirectory()) {
            file = new File(file, "index.html");
            if (!file.exists()) {
                sendDirectoryListing(ctx, file.getParentFile());
                return;
            }
        }
        
        // 发送文件
        sendFile(ctx, request, file);
    }
    
    /**
     * 清理URI，防止路径遍历攻击
     */
    private String sanitizeUri(String uri) {
        try {
            uri = URLDecoder.decode(uri, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return null;
        }
        
        if (uri.contains("..") || uri.contains("./") || uri.startsWith("/")) {
            // 防止路径遍历
        }
        
        return WEB_ROOT + File.separator + uri.replace('/', File.separatorChar);
    }
    
    /**
     * 发送文件
     */
    private void sendFile(ChannelHandlerContext ctx, FullHttpRequest request, File file) 
            throws IOException {
        
        RandomAccessFile raf = new RandomAccessFile(file, "r");
        long fileLength = raf.length();
        
        HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        HttpUtil.setContentLength(response, fileLength);
        
        // 设置Content-Type
        String contentType = getContentType(file);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType);
        
        // 支持Keep-Alive
        if (HttpUtil.isKeepAlive(request)) {
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        }
        
        // 发送响应头
        ctx.write(response);
        
        // 发送文件内容
        ctx.write(new DefaultFileRegion(raf.getChannel(), 0, fileLength));
        
        // 发送结束标记
        ChannelFuture future = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
        
        if (!HttpUtil.isKeepAlive(request)) {
            future.addListener(ChannelFutureListener.CLOSE);
        }
    }
    
    /**
     * 获取Content-Type
     */
    private String getContentType(File file) {
        String name = file.getName();
        if (name.endsWith(".html") || name.endsWith(".htm")) {
            return "text/html; charset=UTF-8";
        } else if (name.endsWith(".css")) {
            return "text/css; charset=UTF-8";
        } else if (name.endsWith(".js")) {
            return "application/javascript; charset=UTF-8";
        } else if (name.endsWith(".json")) {
            return "application/json; charset=UTF-8";
        } else if (name.endsWith(".jpg") || name.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (name.endsWith(".png")) {
            return "image/png";
        } else if (name.endsWith(".gif")) {
            return "image/gif";
        }
        return "application/octet-stream";
    }
    
    /**
     * 发送目录列表
     */
    private void sendDirectoryListing(ChannelHandlerContext ctx, File dir) {
        FullHttpResponse response = new DefaultFullHttpResponse(
            HttpVersion.HTTP_1_1,
            HttpResponseStatus.OK
        );
        
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8");
        
        StringBuilder buf = new StringBuilder();
        buf.append("<!DOCTYPE html>\r\n");
        buf.append("<html><head><title>目录列表</title></head><body>\r\n");
        buf.append("<h1>目录: ").append(dir.getPath()).append("</h1>\r\n");
        buf.append("<ul>\r\n");
        
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                String name = f.getName();
                if (f.isDirectory()) {
                    buf.append("<li><a href='").append(name).append("/'>")
                       .append(name).append("/</a></li>\r\n");
                } else {
                    buf.append("<li><a href='").append(name).append("'>")
                       .append(name).append("</a></li>\r\n");
                }
            }
        }
        
        buf.append("</ul></body></html>\r\n");
        
        ByteBuf buffer = Unpooled.copiedBuffer(buf, CharsetUtil.UTF_8);
        response.content().writeBytes(buffer);
        buffer.release();
        
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }
    
    /**
     * 发送错误响应
     */
    private void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
        FullHttpResponse response = new DefaultFullHttpResponse(
            HttpVersion.HTTP_1_1,
            status,
            Unpooled.copiedBuffer("Error: " + status.toString(), CharsetUtil.UTF_8)
        );
        
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
        
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        if (ctx.channel().isActive()) {
            sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
```

## 6. 完整的HTTP服务器

综合以上功能，创建一个完整的HTTP服务器：

```java
/**
 * 完整的HTTP服务器
 */
public class FullHttpServer {
    
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
                       // HTTP编解码
                       .addLast(new HttpServerCodec())
                       // 聚合HTTP消息
                       .addLast(new HttpObjectAggregator(65536))
                       // 压缩
                       .addLast(new HttpContentCompressor())
                       // 支持大文件传输
                       .addLast(new ChunkedWriteHandler())
                       // 路由处理
                       .addLast(new RouterHandler());
                 }
             });
            
            ChannelFuture f = b.bind(8080).sync();
            System.out.println("=================================");
            System.out.println("  HTTP服务器启动成功");
            System.out.println("  访问: http://localhost:8080");
            System.out.println("=================================");
            
            f.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
```

## 7. 小结

本章我们学习了：

✅ HTTP协议基础  
✅ 使用Netty实现HTTP服务器  
✅ 路由处理  
✅ GET和POST请求处理  
✅ 静态文件服务  

**核心要点：**
1. 使用HttpServerCodec处理HTTP协议
2. 使用HttpObjectAggregator聚合消息
3. 使用ChunkedWriteHandler支持大文件
4. 注意安全问题（路径遍历等）

## 8. 练习题

1. **基础练习**：实现一个支持GET和POST的REST API
2. **进阶练习**：添加文件上传功能
3. **挑战练习**：实现会话管理（Session）

## 9. 下一步

下一章我们将学习WebSocket，实现一个实时聊天室！🚀
