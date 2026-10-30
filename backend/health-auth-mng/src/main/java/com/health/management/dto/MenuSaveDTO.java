package com.health.management.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 新增/修改菜单请求
 */
@Data
public class MenuSaveDTO implements Serializable {

    private Long id;

    @NotBlank(message = "菜单名称不能为空")
    @Size(max = 50, message = "菜单名称长度不能超过50个字符")
    private String menuName;

    /**
     * 0 表示根节点
     */
    private Long parentId;

    @NotNull(message = "菜单类型不能为空")
    private Integer menuType;

    private Integer orderNum;

    @Size(max = 100, message = "路由地址长度不能超过100个字符")
    private String path;

    @Size(max = 100, message = "组件路径长度不能超过100个字符")
    private String component;

    @Size(max = 100, message = "权限标识长度不能超过100个字符")
    private String perms;

    @Size(max = 50, message = "图标长度不能超过50个字符")
    private String icon;

    private Integer status;

    @Size(max = 500, message = "备注长度不能超过500个字符")
    private String remark;
}
