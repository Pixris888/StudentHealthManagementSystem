package com.health.management.convert;

import com.health.management.constant.MessageConstants;
import com.health.management.entity.MessageNotification;
import com.health.management.vo.MessageVO;

/**
 * 消息实体 → VO 转换。
 */
public final class MessageConverter {

    private MessageConverter() {
    }

    public static MessageVO toVO(MessageNotification message, String senderName) {
        if (message == null) {
            return null;
        }
        MessageVO vo = new MessageVO();
        vo.setId(message.getId());
        vo.setReceiverId(message.getReceiverId());
        vo.setMessageType(message.getMessageType());
        vo.setMessageTypeName(MessageConstants.typeName(message.getMessageType()));
        vo.setTitle(message.getTitle());
        vo.setContent(message.getContent());
        vo.setSenderId(message.getSenderId());
        vo.setSenderName(senderName);
        vo.setSendType(message.getSendType());
        vo.setSendTypeName(MessageConstants.sendTypeName(message.getSendType()));
        vo.setPublishTime(message.getPublishTime());
        vo.setReadStatus(message.getReadStatus());
        vo.setCreateTime(message.getCreateTime());
        return vo;
    }

    public static String senderName(Long senderId, String realName) {
        if (senderId == null || senderId == 0L) {
            return MessageConstants.SENDER_SYSTEM;
        }
        return realName == null || realName.trim().isEmpty() ? "未知用户" : realName;
    }
}
