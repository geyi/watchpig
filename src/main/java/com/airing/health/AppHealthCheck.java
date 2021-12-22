package com.airing.health;

import com.airing.Constant;
import com.airing.NettySocketHolder;
import com.airing.Startup;
import com.airing.entity.AppGroupStateReqMsg;
import com.airing.entity.AppGroupStateRespMsg;
import com.airing.entity.AppInfo;
import com.airing.entity.DBInfo;
import com.airing.entity.NodeInfo;
import com.airing.enums.AppStateEnum;
import com.airing.enums.MsgTypeEnum;
import com.airing.msg.service.CallbackHandler;
import com.airing.utils.CommonUtils;
import com.airing.utils.PropertiesUtils;
import com.airing.utils.http.HttpRequestUtils;
import com.alibaba.fastjson.JSONObject;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class AppHealthCheck implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(AppHealthCheck.class);

    private final List<AppInfo> appList;

    public AppHealthCheck(List<AppInfo> appList) {
        this.appList = appList;
    }

    @Override
    public void run() {
        if (appList == null || appList.isEmpty()) {
            return;
        }
        refreshAppListState(appList);
        if (log.isDebugEnabled()) {
            log.debug("app list state: {}", JSONObject.toJSONString(appList));
        }

        for (String group : PropertiesUtils.groupList) {
            if (!checkAppGroupHealth(group)) {
                if (group.equals(DBHealthCheck.replicationGroup)) {
                    log.warn("check replication app group health is false, group name: {}", group);
                    continue;
                } else if (group.equals(DBHealthCheck.primaryGroup)) {
                    log.error("check primary app group health is false, group name: {}, start failover!", group);
                }
                // 询问其它节点该组的健康的状态
                // 认为当前组不可用的watchpig的个数
                int downCount = 1;
                // 推选当前实例进行故障转移的watchpig的个数
                int failoverCount = HealthCheckUtils.getMinNodeId() == Startup.nodeId ? 1 : 0;
                List<NodeInfo> anotherNodeList = PropertiesUtils.anotherNodeList;
                if (!anotherNodeList.isEmpty()) {
                    List<CompletableFuture<String>> futures = new ArrayList<>(anotherNodeList.size());
                    for (NodeInfo nodeInfo : anotherNodeList) {
                        CompletableFuture<String> future = getRemoteAppGroupState(nodeInfo, group);
                        if (future != null) {
                            futures.add(future);
                        }
                    }
                    if (!futures.isEmpty()) {
                        for (CompletableFuture<String> future : futures) {
                            try {
                                AppGroupStateRespMsg appGroupStateRespMsg = JSONObject.parseObject(future.get(),
                                        AppGroupStateRespMsg.class);
                                if (!AppStateEnum.UP.getState().equals(appGroupStateRespMsg.getState())) {
                                    ++downCount;
                                }
                                if (Startup.nodeId == appGroupStateRespMsg.getMinNodeId()) {
                                    ++failoverCount;
                                }
                            } catch (InterruptedException e) {
                                log.error("inquiry remote app group state InterruptedException: {}", e.getMessage());
                            } catch (ExecutionException e) {
                                log.error("inquiry remote app group state ExecutionException: {}", e.getMessage());
                            }
                        }
                    }
                }
                log.info("downCount: {}, failoverCount: {}", downCount, failoverCount);
                if (downCount >= 1 && failoverCount >= 1) {
                    log.info("invoke failover shell script");
                    DBInfo dbInfo = DBHealthCheck.getDBInfoByGroup(group);
                    log.info("primary dbInfo: {}", JSONObject.toJSONString(dbInfo));
                    if (dbInfo != null) {
                        if (DBHealthCheck.checkDBHealthByGroup(group)
                                && DBHealthCheck.isPrimary(dbInfo.getHostname(), dbInfo.getPort())) {
                            HealthCheckUtils.stop(dbInfo.getHostname());
                        }
                        DBInfo replicationOnUp = DBHealthCheck.getReplicationOnUp();
                        if (replicationOnUp != null) {
                            HealthCheckUtils.failover(0, group,
                                    replicationOnUp.getPort(),
                                    replicationOnUp.getHostname(),
                                    0);
                        }
                    }

                }
            }
        }

    }

    public static CompletableFuture<String> getRemoteAppGroupState(NodeInfo nodeInfo, String group) {
        Integer nodeId = nodeInfo.getNodeId();
        Channel channel = NettySocketHolder.get(String.valueOf(nodeId));
        if (channel == null) {
            return null;
        }

        long requestId = Math.abs(UUID.randomUUID().getLeastSignificantBits());
        CompletableFuture<String> completableFuture = new CompletableFuture<>();
        CallbackHandler.add(requestId, completableFuture);

        AppGroupStateReqMsg appGroupStateReqMsg = new AppGroupStateReqMsg();
        appGroupStateReqMsg.setGroup(group);
        String baseMsg = CommonUtils.baseMsg(MsgTypeEnum.APP_GROUP_STAT_REQ.getType(), requestId,
                JSONObject.toJSONString(appGroupStateReqMsg));
        channel.writeAndFlush(new TextWebSocketFrame(baseMsg));

        return completableFuture;
    }

    public static void refreshAppListState(List<AppInfo> appList) {
        for (AppInfo appInfo : appList) {
            String oldState = appInfo.getState();
            String newSate = getAppState(appInfo);
            appInfo.setState(newSate);
            appInfo.setLastSyncTime(System.currentTimeMillis());
            appInfo.setStateCount(newSate.equals(oldState) ? appInfo.getStateCount() + 1 : 1);
        }
    }

    /**
     * 检查app组的健康状态
     *
     * @return boolean
     * @author GEYI
     * @date 2021年11月22日 10:40
     */
    public static boolean checkAppGroupHealth(String group) {
        List<AppInfo> appList = PropertiesUtils.appList;
        if (appList == null || appList.isEmpty()) {
            return true;
        }

        for (AppInfo appInfo : appList) {
            if (group.equals(appInfo.getGroup()) && checkAppHealth(appInfo)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 实时检查app组的健康状态
     *
     * @param group
     * @return boolean
     * @author GEYI
     * @date 2021年11月25日 16:43
     */
    public static boolean checkAppGroupHealth2(String group) {
        List<AppInfo> appList = PropertiesUtils.appList;
        if (appList == null || appList.isEmpty()) {
            return true;
        }

        for (AppInfo appInfo : appList) {
            if (group.equals(appInfo.getGroup()) && AppStateEnum.UP.getState().equals(getAppState(appInfo))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查单个app的健康状态
     *
     * @param appInfo
     * @return boolean
     * @author GEYI
     * @date 2021年11月22日 13:50
     */
    public static boolean checkAppHealth(AppInfo appInfo) {
        String state = appInfo.getState();
        Long lastSyncTime = appInfo.getLastSyncTime();
        Integer stateCount = appInfo.getStateCount();
        if (AppStateEnum.UP.getState().equals(state) && (System.currentTimeMillis() - 10000) < lastSyncTime
                && stateCount >= 3) {
            return true;
        } else {
            return AppStateEnum.UP.getState().equals(getAppState(appInfo));
        }
    }

    /**
     * 获取app的健康状态
     *
     * @param appInfo
     * @return java.lang.String
     * @author GEYI
     * @date 2021年11月22日 13:51
     */
    public static String getAppState(AppInfo appInfo) {
        String healthApi = String.format(Constant.APP_HEALTH_API, appInfo.getHostname(), appInfo.getPort(),
                appInfo.getHealthApi());
        String appState = null;
        try {
            appState = HttpRequestUtils.get(healthApi, 2000, 2000);
        } catch (Exception e) {
        }
        if (AppStateEnum.UP.getState().equals(appState)) {
            appState = AppStateEnum.UP.getState();
        } else {
            appState = AppStateEnum.DOWN.getState();
        }
        return appState;
    }
}
