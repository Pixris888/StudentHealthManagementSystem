package com.health.management.client.ai;

/**
 * 大模型调用异常。业务层捕获后走本地模型或规则兜底。
 */
public class LlmException extends RuntimeException {

    public LlmException(String message) {
        super(message);
    }

    public LlmException(String message, Throwable cause) {
        super(message, cause);
    }
}
