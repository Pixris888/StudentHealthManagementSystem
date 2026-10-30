package com.health.management.controller.message;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.health.management.dto.MessageQueryDTO;
import com.health.management.dto.MessageReceiverQueryDTO;
import com.health.management.dto.MessageSendDTO;
import com.health.management.service.MessageNotificationService;
import com.health.management.service.MessageWarningService;
import com.health.management.vo.MessageReceiverVO;
import com.health.management.vo.MessageVO;
import com.health.management.vo.WarningRunResultVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.time.LocalDate;

/**
 * 消息通知接口：收件箱、发送、已发送列表、手动预警。
 * 已读相关接口在 MessageReadController。
 */
@RestController
@RequestMapping("/api/message")
public class MessageNotificationController {

    @Autowired
    private MessageNotificationService messageNotificationService;

    @Autowired
    private MessageWarningService messageWarningService;

    /**
     * 当前用户收件箱（个人消息 + 全体广播）。
     */
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('message:list')")
    public IPage<MessageVO> page(@Valid MessageQueryDTO query) {
        return messageNotificationService.pageMine(query);
    }

    /**
     * 当前用户查看消息详情。
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('message:list')")
    public MessageVO detail(@PathVariable Long id) {
        return messageNotificationService.getMyDetail(id);
    }

    /**
     * 校医/超管发送公告或健康关怀留言。
     * 公告为全体广播；关怀留言/其他消息按 receiverIds 逐人发送。
     */
    @PostMapping("/send")
    @PreAuthorize("hasAuthority('message:send')")
    public int send(@Valid @RequestBody MessageSendDTO dto) {
        return messageNotificationService.sendMessages(dto);
    }

    /**
     * 校医/超管查看自己发送过的消息。
     */
    @GetMapping("/sent/page")
    @PreAuthorize("hasAuthority('message:send')")
    public IPage<MessageVO> sentPage(@Valid MessageQueryDTO query) {
        return messageNotificationService.pageSent(query);
    }

    /**
     * 发送关怀留言时搜索启用的学生接收人。
     */
    @GetMapping("/receiver/page")
    @PreAuthorize("hasAuthority('message:send')")
    public IPage<MessageReceiverVO> receiverPage(@Valid MessageReceiverQueryDTO query) {
        return messageNotificationService.pageReceivers(query);
    }

    /**
     * 超级管理员手动触发指定日期的健康预警，便于演示和补跑。
     */
    @PostMapping("/warning/run")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public WarningRunResultVO runWarning(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return messageWarningService.run(date);
    }
}
