package com.cc.job.admin.task.netty;

import com.cc.job.admin.task.handler.JobGroupXxlJob;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.util.GsonTool;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.CharsetUtil;

public class NettyClientHandler extends SimpleChannelInboundHandler<FullHttpResponse> {



    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, FullHttpResponse msg) throws Exception {
        // valid status
        if (!HttpResponseStatus.OK.equals(msg.status())) {
            throw new RuntimeException("xxl-rpc response status invalid.");
        }

        // response parse
        String json = msg.content().toString(CharsetUtil.UTF_8);

//        ReturnT returnT = GsonTool.fromJson(json, ReturnT.class);
//
//        JobGroupXxlJob.addJobMap(String.valueOf(returnT.getContent()),returnT.getCode()==ReturnT.SUCCESS_CODE);
    }
}
