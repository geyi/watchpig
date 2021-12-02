package com.airing.enums;

public enum MsgTypeEnum {
    /** 客户端请求建立连接 */
    CONNECT(0, ""),
    /** ping */
    PING(1, ""),
    /** pong */
    PONG(2, ""),
    /** 服务端响应连接已建立 */
    CONNECTED(3, ""),
    /** app组状态请求 */
    APP_GROUP_STAT_REQ(4, ""),
    /** app组状态响应 */
    APP_GROUP_STAT_RESP(5, ""),
    DB_STAT_REQ(6, ""),
    DB_STAT_RESP(7, ""),
    ;

    private int type;
    private String desc;

    public int getType() {
        return type;
    }

    public String getDesc() {
        return desc;
    }

    MsgTypeEnum(int type, String desc) {
        this.type = type;
        this.desc = desc;
    }

    public static MsgTypeEnum getByType(int type) {
        for (MsgTypeEnum value : MsgTypeEnum.values()) {
            if (value.getType() == type) {
                return value;
            }
        }
        return null;
    }
}
