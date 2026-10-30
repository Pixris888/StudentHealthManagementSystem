package com.health.management.dto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.io.Serializable;
import java.util.List;

/**
 * 分配角色/菜单的ID列表
 */
@Data
public class AssignIdsDTO implements Serializable {

    @NotEmpty(message = "ID列表不能为空")
    private List<Long> ids;
}
