package com.health.management.vo.ai;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * AI 问答回答结果。
 */
@Data
public class AiAnswerVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long consultationId;

    private String sessionId;

    private String answer;

    /** 命中的知识素材来源，用于前端展示引用 */
    private List<String> sources = new ArrayList<>();

    private String knowledgeSource;

    private String modelName;

    private String provider;

    private Integer answerStatus;

    /** 是否使用了备用模型/规则兜底 */
    private Boolean fallback;

    /** 是否紧急问题 */
    private Boolean emergency;

    private LocalDateTime consultTime;
}
