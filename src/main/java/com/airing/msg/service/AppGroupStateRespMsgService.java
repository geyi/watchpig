package com.airing.msg.service;

import com.airing.entity.BaseMsg;
import io.netty.channel.ChannelHandlerContext;

public class AppGroupStateRespMsgService implements MsgService {
    @Override
    public String msgHandler(BaseMsg baseMsg, ChannelHandlerContext ctx) {
        CallbackHandler.run(baseMsg);
        return null;
    }
}
