package com.health.management.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.health.management.entity.MessageRead;
import com.health.management.vo.UnreadCountVO;

/**
 * 消息已读与未读数。
 */
public interface MessageReadService extends IService<MessageRead> {

    /** 指定用户未读数（内部使用） */
    long countUnread(Long userId);

    /** 当前用户未读数 */
    UnreadCountVO countUnread();

    /** 当前用户标记单条已读，返回最新未读数 */
    UnreadCountVO markRead(Long messageId);

    /** 当前用户全部已读，返回最新未读数 */
    UnreadCountVO markAllRead();

    /** 查询当前用户的已读记录，未读返回 null */
    MessageRead getReadRecord(Long messageId, Long userId);
}
