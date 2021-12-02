package com.airing.entity;

public class AppGroupStateRespMsg {
    private String state;
    private Integer minNodeId;

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Integer getMinNodeId() {
        return minNodeId;
    }

    public void setMinNodeId(Integer minNodeId) {
        this.minNodeId = minNodeId;
    }
}
