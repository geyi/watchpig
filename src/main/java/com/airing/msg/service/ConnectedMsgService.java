package com.airing.msg.service;

import com.airing.NettySocketHolder;
import com.airing.entity.BaseMsg;
import com.airing.entity.ConnectedMsg;
import com.alibaba.fastjson.JSONObject;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.airing.Constant.EMPTY_STR;

public class ConnectedMsgService implements MsgService {

    private static final Logger log = LoggerFactory.getLogger(ConnectedMsg.class);

    @Override
    public String msgHandler(BaseMsg baseMsg, ChannelHandlerContext ctx) {
        ConnectedMsg connectedMsg = JSONObject.parseObject(baseMsg.getContent(), ConnectedMsg.class);
        Channel channel = ctx.channel();
        if (!NettySocketHolder.syncPut(String.valueOf(connectedMsg.getServer()), channel)) {
            log.warn("already connect: {}, will be close!", channel.toString());
            ctx.close();
            return EMPTY_STR;
        }
        return EMPTY_STR;
    }
}
