package com.health.management.dto.ai;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * AI 健康问答请求。
 */
@Data
public class AiAskDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "问题不能为空")
    @Size(max = 500, message = "问题长度不能超过500个字符")
    private String question;

    /**
     * 多轮会话 ID；为空时后端自动生成。
     */
    @Size(max = 64, message = "会话ID长度不能超过64个字符")
    private String sessionId;
}
