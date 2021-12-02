package com.airing.entity;

public class NodeInfo {
    private Integer nodeId;
    private String hostname;
    private Integer port;

    public NodeInfo(Integer nodeId, String hostname, Integer port) {
        this.nodeId = nodeId;
        this.hostname = hostname;
        this.port = port;
    }

    public Integer getNodeId() {
        return nodeId;
    }

    public void setNodeId(Integer nodeId) {
        this.nodeId = nodeId;
    }

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
}
