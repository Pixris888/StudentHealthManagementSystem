package com.health.management.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.IdType;
import com.health.management.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;


@Data
@EqualsAndHashCode(callSuper = false)
@TableName("health_checkin_photo")
public class HealthCheckinPhoto extends BaseEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("checkin_id")
    private Long checkinId; // 打卡记录ID(逻辑关联 health_checkin.id)
    @TableField("user_id")
    private Long userId; // 用户ID(逻辑关联 sys_user.id)
    @TableField("photo_url")
    private String photoUrl; // 照片 objectKey（本地路径或 OSS objectKey），展示时生成访问 URL
    @TableField("photo_type")
    private Integer photoType; // 照片类型 1-睡眠 2-运动 3-饮食 4-其他
}
