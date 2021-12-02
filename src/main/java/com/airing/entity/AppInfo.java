package com.airing.entity;

public class AppInfo {
    private String hostname;
    private Integer port;
    private String healthApi;
    /** app所属的组名 */
    private String group;
    /** app状态 UP: 可用状态 DOWN: 不可用状态 */
    private String state;
    /** 最近一次同步状态的时间戳 */
    private Long lastSyncTime = 0L;
    /** 得到连续相同状态的次数 */
    private Integer stateCount = 0;

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

    public String getHealthApi() {
        return healthApi;
    }

    public void setHealthApi(String healthApi) {
        this.healthApi = healthApi;
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
}
