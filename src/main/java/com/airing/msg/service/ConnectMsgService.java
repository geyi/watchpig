package com.airing.msg.service;

import com.airing.NettySocketHolder;
import com.airing.Startup;
import com.airing.entity.BaseMsg;
import com.airing.entity.ConnectMsg;
import com.airing.entity.ConnectedMsg;
import com.airing.enums.MsgTypeEnum;
import com.airing.utils.CommonUtils;
import com.alibaba.fastjson.JSONObject;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.airing.Constant.EMPTY_STR;

public class ConnectMsgService implements MsgService {

    private static final Logger log = LoggerFactory.getLogger(ConnectMsg.class);

    @Override
    public String msgHandler(BaseMsg baseMsg, ChannelHandlerContext ctx) {
        String content = baseMsg.getContent();
        if (content == null || content.length() == 0) {
            return EMPTY_STR;
        }
        ConnectMsg connectMsg = JSONObject.parseObject(content, ConnectMsg.class);
        String idStr = String.valueOf(connectMsg.getNodeId());
        if (!NettySocketHolder.syncPut(idStr, ctx.channel())) {
            log.warn("already connect: {}, will be close!", ctx.channel().toString());
            ctx.close();
            return EMPTY_STR;
        }

        ConnectedMsg connectedMsg = new ConnectedMsg();
        connectedMsg.setClient(connectMsg.getNodeId());
        connectedMsg.setServer(Startup.nodeId);

        return CommonUtils.baseMsg(MsgTypeEnum.CONNECTED.getType(), JSONObject.toJSONString(connectedMsg));
    }
}
