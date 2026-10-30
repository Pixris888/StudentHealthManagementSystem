package com.health.management.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 打卡照片展示对象。photoUrl 为可直接访问的 URL（OSS 为临时签名 URL）。
 */
@Data
public class HealthCheckinPhotoVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long checkinId;
    private String photoUrl;
    private Integer photoType;
    private String photoTypeName;
    private LocalDateTime createTime;
}
