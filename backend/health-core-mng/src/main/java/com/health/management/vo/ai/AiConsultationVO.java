package com.health.management.vo.ai;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI 问答记录展示对象。
 */
@Data
public class AiConsultationVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long userId;

    /** 管理端展示，校医查看时脱敏 */
    private String studentNo;

    /** 管理端展示，校医查看时脱敏 */
    private String realName;

    private String sessionId;

    private String question;

    private String answer;

    private String knowledgeSource;

    private String modelName;

    private String provider;

    private Integer answerStatus;

    private String errorMsg;

    private Integer promptTokens;

    private Integer completionTokens;

    private Integer latencyMs;

    private Integer feedback;

    private String feedbackRemark;

    private String retrievedIds;

    private Integer emergency;

    private Boolean fallback;

    private LocalDateTime consultTime;

    private LocalDateTime createTime;
}
