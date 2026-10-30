package com.health.management.vo.ai;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * SSE 流式回答的首个事件：先告诉前端会话、记录 ID 和命中的知识来源。
 * 这些信息不依赖模型输出，可以在等待首字时先渲染出来。
 */
@Data
public class AiStreamMetaVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long consultationId;

    private String sessionId;

    private String knowledgeSource;

    /** 命中的知识素材来源，用于展示“参考来源” */
    private List<String> sources = new ArrayList<>();

    /** 是否紧急问题 */
    private Boolean emergency;

    private LocalDateTime consultTime;
}
