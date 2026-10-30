package com.health.management.common.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 菜单树（前端路由用）
 */
@Data
public class MenuTree implements Serializable {

    private Long id;
    private Long parentId;
    private String menuName;
    private String path;
    private String component;
    private String perms;
    private String icon;
    private Integer menuType;
    private Integer orderNum;
    private Integer status;
    private List<MenuTree> children = new ArrayList<>();
}
