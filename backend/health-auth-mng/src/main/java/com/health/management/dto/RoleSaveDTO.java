package com.health.management.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.List;

/**
 * 新增/修改角色请求
 */
@Data
public class RoleSaveDTO implements Serializable {

    /**
     * 新增时为空，修改时必填
     */
    private Long id;

    @NotBlank(message = "角色编码不能为空")
    @Size(max = 50, message = "角色编码长度不能超过50个字符")
    private String roleCode;

    @NotBlank(message = "角色名称不能为空")
    @Size(max = 50, message = "角色名称长度不能超过50个字符")
    private String roleName;

    @NotNull(message = "用户类型不能为空")
    private Integer userType;

    private Integer roleSort;

    private Integer dataScope;

    private Integer status;

    /**
     * 新增时可为空；修改时为空表示不改动已有菜单授权
     */
    private List<Long> menuIds;

    @Size(max = 500, message = "备注长度不能超过500个字符")
    private String remark;
}
