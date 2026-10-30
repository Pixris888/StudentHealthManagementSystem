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
@TableName("health_article")
public class HealthArticle extends BaseEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("category_id")
    private Long categoryId; // 分类ID(逻辑关联 health_article_category.id)
    @TableField("title")
    private String title; // 文章标题
    @TableField("cover_url")
    private String coverUrl; // 封面图
    @TableField("author")
    private String author; // 作者/发布人
    @TableField("summary")
    private String summary; // 摘要
    @TableField("content")
    private String content; // 正文内容
    @TableField("source")
    private String source; // 来源
    @TableField("doc_url")
    private String docUrl; // 官方文档附件地址
    @TableField("view_count")
    private Integer viewCount; // 浏览量
    @TableField("like_count")
    private Integer likeCount; // 点赞数
    @TableField("status")
    private Integer status; // 状态 0-草稿 1-已发布 2-已下架
    @TableField("audit_status")
    private Integer auditStatus; // 审核状态 0-待审核 1-通过 2-不通过(超级管理员审核)
    @TableField("audit_by")
    private Long auditBy; // 审核人(逻辑关联 sys_user.id)
    @TableField("audit_time")
    private LocalDateTime auditTime; // 审核时间
    @TableField("audit_remark")
    private String auditRemark; // 审核意见
    @TableField("publish_by")
    private Long publishBy; // 发布人
    @TableField("publish_time")
    private LocalDateTime publishTime; // 发布时间
}
