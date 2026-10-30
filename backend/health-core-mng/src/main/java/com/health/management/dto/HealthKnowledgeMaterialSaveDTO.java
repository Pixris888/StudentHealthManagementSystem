package com.health.management.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * AI 问答知识素材新增/编辑请求。
 * 素材用于后续 AI 健康助手的权威知识召回，需审核通过并启用后才可被使用。
 */
@Data
public class HealthKnowledgeMaterialSaveDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "请选择素材分类")
    private Long categoryId;

    @NotBlank(message = "素材标题不能为空")
    @Size(max = 200, message = "素材标题长度不能超过200个字符")
    private String title;

    @NotBlank(message = "素材内容不能为空")
    private String content;

    @Size(max = 255, message = "关键词长度不能超过255个字符")
    private String keyword;

    @Size(max = 100, message = "素材来源长度不能超过100个字符")
    private String source;

    @Min(value = 0, message = "状态只能是0-停用、1-启用")
    @Max(value = 1, message = "状态只能是0-停用、1-启用")
    private Integer status = 1;

    @Size(max = 500, message = "备注长度不能超过500个字符")
    private String remark;
}
