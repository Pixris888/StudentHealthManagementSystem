package com.health.management.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI 问答知识素材展示对象。
 */
@Data
public class HealthKnowledgeMaterialVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long categoryId;
    private String categoryName;
    private String title;
    private String content;
    private String keyword;
    private String source;
    private Integer status;
    private String statusName;
    private Integer auditStatus;
    private String auditStatusName;
    private Long auditBy;
    private LocalDateTime auditTime;
    private String auditRemark;
    private Long createBy;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String remark;
}
