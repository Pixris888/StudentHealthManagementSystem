package com.health.management.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 内容审核请求：超级管理员对科普文章或 AI 知识素材做合规审核。
 */
@Data
public class ContentAuditDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "请选择审核结果")
    private Boolean approved;

    @Size(max = 255, message = "审核意见长度不能超过255个字符")
    private String remark;
}
