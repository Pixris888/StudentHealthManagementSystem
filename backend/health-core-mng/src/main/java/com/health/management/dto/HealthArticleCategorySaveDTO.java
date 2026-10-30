package com.health.management.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 科普文章分类新增/编辑请求。
 */
@Data
public class HealthArticleCategorySaveDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "分类名称不能为空")
    @Size(max = 50, message = "分类名称长度不能超过50个字符")
    private String categoryName;

    @Min(value = 0, message = "父分类ID不能小于0")
    private Long parentId = 0L;

    @Min(value = 0, message = "显示顺序不能小于0")
    @Max(value = 9999, message = "显示顺序不能超过9999")
    private Integer orderNum = 0;

    @Min(value = 0, message = "状态只能是0-停用、1-启用")
    @Max(value = 1, message = "状态只能是0-停用、1-启用")
    private Integer status = 1;

    @Size(max = 500, message = "备注长度不能超过500个字符")
    private String remark;
}
