package com.health.management.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.IdType;
import com.health.management.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("message_read")
public class MessageRead extends BaseEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("message_id")
    private Long messageId; // 消息ID(逻辑关联 message_notification.id)
    @TableField("user_id")
    private Long userId; // 用户ID(逻辑关联 sys_user.id)
    @TableField("read_time")
    private LocalDateTime readTime; // 阅读时间
}
