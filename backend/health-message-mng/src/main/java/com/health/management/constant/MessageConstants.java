package com.health.management.constant;

/**
 * 消息通知模块常量。
 */
public final class MessageConstants {

    /** 熬夜预警（系统生成） */
    public static final int TYPE_STAY_UP_LATE = 1;
    /** 运动不足预警（系统生成） */
    public static final int TYPE_EXERCISE_INSUFFICIENT = 2;
    /** 校园公告 */
    public static final int TYPE_ANNOUNCEMENT = 3;
    /** 健康关怀留言 */
    public static final int TYPE_CARE = 4;
    /** 其他 */
    public static final int TYPE_OTHER = 5;

    /** 个人消息 */
    public static final int SEND_TYPE_PERSONAL = 1;
    /** 全体广播 */
    public static final int SEND_TYPE_BROADCAST = 2;
    /** 按条件发送（当前版本预留） */
    public static final int SEND_TYPE_CONDITION = 3;

    /** 单次群发最多接收人数 */
    public static final int MAX_RECIPIENTS = 100;

    public static final String SENDER_SYSTEM = "系统";

    private MessageConstants() {
    }

    public static String typeName(Integer messageType) {
        if (messageType == null) {
            return "未知";
        }
        switch (messageType) {
            case TYPE_STAY_UP_LATE:
                return "熬夜预警";
            case TYPE_EXERCISE_INSUFFICIENT:
                return "运动不足预警";
            case TYPE_ANNOUNCEMENT:
                return "校园公告";
            case TYPE_CARE:
                return "健康关怀";
            case TYPE_OTHER:
                return "其他消息";
            default:
                return "未知";
        }
    }

    public static String sendTypeName(Integer sendType) {
        if (sendType == null) {
            return "未知";
        }
        switch (sendType) {
            case SEND_TYPE_PERSONAL:
                return "个人";
            case SEND_TYPE_BROADCAST:
                return "全体";
            case SEND_TYPE_CONDITION:
                return "按条件";
            default:
                return "未知";
        }
    }

    public static boolean isSystemWarningType(Integer messageType) {
        return messageType != null
                && (messageType == TYPE_STAY_UP_LATE || messageType == TYPE_EXERCISE_INSUFFICIENT);
    }
}
