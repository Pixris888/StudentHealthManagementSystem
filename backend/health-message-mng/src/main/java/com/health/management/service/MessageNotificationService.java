package com.health.management.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.health.management.dto.MessageQueryDTO;
import com.health.management.dto.MessageReceiverQueryDTO;
import com.health.management.dto.MessageSendDTO;
import com.health.management.entity.MessageNotification;
import com.health.management.vo.MessageReceiverVO;
import com.health.management.vo.MessageVO;

/**
 * 消息收件箱、发送与已发送列表。
 */
public interface MessageNotificationService extends IService<MessageNotification> {

    /** 当前用户收件箱分页（个人消息 + 全体广播） */
    IPage<MessageVO> pageMine(MessageQueryDTO query);

    /** 当前用户查看消息详情 */
    MessageVO getMyDetail(Long id);

    /** 校医/超管发送公告或关怀留言，返回实际生成的消息条数 */
    int sendMessages(MessageSendDTO dto);

    /** 校医/超管查看自己发送过的消息 */
    IPage<MessageVO> pageSent(MessageQueryDTO query);

    /** 发送关怀留言时搜索启用的学生接收人 */
    IPage<MessageReceiverVO> pageReceivers(MessageReceiverQueryDTO query);

    /** 判断消息对指定用户是否可见 */
    boolean isVisibleToUser(MessageNotification message, Long userId);
}
