package com.airing;

import com.airing.entity.BaseMsg;
import com.airing.msg.service.MsgContext;
import com.airing.utils.CommonUtils;
import com.alibaba.fastjson.JSONObject;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebSocketServerHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {
    private static final Logger log = LoggerFactory.getLogger(WebSocketServerHandler.class);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception {
        String data = msg.text();
        log.debug("server data: {}", data);
        CommonUtils.channelRead(data, ctx);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt == WebSocketServerProtocolHandler.ServerHandshakeStateEvent.HANDSHAKE_COMPLETE) {
            log.info("{} connected", ctx.channel().toString());
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        String key = NettySocketHolder.remove(channel);
        log.warn("client disconnect: {}, {}", key, channel);
        if (StringUtils.isNumeric(key)) {
            new Client().reconnect(Integer.parseInt(key));
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        Channel channel = ctx.channel();
        String key = NettySocketHolder.remove(channel);
        log.error("client lost connect: {}, {}", key, ctx.channel().toString(), cause);
        ctx.close();
        if (StringUtils.isNumeric(key)) {
            new Client().reconnect(Integer.parseInt(key));
        }
    }
}
