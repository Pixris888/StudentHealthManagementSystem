package com.health.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.health.management.entity.MessageRead;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface MessageReadMapper extends BaseMapper<MessageRead> {

    /**
     * 写入单条已读记录；唯一键保证重复点击不会产生多条记录。
     */
    int insertIgnore(@Param("messageId") Long messageId,
                     @Param("userId") Long userId,
                     @Param("readTime") LocalDateTime readTime);

    /**
     * 批量写已读记录。
     */
    int insertIgnoreBatch(@Param("userId") Long userId,
                          @Param("messageIds") List<Long> messageIds,
                          @Param("readTime") LocalDateTime readTime);
}
