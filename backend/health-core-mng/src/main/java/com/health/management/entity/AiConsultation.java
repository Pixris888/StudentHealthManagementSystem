package com.health.management.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.IdType;
import com.health.management.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("ai_consultation")
public class AiConsultation extends BaseEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("user_id")
    private Long userId; // 用户ID(逻辑关联 sys_user.id)
    @TableField("session_id")
    private String sessionId; // 会话ID(多轮问答分组)
    @TableField("question")
    private String question; // 用户提问
    @TableField("answer")
    private String answer; // AI回答内容
    @TableField("answer_status")
    private Integer answerStatus; // 回答状态 0-处理中 1-成功 2-失败 3-降级
    @TableField("error_msg")
    private String errorMsg; // 失败/降级原因
    @TableField("knowledge_source")
    private String knowledgeSource; // 知识来源(权威知识库)
    @TableField("retrieved_ids")
    private String retrievedIds; // 命中的知识素材ID(逗号分隔)
    @TableField("model_name")
    private String modelName; // 使用模型
    @TableField("provider")
    private String provider; // 模型提供方 bailian/ollama/rule/mock
    @TableField("prompt_tokens")
    private Integer promptTokens; // 输入token数
    @TableField("completion_tokens")
    private Integer completionTokens; // 输出token数
    @TableField("latency_ms")
    private Integer latencyMs; // 响应耗时(毫秒)
    @TableField("feedback")
    private Integer feedback; // 反馈 0-未评价 1-有帮助 2-没帮助
    @TableField("feedback_remark")
    private String feedbackRemark; // 反馈备注
    @TableField("emergency")
    private Integer emergency; // 是否紧急问题 0-否 1-是
    @TableField("consult_time")
    private LocalDateTime consultTime; // 咨询时间
}
