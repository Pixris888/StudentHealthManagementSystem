package com.health.management.client.ai;

/**
 * 流式输出被取消（通常是前端断开连接）。
 * 模型客户端不要把该异常包装成普通调用失败，否则会触发不必要的降级。
 */
public class StreamCancelledException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public StreamCancelledException(String message) {
        super(message);
    }

    public StreamCancelledException(String message, Throwable cause) {
        super(message, cause);
    }
}
