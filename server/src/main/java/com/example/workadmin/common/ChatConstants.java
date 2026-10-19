package com.example.workadmin.common;

/**
 * 聊天模块常量，避免魔法字符串散落各处。
 */
public final class ChatConstants {

    private ChatConstants() {
    }

    public static final String CONVERSATION_PRIVATE = "PRIVATE";
    public static final String CONVERSATION_GROUP = "GROUP";

    public static final String ROLE_OWNER = "OWNER";
    public static final String ROLE_MEMBER = "MEMBER";

    public static final String DEVICE_PENDING = "PENDING";
    public static final String DEVICE_ACTIVE = "ACTIVE";
    public static final String DEVICE_REVOKED = "REVOKED";

    public static final String LINK_PENDING = "PENDING";
    public static final String LINK_APPROVED = "APPROVED";
    public static final String LINK_REJECTED = "REJECTED";

    public static final String POLICY_AUTO_ALLOW = "AUTO_ALLOW";
    public static final String POLICY_AUTO_DENY = "AUTO_DENY";
    public static final String POLICY_MANUAL = "MANUAL";

    public static final String OVERRIDE_INHERIT = "INHERIT";
    public static final String OVERRIDE_ALLOW = "ALLOW";
    public static final String OVERRIDE_DENY = "DENY";

    public static final int MAX_GROUP_MEMBERS = 200;
    public static final int MAX_ONE_TIME_PRE_KEYS = 200;
}
