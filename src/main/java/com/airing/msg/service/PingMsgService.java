package com.airing.msg.service;

import com.airing.Startup;
import com.airing.entity.BaseMsg;
import com.airing.enums.MsgTypeEnum;
import com.airing.utils.CommonUtils;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PingMsgService implements MsgService {

    private static final Logger log = LoggerFactory.getLogger(PingMsgService.class);

    @Override
    public String msgHandler(BaseMsg baseMsg, ChannelHandlerContext ctx) {
        log.debug("{} receive ping from {}", Startup.nodeId, ctx.channel().toString());
        return CommonUtils.baseMsg(MsgTypeEnum.PONG.getType(), null);
    }
}
