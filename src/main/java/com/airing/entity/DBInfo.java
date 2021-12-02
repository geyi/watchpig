package com.airing.entity;

import com.alibaba.fastjson.annotation.JSONField;

import java.sql.Connection;

public class DBInfo {
    private String hostname;
    private Integer port;
    private String username;
    private String password;
    private String group;
    /** db状态 UP: 可用状态 DOWN: 不可用状态 */
    private String state;
    /** 最近一次同步状态的时间戳 */
    private Long lastSyncTime = 0L;
    /** 得到连续相同状态的次数 */
    private Integer stateCount = 0;
    @JSONField(serialize = false, deserialize = false)
    private Connection connection;
    private String isRecovery;

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Long getLastSyncTime() {
        return lastSyncTime;
    }

    public void setLastSyncTime(Long lastSyncTime) {
        this.lastSyncTime = lastSyncTime;
    }

    public Integer getStateCount() {
        return stateCount;
    }

    public void setStateCount(Integer stateCount) {
        this.stateCount = stateCount;
    }

    public Connection getConnection() {
        return connection;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public String getIsRecovery() {
        return isRecovery;
    }

    public void setIsRecovery(String isRecovery) {
        this.isRecovery = isRecovery;
    }
}
