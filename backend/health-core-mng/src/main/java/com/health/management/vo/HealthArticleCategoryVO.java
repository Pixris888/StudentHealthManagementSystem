package com.health.management.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 科普文章分类展示对象，可组装为树形结构。
 */
@Data
public class HealthArticleCategoryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String categoryName;
    private Long parentId;
    private Integer orderNum;
    private Integer status;
    private String statusName;
    /** 文章数量：学生端只统计已发布文章，管理端统计全部未删除文章 */
    private Long articleCount;
    private List<HealthArticleCategoryVO> children;
    private LocalDateTime createTime;
    private String remark;
}
