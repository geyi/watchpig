package com.airing.msg.service;

import com.airing.entity.BaseMsg;
import com.airing.entity.DBStateReqMsg;
import com.airing.entity.DBStateRespMsg;
import com.airing.enums.DBStateEnum;
import com.airing.enums.MsgTypeEnum;
import com.airing.health.DBHealthCheck;
import com.airing.health.HealthCheckUtils;
import com.airing.utils.CommonUtils;
import com.alibaba.fastjson.JSONObject;
import io.netty.channel.ChannelHandlerContext;

public class DBStateReqMsgService implements MsgService {
    @Override
    public String msgHandler(BaseMsg baseMsg, ChannelHandlerContext ctx) {
        DBStateReqMsg dbStateReqMsg = JSONObject.parseObject(baseMsg.getContent(),
                DBStateReqMsg.class);
        boolean isHealth = DBHealthCheck.checkDBHealthByGroup2(dbStateReqMsg.getGroup());
        DBStateRespMsg dbStateRespMsg = new DBStateRespMsg();
        dbStateRespMsg.setState(isHealth ? DBStateEnum.UP.getState() : DBStateEnum.DOWN.getState());
        dbStateRespMsg.setMinNodeId(HealthCheckUtils.getMinNodeId());
        return CommonUtils.baseMsg(MsgTypeEnum.DB_STAT_RESP.getType(), baseMsg.getRequestId(),
                JSONObject.toJSONString(dbStateRespMsg));
    }
}
