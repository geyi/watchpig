package com.airing.health;

import com.airing.Constant;
import com.airing.NettySocketHolder;
import com.airing.Startup;
import com.airing.entity.DBInfo;
import com.airing.entity.DBStateReqMsg;
import com.airing.entity.DBStateRespMsg;
import com.airing.entity.NodeInfo;
import com.airing.enums.DBStateEnum;
import com.airing.enums.MsgTypeEnum;
import com.airing.msg.service.CallbackHandler;
import com.airing.utils.CommonUtils;
import com.airing.utils.PropertiesUtils;
import com.alibaba.fastjson.JSONObject;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class DBHealthCheck implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(DBHealthCheck.class);

    public static List<DBInfo> dbList;
    public static String primaryGroup;
    public static String replicationGroup;

    public DBHealthCheck(List<DBInfo> dbList) {
        DBHealthCheck.dbList = dbList;
        for (DBInfo dbInfo : dbList) {
            dbInfo.setConnection(getConnection(dbInfo));
        }
    }

    private static Connection getConnection(DBInfo dbInfo) {
        try {
            String dbUrl = String.format(Constant.DB_URL, dbInfo.getHostname(), dbInfo.getPort());
            String username = dbInfo.getUsername();
            String password = dbInfo.getPassword();
            Class.forName("org.postgresql.Driver");
            return DriverManager.getConnection(dbUrl, username, password);
        } catch (Exception e) {
            log.error("getConnect error: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public void run() {
        try {
            refreshDBListState(dbList);
            if (log.isDebugEnabled()) {
                log.debug("db list state: {}, primaryGroup: {}, replicationGroup: {}",
                        JSONObject.toJSONString(dbList),
                        primaryGroup,
                        replicationGroup);
            }

            for (DBInfo dbInfo : dbList) {
                if (!DBStateEnum.UP.getState().equals(dbInfo.getState())) {
                    String group = dbInfo.getGroup();
                    if (group.equals(replicationGroup)) {
                        log.warn("check replication db group health is false, group name: {}", group);
                        continue;
                    } else if (group.equals(primaryGroup)) {
                        log.error("check primary db group health is false, group name: {}, start failover!", group);
                    }

                    // 认为主库宕机的节点数
                    int downCount = 1;
                    // 认为应该由当前节点进行故障转移的节点数
                    int failoverCount = HealthCheckUtils.getMinNodeId() == Startup.nodeId ? 1 : 0;
                    List<NodeInfo> anotherNodeList = PropertiesUtils.anotherNodeList;
                    if (!anotherNodeList.isEmpty()) {
                        List<CompletableFuture<String>> futures = new ArrayList<>(anotherNodeList.size());
                        for (NodeInfo nodeInfo : anotherNodeList) {
                            CompletableFuture<String> future = getRemoteDBGroupState(nodeInfo, group);
                            if (future != null) {
                                futures.add(future);
                            }
                        }
                        if (!futures.isEmpty()) {
                            for (CompletableFuture<String> future : futures) {
                                try {
                                    DBStateRespMsg dbStateRespMsg = JSONObject.parseObject(future.get(),
                                            DBStateRespMsg.class);
                                    if (!DBStateEnum.UP.getState().equals(dbStateRespMsg.getState())) {
                                        ++downCount;
                                    }
                                    if (Startup.nodeId == dbStateRespMsg.getMinNodeId()) {
                                        ++failoverCount;
                                    }
                                } catch (InterruptedException e) {
                                    log.error("inquiry remote db state InterruptedException: {}", e.getMessage());
                                } catch (ExecutionException e) {
                                    log.error("inquiry remote db state ExecutionException: {}", e.getMessage());
                                }
                            }
                        }
                    }
                    log.info("downCount: {}, failoverCount: {}", downCount, failoverCount);
                    if (downCount >= 1 && failoverCount >= 1) {
                        log.info("invoke failover shell script");
                        DBInfo replicationOnUp = getReplicationOnUp();
                        if (replicationOnUp != null) {
                            HealthCheckUtils.failover(dbInfo.getPort(), dbInfo.getHostname(),
                                    replicationOnUp.getPort(),
                                    replicationOnUp.getHostname(),
                                    dbInfo.getPort());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("run error: {}", e.getMessage());
        }
    }

    public static CompletableFuture<String> getRemoteDBGroupState(NodeInfo nodeInfo, String group) {
        Integer nodeId = nodeInfo.getNodeId();
        Channel channel = NettySocketHolder.get(String.valueOf(nodeId));
        if (channel == null) {
            return null;
        }

        long requestId = Math.abs(UUID.randomUUID().getLeastSignificantBits());
        DBStateReqMsg dbStateReqMsg = new DBStateReqMsg();
        dbStateReqMsg.setGroup(group);
        String baseMsg = CommonUtils.baseMsg(MsgTypeEnum.DB_STAT_REQ.getType(), requestId,
                JSONObject.toJSONString(dbStateReqMsg));
        channel.writeAndFlush(new TextWebSocketFrame(baseMsg));

        CompletableFuture<String> completableFuture = new CompletableFuture<>();
        CallbackHandler.add(requestId, completableFuture);
        return completableFuture;
    }

    public static void refreshDBListState(List<DBInfo> dbList) {
        for (DBInfo dbInfo : dbList) {
            String oldState = dbInfo.getState();
            String newSate = getDBState(dbInfo);
            dbInfo.setState(newSate);
            dbInfo.setLastSyncTime(System.currentTimeMillis());
            dbInfo.setStateCount(newSate.equals(oldState) ? dbInfo.getStateCount() + 1 : 1);
            if ("f".equals(dbInfo.getIsRecovery())) {
                primaryGroup = dbInfo.getGroup();
            } else {
                replicationGroup = dbInfo.getGroup();
            }
        }
    }

    public static boolean checkDBHealthByGroup(String group) {
        for (DBInfo dbInfo : PropertiesUtils.dbList) {
            if (dbInfo.getGroup().equals(group)) {
                return checkDBHealth(dbInfo);
            }
        }
        return false;
    }

    public static boolean checkDBHealthByGroup2(String group) {
        for (DBInfo dbInfo : PropertiesUtils.dbList) {
            if (dbInfo.getGroup().equals(group)) {
                return DBStateEnum.UP.getState().equals(getDBState(dbInfo));
            }
        }
        return false;
    }

    public static boolean checkDBHealth(DBInfo dbInfo) {
        String state = dbInfo.getState();
        Long lastSyncTime = dbInfo.getLastSyncTime();
        Integer stateCount = dbInfo.getStateCount();
        if (DBStateEnum.UP.getState().equals(state) && (System.currentTimeMillis() - 10000) < lastSyncTime
                && stateCount >= 3) {
            return true;
        } else {
            return DBStateEnum.UP.getState().equals(getDBState(dbInfo));
        }
    }

    public static String getDBState(DBInfo dbInfo) {
        String isRecovery = isRecovery(dbInfo);
        return StringUtils.isEmpty(isRecovery) ? DBStateEnum.DOWN.getState() : DBStateEnum.UP.getState();
    }

    public static String isRecovery(DBInfo dbInfo) {
        try {
            Connection connection = dbInfo.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement("select pg_is_in_recovery();");
            ResultSet resultSet = preparedStatement.executeQuery();
            resultSet.next();
            String isRecovery = resultSet.getString("pg_is_in_recovery");
            // 如果是主库（这时说明主库可用），则去把原来挂掉的主库isRecovery设置为t（如果有的话）
            if ("f".equals(isRecovery)) {
                resetRecovery();
            }
            dbInfo.setIsRecovery(isRecovery);
            return isRecovery;
        } catch (Exception e) {
            log.error("isRecovery error: {}", e.getMessage());
            if (dbInfo.getConnection() == null || e.getMessage().contains("This connection has been closed")) {
                dbInfo.setConnection(getConnection(dbInfo));
            }
            return null;
        }
    }

    public static boolean isPrimary(String host, int port) {
        String isRecovery = null;
        for (DBInfo dbInfo : PropertiesUtils.dbList) {
            if (dbInfo.getHostname().equals(host) && dbInfo.getPort() == port) {
                isRecovery = isRecovery(dbInfo);
            }
        }
        return StringUtils.equals(isRecovery, "f");
    }

    private static void resetRecovery() {
        for (DBInfo dbInfo : PropertiesUtils.dbList) {
            if (DBStateEnum.DOWN.getState().equals(dbInfo.getState()) && "f".equals(dbInfo.getIsRecovery())) {
                dbInfo.setIsRecovery("t");
            }
        }
    }

    public static DBInfo getReplicationOnUp() {
        for (DBInfo dbInfo : PropertiesUtils.dbList) {
            if (DBStateEnum.UP.getState().equals(dbInfo.getState()) && "t".equals(dbInfo.getIsRecovery())) {
                return dbInfo;
            }
        }
        return null;
    }

    public static DBInfo getDBInfoByGroup(String group) {
        for (DBInfo dbInfo : PropertiesUtils.dbList) {
            if (dbInfo.getGroup().equals(group)) {
                return dbInfo;
            }
        }
        return null;
    }
}
