package com.health.management.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 消息展示对象。
 */
@Data
public class MessageVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long receiverId;
    /** 已发送列表展示用：接收人姓名；广播为“全体学生” */
    private String receiverName;
    private Integer messageType;
    private String messageTypeName;
    private String title;
    private String content;
    private Long senderId;
    private String senderName;
    private Integer sendType;
    private String sendTypeName;
    private LocalDateTime publishTime;

    /** 当前用户是否已读：0-未读，1-已读 */
    private Integer readStatus;
    private LocalDateTime readTime;

    private LocalDateTime createTime;
}
