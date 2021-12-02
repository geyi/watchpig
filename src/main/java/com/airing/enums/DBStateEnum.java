package com.airing.enums;

public enum DBStateEnum {
    UP("UP"),
    DOWN("DOWN"),
    ;

    private String state;

    public String getState() {
        return state;
    }

    DBStateEnum(String state) {
        this.state = state;
    }
}
