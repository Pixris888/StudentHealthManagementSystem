package com.health.management.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * AI 问答知识素材分页查询条件。
 */
@Data
public class HealthKnowledgeMaterialQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Min(value = 1, message = "页码必须大于0")
    private Integer page = 1;

    @Min(value = 1, message = "每页数量必须大于0")
    @Max(value = 100, message = "每页数量不能超过100")
    private Integer size = 10;

    @Min(value = 1, message = "分类ID必须大于0")
    private Long categoryId;

    @Size(max = 100, message = "搜索关键词长度不能超过100个字符")
    private String keyword;

    @Min(value = 0, message = "状态只能是0-停用、1-启用")
    @Max(value = 1, message = "状态只能是0-停用、1-启用")
    private Integer status;

    @Min(value = 0, message = "审核状态只能是0-待审核、1-通过、2-不通过")
    @Max(value = 2, message = "审核状态只能是0-待审核、1-通过、2-不通过")
    private Integer auditStatus;
}
