package com.health.management.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI 健康助手配置（application.yml 的 health.ai 节点）。
 *
 * 主模型默认走阿里百炼 OpenAI 兼容接口；Ollama 作为本地备用模型。
 * API Key 不写死在代码或提交到仓库，通过 DASHSCOPE_API_KEY 环境变量注入。
 */
@Data
@Component
@ConfigurationProperties(prefix = "health.ai")
public class AiProperties {

    /** 是否启用 AI；关闭时使用 Mock 模式，保证接口可联调 */
    private boolean enabled = true;

    /** 主模型提供方：bailian / ollama / mock */
    private String provider = "bailian";

    /** OpenAI 兼容接口地址 */
    private String baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";

    /** 百炼 API Key，通过 DASHSCOPE_API_KEY 环境变量注入 */
    private String apiKey = "";

    /** 主模型名称，以百炼控制台当前可用模型为准 */
    private String model = "qwen-plus";

    /** 采样温度 */
    private Double temperature = 0.4;

    /** 是否开启百炼思考模式；思考模型建议 false，避免 content 为空 */
    private Boolean enableThinking = Boolean.FALSE;

    /** 单次最大输出 token */
    private Integer maxTokens = 1200;

    /** 主模型超时时间（秒） */
    private Integer timeoutSeconds = 45;

    /** 每个学生每天问答次数上限 */
    private Integer dailyLimit = 30;

    /** 每个学生每天计划生成次数上限 */
    private Integer planDailyLimit = 10;

    /** 单次问题最大长度 */
    private Integer maxInputLength = 500;

    /** 知识素材召回数量 */
    private Integer retrieveLimit = 4;

    /** 多轮对话携带的历史问答条数 */
    private Integer historyRounds = 3;

    /** SSE 流式开关；关闭后 /ask/stream 返回业务错误，前端应回退到同步接口 */
    private Boolean streamEnabled = true;

    /** SSE 连接超时时间（秒），应大于主模型 + 备用模型的最长耗时 */
    private Integer streamTimeoutSeconds = 180;

    /** SSE 心跳间隔（秒），用于等待首字期间保活和探测断连；<=0 关闭心跳 */
    private Integer streamHeartbeatSeconds = 15;

    /** AI 流式线程池核心线程数 */
    private Integer streamCorePoolSize = 4;

    /** AI 流式线程池最大线程数 */
    private Integer streamMaxPoolSize = 8;

    /** AI 流式线程池队列容量 */
    private Integer streamQueueCapacity = 50;

    /** 本地 Ollama 备用模型 */
    private Fallback fallback = new Fallback();

    @Data
    public static class Fallback {

        /** 是否启用本地备用模型 */
        private boolean enabled = true;

        /** Ollama 原生接口地址（客户端会自动追加 /api/chat） */
        private String baseUrl = "http://localhost:11434";

        /** 仅 OpenAI 兼容模式使用；原生 Ollama 不校验 Key */
        private String apiKey = "ollama";

        /** 本地模型名称；默认使用本机已装的 0.6b 保证可跑通，建议换成 qwen3:4b / qwen2.5:7b */
        private String model = "qwen3:0.6b";

        /** 本地模型超时时间（秒） */
        private Integer timeoutSeconds = 120;

        /** 本地模型单次最大输出 token */
        private Integer maxTokens = 800;
    }
}
