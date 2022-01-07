package com.airing;

import com.airing.entity.NodeInfo;
import com.airing.enums.MsgTypeEnum;
import com.airing.msg.service.CallbackHandler;
import com.airing.utils.CommonUtils;
import com.airing.utils.PropertiesUtils;
import com.airing.utils.ThreadPoolUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class Client {

    private static final Logger log = LoggerFactory.getLogger(Client.class);

    public void startClient(NodeInfo nodeInfo) throws URISyntaxException {
        String hostname = nodeInfo.getHostname();
        int port = nodeInfo.getPort();
        URI websocketURI = new URI("ws://" + hostname + ":" + port + "/psql");
        WebSocketClientHandshaker handShaker = WebSocketClientHandshakerFactory.newHandshaker(websocketURI,
                WebSocketVersion.V13, null, true, new DefaultHttpHeaders());

        EventLoopGroup group = new NioEventLoopGroup(1);
        Bootstrap clientBootstrap = new Bootstrap().group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        ChannelPipeline pipeline = socketChannel.pipeline();
                        pipeline.addLast(new IdleStateHandler(2, 1, 0));
                        pipeline.addLast(new IdleHandler());
                        pipeline.addLast(new HttpClientCodec());
                        pipeline.addLast(new HttpObjectAggregator(64 * 1024));
                        pipeline.addLast("WSClient", new WebSocketClientHandler(handShaker, nodeInfo));
                    }
                });

        ChannelFuture connectFuture = clientBootstrap.connect(websocketURI.getHost(), websocketURI.getPort());
        connectFuture.addListener(future -> {
            if (future.isSuccess()) {
                log.info("connect successfully, hostname: {}, port: {}", hostname, port);
            } else {
                log.warn("connect failed, hostname: {}, port: {}", hostname, port);
            }
        });

        connectFuture.channel().closeFuture().addListener(future -> {
            if (future.isDone()) {
                log.warn("{} and {} are disconnected, reconnecting soon!", Startup.nodeId, nodeInfo.getNodeId());
                group.shutdownGracefully();
                reconnect(nodeInfo);
            }
        });
        log.debug("connect {} end", nodeInfo.getHostname());
    }

    private void reconnect(NodeInfo nodeInfo) {
        if (NettySocketHolder.containsKey(String.valueOf(nodeInfo.getNodeId()))) {
            return;
        }
        ThreadPoolUtils.getSingle().execute(() -> {
            try {
                TimeUnit.SECONDS.sleep(5);
                startClient(nodeInfo);
            } catch (URISyntaxException e) {
                log.error("reconnect URISyntaxException: {}", e.getMessage());
            } catch (InterruptedException e) {
                log.error("reconnect InterruptedException: {}", e.getMessage());
            }
        });
    }

    public void reconnect(int nodeId) {
        for (NodeInfo nodeInfo : PropertiesUtils.anotherNodeList) {
            if (nodeInfo.getNodeId() == nodeId) {
                reconnect(nodeInfo);
                break;
            }
        }
    }

    private static class IdleHandler extends ChannelDuplexHandler {
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt instanceof IdleStateEvent) {
                IdleStateEvent e = (IdleStateEvent) evt;
                if (e.state() == IdleState.READER_IDLE) {
                    log.debug("read idle");
                    ctx.channel().close().sync();
                } else if (e.state() == IdleState.WRITER_IDLE) {
                    String baseMsg = CommonUtils.baseMsg(MsgTypeEnum.PING.getType(), null);
                    ctx.channel().writeAndFlush(new TextWebSocketFrame(baseMsg));
                    log.debug("{} send ping to {}", Startup.nodeId, ctx.channel().toString());
                }
            }
        }
    }
}
