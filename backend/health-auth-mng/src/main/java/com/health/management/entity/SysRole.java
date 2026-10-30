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
@TableName("sys_role")
public class SysRole extends BaseEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("role_code")
    private String roleCode; // 角色编码(唯一)
    @TableField("role_name")
    private String roleName; // 角色名称
    @TableField("user_type")
    private Integer userType; // 用户类型 1-学生 2-校医科普管理员 3-超级管理员
    @TableField("role_sort")
    private Integer roleSort; // 显示顺序
    @TableField("data_scope")
    private Integer dataScope; // 数据权限范围 1-全部 2-自定义 3-仅本人 4-部门
    @TableField("status")
    private Integer status; // 状态 0-停用 1-正常
}
