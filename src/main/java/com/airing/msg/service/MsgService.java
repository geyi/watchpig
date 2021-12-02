package com.airing.msg.service;

import com.airing.entity.BaseMsg;
import io.netty.channel.ChannelHandlerContext;

public interface MsgService {
    String msgHandler(BaseMsg baseMsg, ChannelHandlerContext ctx);
}
