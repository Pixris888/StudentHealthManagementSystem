package com.health.management.service.ai;

import com.health.management.vo.ai.AiAnswerVO;
import com.health.management.vo.ai.AiStreamMetaVO;

/**
 * 流式输出出口。
 *
 * 业务层只依赖该接口，不感知 SseEmitter/HTTP；单元测试可以用内存实现捕获事件。
 * sendMeta / sendDelta 在客户端断开时应抛出
 * {@link com.health.management.client.ai.StreamCancelledException}，
 * 以便中断模型读取；sendDone / sendError 是终止事件，实现层负责结束响应。
 */
public interface AiStreamSink {

    void sendMeta(AiStreamMetaVO meta);

    void sendDelta(String content);

    void sendDone(AiAnswerVO answer);

    void sendError(Integer code, String message, Long consultationId, boolean partial);

    boolean isCancelled();
}
