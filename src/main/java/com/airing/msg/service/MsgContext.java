package com.airing.msg.service;

import com.airing.entity.BaseMsg;
import com.airing.enums.MsgTypeEnum;
import io.netty.channel.ChannelHandlerContext;

public class MsgContext {
    private BaseMsg baseMsg;
    private int msgType;
    private ChannelHandlerContext ctx;

    private MsgService msgService;

    public MsgContext(BaseMsg baseMsg, ChannelHandlerContext ctx) {
        this.baseMsg = baseMsg;
        this.msgType = baseMsg.getMsgType();
        this.ctx = ctx;
    }

    public String execute() {
        MsgTypeEnum msgTypeEnum = MsgTypeEnum.getByType(msgType);
        switch (msgTypeEnum) {
            case CONNECT:
                msgService = new ConnectMsgService();
                break;
            case CONNECTED:
                msgService = new ConnectedMsgService();
                break;
            case PING:
                msgService = new PingMsgService();
                break;
            case PONG:
                msgService = new PongMsgService();
                break;
            case APP_GROUP_STAT_REQ:
                msgService = new AppGroupStateReqMsgService();
                break;
            case APP_GROUP_STAT_RESP:
                msgService = new AppGroupStateRespMsgService();
                break;
            case DB_STAT_REQ:
                msgService = new DBStateReqMsgService();
                break;
            case DB_STAT_RESP:
                msgService = new DBStateRespMsgService();
                break;
        }
        return msgService == null ? "" : msgService.msgHandler(baseMsg, ctx);
    }
}
