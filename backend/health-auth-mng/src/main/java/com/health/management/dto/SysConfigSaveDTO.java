package com.health.management.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 系统参数新增/编辑请求。
 */
@Data
public class SysConfigSaveDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 编辑时必填 */
    private Long id;

    @NotBlank(message = "参数名称不能为空")
    @Size(max = 100, message = "参数名称长度不能超过100个字符")
    private String configName;

    @NotBlank(message = "参数键名不能为空")
    @Size(max = 100, message = "参数键名长度不能超过100个字符")
    @Pattern(regexp = "^[A-Za-z][A-Za-z0-9._-]{2,99}$",
            message = "参数键名必须以字母开头，只能包含字母、数字、点、下划线、短横线")
    private String configKey;

    @Size(max = 500, message = "参数值长度不能超过500个字符")
    private String configValue;

    @Min(value = 0, message = "参数类型只能是0-系统内置、1-自定义")
    @Max(value = 1, message = "参数类型只能是0-系统内置、1-自定义")
    private Integer configType;

    @Size(max = 500, message = "备注长度不能超过500个字符")
    private String remark;
}
