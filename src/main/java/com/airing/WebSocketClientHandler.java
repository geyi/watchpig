package com.airing;

import com.airing.entity.ConnectMsg;
import com.airing.entity.NodeInfo;
import com.airing.enums.MsgTypeEnum;
import com.airing.utils.CommonUtils;
import com.alibaba.fastjson.JSONObject;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketHandshakeException;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebSocketClientHandler extends SimpleChannelInboundHandler<Object> {

    private static final Logger log = LoggerFactory.getLogger(WebSocketClientHandler.class);

    private WebSocketClientHandshaker handShaker;
    private ChannelPromise handshakeFuture;
    private NodeInfo nodeInfo;

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        this.handshakeFuture = ctx.newPromise();
    }

    public ChannelFuture handshakeFuture() {
        return this.handshakeFuture;
    }

    public WebSocketClientHandler(WebSocketClientHandshaker handShaker, NodeInfo nodeInfo) {
        this.handShaker = handShaker;
        this.nodeInfo = nodeInfo;
    }

    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        Channel ch = ctx.channel();
        FullHttpResponse response;
        if (!this.handShaker.isHandshakeComplete()) {
            try {
                response = (FullHttpResponse) msg;
                // 握手协议返回，设置结束握手
                this.handShaker.finishHandshake(ch, response);
                // 设置成功
                this.handshakeFuture.setSuccess();
                log.info("WebSocket Client connected! response headers[sec-websocket-extensions]: {}", response.headers());

                ConnectMsg connectMsg = new ConnectMsg();
                connectMsg.setNodeId(Startup.nodeId);
                String connMsg = CommonUtils.baseMsg(MsgTypeEnum.CONNECT.getType(), JSONObject.toJSONString(connectMsg));
                ctx.channel().writeAndFlush(new TextWebSocketFrame(connMsg));
            } catch (WebSocketHandshakeException var7) {
                FullHttpResponse res = (FullHttpResponse) msg;
                String errorMsg = String.format("client failed to connect, status: %s, reason: %s",
                        res.status(), res.content().toString(CharsetUtil.UTF_8));
                this.handshakeFuture.setFailure(new Exception(errorMsg));
            }
        } else {
            WebSocketFrame frame = (WebSocketFrame) msg;
            if (frame instanceof TextWebSocketFrame) {
                TextWebSocketFrame textFrame = (TextWebSocketFrame) frame;
                String data = textFrame.text();
                log.debug("client data: {}", data);
                CommonUtils.channelRead(data, ctx);
            }
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        String hostname = nodeInfo.getHostname();
        int port = nodeInfo.getPort();
        ChannelFuture handshakeFuture = handShaker.handshake(ctx.channel());
        handshakeFuture.addListener(future -> {
            if (handshakeFuture.isSuccess()) {
                log.info("handshake successfully, hostname: {}, port: {}", hostname, port);
            } else {
                log.warn("handshake failed, hostname: {}, port: {}", hostname, port);
            }
        });
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        String key = NettySocketHolder.remove(channel);
        log.warn("disconnect: {}, {}", key, channel);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        Channel channel = ctx.channel();
        String key = NettySocketHolder.remove(channel);
        log.error("lost connect: {}, {}", key, ctx.channel().toString(), cause);
        ctx.close();
    }
}
