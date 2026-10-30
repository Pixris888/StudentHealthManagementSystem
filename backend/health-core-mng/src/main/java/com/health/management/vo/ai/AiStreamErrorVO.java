package com.health.management.vo.ai;

import lombok.Data;

import java.io.Serializable;

/**
 * SSE 流式回答的错误事件。
 */
@Data
public class AiStreamErrorVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer code;

    private String message;

    /** 已创建的问答记录 ID；参数校验/限流失败时可能为空 */
    private Long consultationId;

    /** 是否已经输出过部分内容；前端应保留已渲染内容并提示重试 */
    private Boolean partial;
}
