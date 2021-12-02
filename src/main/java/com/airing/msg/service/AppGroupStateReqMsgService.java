package com.airing.msg.service;

import com.airing.entity.AppGroupStateReqMsg;
import com.airing.entity.AppGroupStateRespMsg;
import com.airing.entity.BaseMsg;
import com.airing.enums.AppStateEnum;
import com.airing.enums.MsgTypeEnum;
import com.airing.health.AppHealthCheck;
import com.airing.health.HealthCheckUtils;
import com.airing.utils.CommonUtils;
import com.alibaba.fastjson.JSONObject;
import io.netty.channel.ChannelHandlerContext;

public class AppGroupStateReqMsgService implements MsgService {
    @Override
    public String msgHandler(BaseMsg baseMsg, ChannelHandlerContext ctx) {
        AppGroupStateReqMsg appGroupStateReqMsg = JSONObject.parseObject(baseMsg.getContent(),
                AppGroupStateReqMsg.class);
        boolean isHealth = AppHealthCheck.checkAppGroupHealth2(appGroupStateReqMsg.getGroup());
        AppGroupStateRespMsg appGroupStateRespMsg = new AppGroupStateRespMsg();
        appGroupStateRespMsg.setState(isHealth ? AppStateEnum.UP.getState() : AppStateEnum.DOWN.getState());
        appGroupStateRespMsg.setMinNodeId(HealthCheckUtils.getMinNodeId());
        return CommonUtils.baseMsg(MsgTypeEnum.APP_GROUP_STAT_RESP.getType(), baseMsg.getRequestId(),
                JSONObject.toJSONString(appGroupStateRespMsg));
    }
}
