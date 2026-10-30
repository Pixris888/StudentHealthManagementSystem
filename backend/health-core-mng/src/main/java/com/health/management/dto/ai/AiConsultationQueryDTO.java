package com.health.management.dto.ai;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * AI 问答记录分页查询条件（个人查询与管理端查询共用）。
 */
@Data
public class AiConsultationQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Min(value = 1, message = "页码必须大于0")
    private Integer page = 1;

    @Min(value = 1, message = "每页数量必须大于0")
    @Max(value = 100, message = "每页数量不能超过100")
    private Integer size = 10;

    @Size(max = 100, message = "关键词长度不能超过100个字符")
    private String keyword;

    @Min(value = 0, message = "回答状态只能是0-3")
    @Max(value = 3, message = "回答状态只能是0-3")
    private Integer answerStatus;

    @Min(value = 0, message = "反馈状态只能是0-2")
    @Max(value = 2, message = "反馈状态只能是0-2")
    private Integer feedback;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    /**
     * 仅校医/超级管理员查询时生效。
     */
    private Long userId;
}
