package com.health.management.controller.ai;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.health.management.client.ai.StreamCancelledException;
import com.health.management.common.enums.ResultCode;
import com.health.management.common.exception.ServiceException;
import com.health.management.common.util.ValidateUtils;
import com.health.management.config.AiProperties;
import com.health.management.dto.ai.AiAskDTO;
import com.health.management.dto.ai.AiConsultationQueryDTO;
import com.health.management.dto.ai.AiFeedbackDTO;
import com.health.management.security.SecurityUtils;
import com.health.management.service.AiConsultationService;
import com.health.management.vo.ai.AiAnswerVO;
import com.health.management.vo.ai.AiConsultationVO;
import com.health.management.vo.ai.AiStatsVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.validation.Valid;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

/**
 * AI 健康问答接口。
 * 学生接口只操作本人数据；管理端接口仅校医/超管可用。
 */
@RestController
@RequestMapping("/api/ai/consultation")
public class AiConsultationController {

    private static final Logger log = LoggerFactory.getLogger(AiConsultationController.class);

    @Autowired
    private AiConsultationService aiConsultationService;

    @Autowired
    private AiProperties aiProperties;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    @Qualifier("aiStreamExecutor")
    private ThreadPoolTaskExecutor aiStreamExecutor;

    @Autowired
    @Qualifier("aiStreamHeartbeatScheduler")
    private ScheduledExecutorService aiStreamHeartbeatScheduler;

    /**
     * 学生提问（同步）。
     */
    @PostMapping("/ask")
    public AiAnswerVO ask(@Valid @RequestBody AiAskDTO dto) {
        return aiConsultationService.ask(dto);
    }

    /**
     * 学生提问（SSE 流式）。
     *
     * 使用 POST 而不是 EventSource：JWT 需要放在 Authorization 头里，
     * 前端用 fetch + ReadableStream 读取 text/event-stream。
     * 事件顺序：meta -> delta* -> done；异常时为 error。
     */
    @PostMapping("/ask/stream")
    public SseEmitter askStream(@Valid @RequestBody AiAskDTO dto) {
        if (!Boolean.TRUE.equals(aiProperties.getStreamEnabled())) {
            throw ServiceException.badRequest("流式输出未启用，请使用同步问答接口");
        }
        // SecurityContext 不会自动传到异步线程，必须在请求线程先取 userId
        Long userId = SecurityUtils.getUserId();

        long timeoutMillis = (aiProperties.getStreamTimeoutSeconds() == null
                ? 180 : Math.max(30, aiProperties.getStreamTimeoutSeconds())) * 1000L;
        long heartbeatMillis = aiProperties.getStreamHeartbeatSeconds() == null
                ? 15_000L : Math.max(0, aiProperties.getStreamHeartbeatSeconds()) * 1000L;

        AiSseStreamSink sink = new AiSseStreamSink(
                timeoutMillis, heartbeatMillis, aiStreamHeartbeatScheduler, objectMapper);
        try {
            aiStreamExecutor.execute(() -> {
                try {
                    aiConsultationService.askStream(dto, userId, sink);
                } catch (StreamCancelledException e) {
                    log.debug("AI 流式问答已取消：{}", e.getMessage());
                } catch (ServiceException e) {
                    sink.sendError(e.getCode(), e.getMessage(), null, false);
                } catch (Exception e) {
                    log.error("AI 流式问答异常", e);
                    sink.sendError(ResultCode.SYSTEM_ERROR.getCode(),
                            "系统内部错误，请稍后重试", null, false);
                } finally {
                    // 业务层正常会发送 done/error；这里兜底，避免异常路径连接一直挂起
                    sink.complete();
                }
            });
        } catch (TaskRejectedException e) {
            log.warn("AI 流式线程池已满，拒绝请求");
            sink.sendError(ResultCode.FAIL.getCode(), "AI 服务繁忙，请稍后重试", null, false);
        }
        return sink.getEmitter();
    }

    /**
     * 当前学生问答历史。
     */
    @GetMapping("/mine/page")
    public IPage<AiConsultationVO> pageMine(@Valid AiConsultationQueryDTO query) {
        return aiConsultationService.pageMine(query);
    }

    /**
     * 当前学生某个会话的全部消息。
     */
    @GetMapping("/session/{sessionId}")
    public List<AiConsultationVO> listSession(@PathVariable String sessionId) {
        return aiConsultationService.listSession(sessionId);
    }

    /**
     * 当前学生问答详情。
     */
    @GetMapping("/{id}")
    public AiConsultationVO getMine(@PathVariable Long id) {
        return aiConsultationService.getMine(id);
    }

    /**
     * 当前学生删除问答记录。
     */
    @DeleteMapping("/{ids}")
    public boolean deleteMine(@PathVariable String ids) {
        return aiConsultationService.deleteMine(ValidateUtils.parseIds(ids));
    }

    /**
     * 学生对回答反馈。
     */
    @PostMapping("/{id}/feedback")
    public AiConsultationVO feedback(@PathVariable Long id,
                                     @Valid @RequestBody AiFeedbackDTO dto) {
        return aiConsultationService.feedback(id, dto);
    }

    /**
     * 校医/超管分页查询问答记录。
     */
    @GetMapping("/manage/page")
    @PreAuthorize("hasAnyRole('DOCTOR_ADMIN','SUPER_ADMIN')")
    public IPage<AiConsultationVO> pageAll(@Valid AiConsultationQueryDTO query) {
        return aiConsultationService.pageAll(query);
    }

    /**
     * 校医/超管问答详情。
     */
    @GetMapping("/manage/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR_ADMIN','SUPER_ADMIN')")
    public AiConsultationVO detail(@PathVariable Long id) {
        return aiConsultationService.getDetail(id);
    }

    /**
     * 校医/超管使用统计。
     */
    @GetMapping("/manage/stats")
    @PreAuthorize("hasAnyRole('DOCTOR_ADMIN','SUPER_ADMIN')")
    public AiStatsVO stats(@RequestParam(defaultValue = "7") Integer days) {
        return aiConsultationService.stats(days);
    }

    /**
     * 校医/超管查看未命中/差评问题。
     */
    @GetMapping("/manage/unanswered")
    @PreAuthorize("hasAnyRole('DOCTOR_ADMIN','SUPER_ADMIN')")
    public List<AiConsultationVO> unanswered(@RequestParam(defaultValue = "20") Integer limit) {
        return aiConsultationService.listUnanswered(limit);
    }
}
