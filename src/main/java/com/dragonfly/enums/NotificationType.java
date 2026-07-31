package com.dragonfly.enums;

/**
 * 描述：
 *
 * @param
 * @author 蜻蜓大王
 * @date 2026/7/31 19:53
 */
public enum NotificationType {
    LIKE_NOTE(1, "点赞了你的笔记"),
    LIKE_COMMENT(2, "点赞了你的评论"),
    COMMENT_NOTE(3, "评论了你的笔记"),
    FOLLOW(4, "关注了你"),
    SYSTEM(5, "系统通知");

    private final int code;
    private final String message;

    NotificationType(int code, String message) {
        this.code = code;
        this.message = message;
    }
    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    // 根据code获取枚举类型
    public static NotificationType fromCode(int code) {
        for (NotificationType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        return SYSTEM;
    }

}
