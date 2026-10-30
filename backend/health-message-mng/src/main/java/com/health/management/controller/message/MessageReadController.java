package com.health.management.controller.message;

import com.health.management.service.MessageReadService;
import com.health.management.vo.UnreadCountVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 消息已读与未读数接口。
 */
@RestController
@RequestMapping("/api/message")
public class MessageReadController {

    @Autowired
    private MessageReadService messageReadService;

    /**
     * 当前用户未读消息数。
     */
    @GetMapping("/unread-count")
    @PreAuthorize("hasAuthority('message:list')")
    public UnreadCountVO unreadCount() {
        return messageReadService.countUnread();
    }

    /**
     * 标记单条消息已读，返回最新未读数。
     */
    @PostMapping("/{id}/read")
    @PreAuthorize("hasAuthority('message:list')")
    public UnreadCountVO markRead(@PathVariable Long id) {
        return messageReadService.markRead(id);
    }

    /**
     * 全部标记已读，返回最新未读数。
     */
    @PostMapping("/read-all")
    @PreAuthorize("hasAuthority('message:list')")
    public UnreadCountVO markAllRead() {
        return messageReadService.markAllRead();
    }
}
