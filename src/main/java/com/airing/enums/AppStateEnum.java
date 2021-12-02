package com.airing.enums;

public enum AppStateEnum {
    UP("UP"),
    DOWN("DOWN"),
    ;

    private String state;

    public String getState() {
        return state;
    }

    AppStateEnum(String state) {
        this.state = state;
    }
}
