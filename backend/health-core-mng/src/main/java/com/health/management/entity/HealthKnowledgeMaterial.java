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
@TableName("health_knowledge_material")
public class HealthKnowledgeMaterial extends BaseEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("category_id")
    private Long categoryId; // 分类ID
    @TableField("title")
    private String title; // 素材标题
    @TableField("content")
    private String content; // 素材内容
    @TableField("keyword")
    private String keyword; // 关键词(逗号分隔)
    @TableField("source")
    private String source; // 来源
    @TableField("status")
    private Integer status; // 状态 0-停用 1-启用
    @TableField("audit_status")
    private Integer auditStatus; // 审核状态 0-待审核 1-通过 2-不通过
    @TableField("audit_by")
    private Long auditBy; // 审核人(逻辑关联 sys_user.id)
    @TableField("audit_time")
    private LocalDateTime auditTime; // 审核时间
    @TableField("audit_remark")
    private String auditRemark; // 审核意见
}
