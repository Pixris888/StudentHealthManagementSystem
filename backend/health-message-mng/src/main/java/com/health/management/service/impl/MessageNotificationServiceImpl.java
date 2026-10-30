package com.health.management.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.health.management.common.constant.RoleConstants;
import com.health.management.common.exception.ServiceException;
import com.health.management.common.util.ValidateUtils;
import com.health.management.config.MessageProperties;
import com.health.management.constant.MessageConstants;
import com.health.management.convert.MessageConverter;
import com.health.management.dto.MessageQueryDTO;
import com.health.management.dto.MessageReceiverQueryDTO;
import com.health.management.dto.MessageSendDTO;
import com.health.management.entity.MessageNotification;
import com.health.management.entity.MessageRead;
import com.health.management.entity.SysUser;
import com.health.management.mapper.MessageNotificationMapper;
import com.health.management.mapper.MessageReadMapper;
import com.health.management.mapper.SysUserMapper;
import com.health.management.security.SecurityUtils;
import com.health.management.service.MessageNotificationService;
import com.health.management.vo.MessageReceiverVO;
import com.health.management.vo.MessageVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 消息收件箱、发送与已发送列表实现。
 */
@Service
public class MessageNotificationServiceImpl
        extends ServiceImpl<MessageNotificationMapper, MessageNotification>
        implements MessageNotificationService {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private MessageReadMapper messageReadMapper;

    @Autowired
    private MessageProperties messageProperties;

    @Override
    public IPage<MessageVO> pageMine(MessageQueryDTO query) {
        MessageQueryDTO q = query == null ? new MessageQueryDTO() : query;
        ValidateUtils.requirePage(q.getPage(), q.getSize());
        int current = q.getPage() == null ? 1 : q.getPage();
        int size = q.getSize() == null ? 10 : q.getSize();

        IPage<MessageVO> page = baseMapper.selectInboxPage(
                new Page<>(current, size), SecurityUtils.getUserId(), q);
        fillDerived(page.getRecords());
        return page;
    }

    @Override
    public MessageVO getMyDetail(Long id) {
        ValidateUtils.requireNotNull(id, "消息ID");
        Long userId = SecurityUtils.getUserId();
        MessageNotification message = getById(id);
        if (message == null || !isVisibleToUser(message, userId)) {
            throw ServiceException.notFound("消息不存在");
        }
        MessageVO vo = MessageConverter.toVO(message, resolveSenderName(message.getSenderId()));
        MessageRead read = messageReadMapper.selectOne(new LambdaQueryWrapper<MessageRead>()
                .eq(MessageRead::getMessageId, id)
                .eq(MessageRead::getUserId, userId));
        vo.setReadStatus(read == null ? 0 : 1);
        vo.setReadTime(read == null ? null : read.getReadTime());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int sendMessages(MessageSendDTO dto) {
        if (!SecurityUtils.isDoctorOrAdmin()) {
            throw ServiceException.forbidden("仅校医或超级管理员可发送通知");
        }
        ValidateUtils.requireNotNull(dto, "请求参数");
        ValidateUtils.requireNotNull(dto.getMessageType(), "消息类型");
        ValidateUtils.requireText(dto.getTitle(), "消息标题");
        ValidateUtils.requireText(dto.getContent(), "消息内容");
        int messageType = dto.getMessageType();
        if (messageType < MessageConstants.TYPE_ANNOUNCEMENT
                || messageType > MessageConstants.TYPE_OTHER) {
            throw ServiceException.badRequest("手工消息类型只能是3-公告、4-关怀留言、5-其他");
        }

        String title = dto.getTitle().trim();
        String content = dto.getContent().trim();
        Long senderId = SecurityUtils.getUserId();
        LocalDateTime now = LocalDateTime.now();
        List<MessageNotification> messages = new ArrayList<>();

        if (messageType == MessageConstants.TYPE_ANNOUNCEMENT) {
            if (dto.getReceiverIds() != null && !dto.getReceiverIds().isEmpty()) {
                throw ServiceException.badRequest("校园公告为全体广播，无需指定接收人");
            }
            messages.add(buildMessage(0L, messageType, title, content,
                    senderId, MessageConstants.SEND_TYPE_BROADCAST, now, null));
        } else {
            List<Long> receiverIds = normalizeReceiverIds(dto.getReceiverIds());
            List<SysUser> users = sysUserMapper.selectBatchIds(receiverIds);
            Map<Long, SysUser> userMap = new HashMap<>();
            for (SysUser user : users) {
                userMap.put(user.getId(), user);
            }
            for (Long receiverId : receiverIds) {
                SysUser receiver = userMap.get(receiverId);
                if (receiver == null
                        || !Integer.valueOf(RoleConstants.USER_TYPE_STUDENT).equals(receiver.getUserType())
                        || !Integer.valueOf(1).equals(receiver.getStatus())) {
                    throw ServiceException.badRequest("接收人不存在或不是启用的学生账号：" + receiverId);
                }
                messages.add(buildMessage(receiverId, messageType, title, content,
                        senderId, MessageConstants.SEND_TYPE_PERSONAL, now, null));
            }
        }

        boolean success = messages.size() == 1 ? save(messages.get(0)) : saveBatch(messages);
        if (!success) {
            throw ServiceException.fail("消息发送失败，请稍后重试");
        }
        return messages.size();
    }

    @Override
    public IPage<MessageVO> pageSent(MessageQueryDTO query) {
        if (!SecurityUtils.isDoctorOrAdmin()) {
            throw ServiceException.forbidden("仅校医或超级管理员可查看发送记录");
        }
        MessageQueryDTO q = query == null ? new MessageQueryDTO() : query;
        ValidateUtils.requirePage(q.getPage(), q.getSize());
        int current = q.getPage() == null ? 1 : q.getPage();
        int size = q.getSize() == null ? 10 : q.getSize();

        LambdaQueryWrapper<MessageNotification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MessageNotification::getSenderId, SecurityUtils.getUserId());
        if (q.getMessageType() != null) {
            wrapper.eq(MessageNotification::getMessageType, q.getMessageType());
        }
        wrapper.orderByDesc(MessageNotification::getPublishTime)
                .orderByDesc(MessageNotification::getId);

        IPage<MessageNotification> page = page(new Page<>(current, size), wrapper);
        Page<MessageVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<MessageVO> records = new ArrayList<>();
        Map<Long, String> senderNames = loadSenderNames(page.getRecords());
        Map<Long, String> receiverNames = loadReceiverNames(page.getRecords());
        for (MessageNotification message : page.getRecords()) {
            MessageVO vo = MessageConverter.toVO(message,
                    MessageConverter.senderName(message.getSenderId(),
                            senderNames.get(message.getSenderId())));
            if (Long.valueOf(0L).equals(message.getReceiverId())) {
                vo.setReceiverName("全体学生");
            } else {
                vo.setReceiverName(receiverNames.getOrDefault(message.getReceiverId(),
                        "用户#" + message.getReceiverId()));
            }
            records.add(vo);
        }
        result.setRecords(records);
        return result;
    }

    @Override
    public IPage<MessageReceiverVO> pageReceivers(MessageReceiverQueryDTO query) {
        if (!SecurityUtils.isDoctorOrAdmin()) {
            throw ServiceException.forbidden("仅校医或超级管理员可选择学生接收人");
        }
        MessageReceiverQueryDTO q = query == null ? new MessageReceiverQueryDTO() : query;
        ValidateUtils.requirePage(q.getPage(), q.getSize());
        int current = q.getPage() == null ? 1 : q.getPage();
        int size = q.getSize() == null ? 20 : q.getSize();

        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(SysUser::getId, SysUser::getUsername, SysUser::getRealName,
                        SysUser::getStudentNo, SysUser::getDeptName)
                .eq(SysUser::getUserType, RoleConstants.USER_TYPE_STUDENT)
                .eq(SysUser::getStatus, 1);
        if (ValidateUtils.hasText(q.getKeyword())) {
            String keyword = q.getKeyword().trim();
            wrapper.and(w -> w.like(SysUser::getRealName, keyword)
                    .or()
                    .like(SysUser::getStudentNo, keyword)
                    .or()
                    .like(SysUser::getUsername, keyword));
        }
        wrapper.orderByAsc(SysUser::getId);

        IPage<SysUser> page = sysUserMapper.selectPage(new Page<>(current, size), wrapper);
        Page<MessageReceiverVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<MessageReceiverVO> records = new ArrayList<>();
        for (SysUser user : page.getRecords()) {
            MessageReceiverVO vo = new MessageReceiverVO();
            vo.setId(user.getId());
            vo.setUsername(user.getUsername());
            vo.setRealName(user.getRealName());
            vo.setStudentNo(user.getStudentNo());
            vo.setDeptName(user.getDeptName());
            records.add(vo);
        }
        result.setRecords(records);
        return result;
    }

    @Override
    public boolean isVisibleToUser(MessageNotification message, Long userId) {
        if (message == null || userId == null) {
            return false;
        }
        Long receiverId = message.getReceiverId();
        if (receiverId != null && receiverId.equals(userId)) {
            return true;
        }
        return Long.valueOf(0L).equals(receiverId)
                && Integer.valueOf(MessageConstants.SEND_TYPE_BROADCAST).equals(message.getSendType());
    }

    private MessageNotification buildMessage(Long receiverId, int messageType, String title,
                                             String content, Long senderId, int sendType,
                                             LocalDateTime publishTime, String dedupKey) {
        MessageNotification message = new MessageNotification();
        message.setReceiverId(receiverId);
        message.setMessageType(messageType);
        message.setTitle(title);
        message.setContent(content);
        message.setSenderId(senderId);
        message.setSendType(sendType);
        message.setPublishTime(publishTime);
        message.setDedupKey(dedupKey);
        return message;
    }

    private List<Long> normalizeReceiverIds(List<Long> receiverIds) {
        if (receiverIds == null || receiverIds.isEmpty()) {
            throw ServiceException.badRequest("请选择接收人");
        }
        int max = messageProperties.getMaxRecipients() == null
                ? MessageConstants.MAX_RECIPIENTS : messageProperties.getMaxRecipients();
        if (receiverIds.size() > max) {
            throw ServiceException.badRequest("单次最多发送给" + max + "名学生");
        }
        Set<Long> uniqueIds = new LinkedHashSet<>();
        for (Long id : receiverIds) {
            if (id == null || id <= 0) {
                throw ServiceException.badRequest("接收人ID不合法");
            }
            uniqueIds.add(id);
        }
        if (uniqueIds.size() > max) {
            throw ServiceException.badRequest("单次最多发送给" + max + "名学生");
        }
        return new ArrayList<>(uniqueIds);
    }

    private void fillDerived(List<MessageVO> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        Map<Long, String> senderNames = loadSenderNamesForVOs(records);
        for (MessageVO vo : records) {
            vo.setMessageTypeName(MessageConstants.typeName(vo.getMessageType()));
            vo.setSendTypeName(MessageConstants.sendTypeName(vo.getSendType()));
            vo.setSenderName(MessageConverter.senderName(vo.getSenderId(),
                    senderNames.get(vo.getSenderId())));
        }
    }

    private Map<Long, String> loadSenderNamesForVOs(List<MessageVO> records) {
        Set<Long> senderIds = new LinkedHashSet<>();
        for (MessageVO vo : records) {
            if (vo.getSenderId() != null && vo.getSenderId() != 0L) {
                senderIds.add(vo.getSenderId());
            }
        }
        if (senderIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, String> result = new HashMap<>();
        for (SysUser user : sysUserMapper.selectBatchIds(senderIds)) {
            result.put(user.getId(), user.getRealName());
        }
        return result;
    }

    private Map<Long, String> loadSenderNames(List<MessageNotification> messages) {
        Set<Long> senderIds = new LinkedHashSet<>();
        for (MessageNotification message : messages) {
            if (message.getSenderId() != null && message.getSenderId() != 0L) {
                senderIds.add(message.getSenderId());
            }
        }
        if (senderIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, String> result = new HashMap<>();
        for (SysUser user : sysUserMapper.selectBatchIds(senderIds)) {
            result.put(user.getId(), user.getRealName());
        }
        return result;
    }

    private Map<Long, String> loadReceiverNames(List<MessageNotification> messages) {
        Set<Long> receiverIds = new LinkedHashSet<>();
        for (MessageNotification message : messages) {
            if (message.getReceiverId() != null && message.getReceiverId() != 0L) {
                receiverIds.add(message.getReceiverId());
            }
        }
        if (receiverIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, String> result = new HashMap<>();
        for (SysUser user : sysUserMapper.selectBatchIds(receiverIds)) {
            result.put(user.getId(), user.getRealName());
        }
        return result;
    }

    private String resolveSenderName(Long senderId) {
        if (senderId == null || senderId == 0L) {
            return MessageConstants.SENDER_SYSTEM;
        }
        SysUser user = sysUserMapper.selectById(senderId);
        return user == null ? "未知用户" : user.getRealName();
    }
}
