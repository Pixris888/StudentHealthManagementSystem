package com.health.management.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 科普文章展示对象。
 * 列表接口不返回正文，详情接口返回完整内容。
 */
@Data
public class HealthArticleVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long categoryId;
    private String categoryName;
    private String title;
    private String coverUrl;
    private String author;
    private String summary;
    private String content;
    private String source;
    private String docUrl;
    private Integer viewCount;
    private Integer likeCount;
    private Integer status;
    private String statusName;
    private Integer auditStatus;
    private String auditStatusName;
    private Long auditBy;
    private LocalDateTime auditTime;
    private String auditRemark;
    private Long publishBy;
    private LocalDateTime publishTime;
    private Long createBy;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String remark;
}
