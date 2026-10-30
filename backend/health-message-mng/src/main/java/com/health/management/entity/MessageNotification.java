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
@TableName("message_notification")
public class MessageNotification extends BaseEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("receiver_id")
    private Long receiverId; // 接收人ID(0 表示全体广播)
    @TableField("message_type")
    private Integer messageType; // 消息类型 1-熬夜预警 2-运动不足预警 3-校园公告 4-关怀留言 5-其他
    @TableField("title")
    private String title; // 消息标题
    @TableField("content")
    private String content; // 消息内容
    @TableField("sender_id")
    private Long senderId; // 发送人ID(0=系统自动推送)
    @TableField("send_type")
    private Integer sendType; // 发送方式 1-个人 2-全体 3-按条件
    @TableField("publish_time")
    private LocalDateTime publishTime; // 发布时间
    @TableField("dedup_key")
    private String dedupKey; // 幂等键：warning:{type}:{userId}:{date}，非预警消息为 null

    @TableField(exist = false)
    private Integer readStatus; // 当前用户是否已读 0-未读 1-已读（非持久化，仅用于展示）
}
