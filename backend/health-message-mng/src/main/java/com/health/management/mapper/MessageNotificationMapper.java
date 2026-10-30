package com.health.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.health.management.dto.MessageQueryDTO;
import com.health.management.entity.MessageNotification;
import com.health.management.vo.MessageVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MessageNotificationMapper extends BaseMapper<MessageNotification> {

    /**
     * 收件箱分页：个人消息 + 全体广播，并关联当前用户的已读状态。
     */
    IPage<MessageVO> selectInboxPage(Page<MessageVO> page,
                                     @Param("userId") Long userId,
                                     @Param("query") MessageQueryDTO query);

    /**
     * 当前用户未读消息数。
     */
    long countUnread(@Param("userId") Long userId);

    /**
     * 当前用户所有未读消息ID，用于“全部已读”批量写入。
     */
    List<Long> selectUnreadIds(@Param("userId") Long userId);

    /**
     * 预警消息批量写入，利用 dedup_key 唯一索引实现 INSERT IGNORE 幂等。
     */
    int insertIgnoreBatch(@Param("list") List<MessageNotification> list);
}
