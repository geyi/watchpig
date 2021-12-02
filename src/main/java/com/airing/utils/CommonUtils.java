package com.airing.utils;

import com.airing.entity.BaseMsg;
import com.airing.msg.service.MsgContext;
import com.alibaba.fastjson.JSONObject;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

public class CommonUtils {

    public static String baseMsg(int msgType, String content) {
        BaseMsg baseMsg = new BaseMsg();
        baseMsg.setMsgType(msgType);
        baseMsg.setTimestamp(System.currentTimeMillis());
        baseMsg.setContent(content);
        return JSONObject.toJSONString(baseMsg);
    }

    public static String baseMsg(int msgType, long requestId, String content) {
        BaseMsg baseMsg = new BaseMsg();
        baseMsg.setMsgType(msgType);
        baseMsg.setTimestamp(System.currentTimeMillis());
        baseMsg.setRequestId(requestId);
        baseMsg.setContent(content);
        return JSONObject.toJSONString(baseMsg);
    }

    public static void channelRead(String data, ChannelHandlerContext ctx) {
        if (data == null || data.length() == 0) {
            return;
        }
        BaseMsg dataMap = JSONObject.parseObject(data, BaseMsg.class);
        String respStr = new MsgContext(dataMap, ctx).execute();
        if (respStr == null || respStr.length() == 0) {
            return;
        }
        ctx.channel().writeAndFlush(new TextWebSocketFrame(respStr));
    }

}
