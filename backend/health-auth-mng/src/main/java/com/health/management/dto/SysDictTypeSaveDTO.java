package com.health.management.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 字典类型新增/编辑请求。
 */
@Data
public class SysDictTypeSaveDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 编辑时必填 */
    private Long id;

    @NotBlank(message = "字典名称不能为空")
    @Size(max = 100, message = "字典名称长度不能超过100个字符")
    private String dictName;

    @NotBlank(message = "字典类型不能为空")
    @Size(max = 100, message = "字典类型长度不能超过100个字符")
    @Pattern(regexp = "^[a-z][a-z0-9_]{1,99}$",
            message = "字典类型必须为小写字母开头，只能包含小写字母、数字、下划线")
    private String dictType;

    @Min(value = 0, message = "状态只能是0或1")
    @Max(value = 1, message = "状态只能是0或1")
    private Integer status;

    @Size(max = 500, message = "备注长度不能超过500个字符")
    private String remark;
}
