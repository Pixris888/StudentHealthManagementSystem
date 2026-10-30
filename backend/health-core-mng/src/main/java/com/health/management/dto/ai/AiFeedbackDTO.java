package com.health.management.dto.ai;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * AI 回答反馈。
 */
@Data
public class AiFeedbackDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "反馈结果不能为空")
    private Boolean helpful;

    @Size(max = 255, message = "反馈备注长度不能超过255个字符")
    private String remark;
}
