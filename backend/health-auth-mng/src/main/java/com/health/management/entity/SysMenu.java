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
@TableName("sys_menu")
public class SysMenu extends BaseEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("menu_name")
    private String menuName; // 菜单名称
    @TableField("parent_id")
    private Long parentId; // 父菜单ID
    @TableField("menu_type")
    private Integer menuType; // 菜单类型 1-目录 2-菜单 3-按钮
    @TableField("order_num")
    private Integer orderNum; // 显示顺序
    @TableField("path")
    private String path; // 路由地址
    @TableField("component")
    private String component; // 组件路径
    @TableField("perms")
    private String perms; // 权限标识(如 system:user:list)
    @TableField("icon")
    private String icon; // 菜单图标
    @TableField("status")
    private Integer status; // 状态 0-停用 1-正常
}
