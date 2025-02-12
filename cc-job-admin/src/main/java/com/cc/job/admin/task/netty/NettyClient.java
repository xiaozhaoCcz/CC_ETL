package com.cc.job.admin.task.netty;

import com.xxl.job.core.util.GsonTool;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.timeout.IdleStateHandler;

import java.net.URI;
import java.net.URL;
import java.util.concurrent.TimeUnit;

public class NettyClient {
    private static NioEventLoopGroup nioEventLoopGroup;

    private Channel channel;

    private String address;
    private String host;


    public void init(String address) throws Exception {
        if (!address.toLowerCase().startsWith("http")) {
            address = "http://" + address;	// IP:PORT, need parse to url
        }

        this.address = address;
        URL url = new URL(address);
        this.host = url.getHost();
        int port = url.getPort()>-1?url.getPort():80;

        // group
        if (nioEventLoopGroup == null) {
            synchronized (NettyClient.class) {
                if (nioEventLoopGroup == null) {
                    nioEventLoopGroup = new NioEventLoopGroup();
//                    xxlRpcInvokerFactory.addStopCallBack(new BaseCallback() {
//                        @Override
//                        public void run() throws Exception {
//                            nioEventLoopGroup.shutdownGracefully();
//                        }
//                    });
                }
            }
        }

        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(nioEventLoopGroup)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel channel) throws Exception {
                        channel.pipeline()
                                .addLast(new IdleStateHandler(0,0, 30, TimeUnit.SECONDS))   // beat N, close if fail
                                .addLast(new HttpClientCodec())
                                .addLast(new HttpObjectAggregator(5*1024*1024))
                                .addLast(new NettyClientHandler());
                    }
                })
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000);
        this.channel = bootstrap.connect(host, port).sync().channel();

        // valid
//        if (!isValidate()) {
//            close();
//            return;
//        }
//
//        logger.debug(">>>>>>>>>>> xxl-rpc netty client proxy, connect to server success at host:{}, port:{}", host, port);
    }

    public void send(Object requestObject) throws Exception {
        String requestBody = GsonTool.toJson(requestObject);
        byte[] requestBytes = requestBody.getBytes("UTF-8");
        DefaultFullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, new URI(address).getRawPath(), Unpooled.wrappedBuffer(requestBytes));
        request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        request.headers().set(HttpHeaderNames.CONTENT_LENGTH, request.content().readableBytes());
        this.channel.writeAndFlush(request);
    }
}
