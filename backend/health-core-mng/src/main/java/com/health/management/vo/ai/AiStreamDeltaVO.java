package com.health.management.vo.ai;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * SSE 流式回答的增量事件。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiStreamDeltaVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 本次增量文本，前端直接追加到当前回答末尾 */
    private String content;
}
