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
@TableName("health_article_category")
public class HealthArticleCategory extends BaseEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("category_name")
    private String categoryName; // 分类名称
    @TableField("parent_id")
    private Long parentId; // 父分类ID
    @TableField("order_num")
    private Integer orderNum; // 显示顺序
    @TableField("status")
    private Integer status; // 状态 0-停用 1-正常
}
