package com.airing.msg.service;

import com.airing.Startup;
import com.airing.entity.BaseMsg;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PongMsgService implements MsgService {

    private static final Logger log = LoggerFactory.getLogger(PongMsgService.class);

    @Override
    public String msgHandler(BaseMsg baseMsg, ChannelHandlerContext ctx) {
        log.debug("{} receive pong {}", Startup.nodeId, ctx.channel().toString());
        return null;
    }
}
