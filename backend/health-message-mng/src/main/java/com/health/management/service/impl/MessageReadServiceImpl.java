package com.health.management.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.health.management.common.exception.ServiceException;
import com.health.management.common.util.ValidateUtils;
import com.health.management.constant.MessageConstants;
import com.health.management.entity.MessageNotification;
import com.health.management.entity.MessageRead;
import com.health.management.mapper.MessageNotificationMapper;
import com.health.management.mapper.MessageReadMapper;
import com.health.management.security.SecurityUtils;
import com.health.management.service.MessageReadService;
import com.health.management.vo.UnreadCountVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 消息已读与未读数实现。
 */
@Service
public class MessageReadServiceImpl extends ServiceImpl<MessageReadMapper, MessageRead>
        implements MessageReadService {

    private static final int MARK_ALL_BATCH_SIZE = 500;

    @Autowired
    private MessageNotificationMapper messageNotificationMapper;

    @Override
    public long countUnread(Long userId) {
        ValidateUtils.requireNotNull(userId, "用户ID");
        return messageNotificationMapper.countUnread(userId);
    }

    @Override
    public UnreadCountVO countUnread() {
        return new UnreadCountVO(countUnread(SecurityUtils.getUserId()));
    }

    @Override
    public UnreadCountVO markRead(Long messageId) {
        ValidateUtils.requireNotNull(messageId, "消息ID");
        Long userId = SecurityUtils.getUserId();
        MessageNotification message = messageNotificationMapper.selectById(messageId);
        if (message == null || !isVisibleToUser(message, userId)) {
            throw ServiceException.notFound("消息不存在");
        }
        baseMapper.insertIgnore(messageId, userId, LocalDateTime.now());
        return new UnreadCountVO(countUnread(userId));
    }

    @Override
    public UnreadCountVO markAllRead() {
        Long userId = SecurityUtils.getUserId();
        List<Long> unreadIds = messageNotificationMapper.selectUnreadIds(userId);
        if (unreadIds != null && !unreadIds.isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            for (int i = 0; i < unreadIds.size(); i += MARK_ALL_BATCH_SIZE) {
                int end = Math.min(i + MARK_ALL_BATCH_SIZE, unreadIds.size());
                baseMapper.insertIgnoreBatch(userId, unreadIds.subList(i, end), now);
            }
        }
        return new UnreadCountVO(countUnread(userId));
    }

    @Override
    public MessageRead getReadRecord(Long messageId, Long userId) {
        if (messageId == null || userId == null) {
            return null;
        }
        return getOne(new LambdaQueryWrapper<MessageRead>()
                .eq(MessageRead::getMessageId, messageId)
                .eq(MessageRead::getUserId, userId), false);
    }

    private boolean isVisibleToUser(MessageNotification message, Long userId) {
        Long receiverId = message.getReceiverId();
        if (receiverId != null && receiverId.equals(userId)) {
            return true;
        }
        return Long.valueOf(0L).equals(receiverId)
                && Integer.valueOf(MessageConstants.SEND_TYPE_BROADCAST).equals(message.getSendType());
    }
}
