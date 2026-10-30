package com.health.management.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.health.management.client.ai.LlmClient;
import com.health.management.client.ai.LlmException;
import com.health.management.client.ai.LlmMessage;
import com.health.management.client.ai.LlmResponse;
import com.health.management.client.ai.StreamCancelledException;
import com.health.management.common.enums.ResultCode;
import com.health.management.common.exception.ServiceException;
import com.health.management.common.util.ValidateUtils;
import com.health.management.config.AiProperties;
import com.health.management.convert.AiConverter;
import com.health.management.dto.ai.AiAskDTO;
import com.health.management.dto.ai.AiConsultationQueryDTO;
import com.health.management.dto.ai.AiFeedbackDTO;
import com.health.management.entity.AiConsultation;
import com.health.management.entity.HealthKnowledgeMaterial;
import com.health.management.entity.SysUser;
import com.health.management.mapper.AiConsultationMapper;
import com.health.management.security.SecurityUtils;
import com.health.management.service.AiConsultationService;
import com.health.management.service.ai.AiContextBuilder;
import com.health.management.service.ai.AiFallbackService;
import com.health.management.service.ai.AiHealthContext;
import com.health.management.service.ai.AiKnowledgeRetriever;
import com.health.management.service.ai.AiPromptBuilder;
import com.health.management.service.ai.AiRateLimiter;
import com.health.management.service.ai.AiRetrievedMaterial;
import com.health.management.service.ai.AiSafetyGuard;
import com.health.management.service.ai.AiStreamSink;
import com.health.management.service.ai.AiUserInfoSupport;
import com.health.management.vo.ai.AiAnswerVO;
import com.health.management.vo.ai.AiConsultationVO;
import com.health.management.vo.ai.AiStatsVO;
import com.health.management.vo.ai.AiStreamMetaVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * AI 健康问答业务实现。
 */
@Service
public class AiConsultationServiceImpl extends ServiceImpl<AiConsultationMapper, AiConsultation>
        implements AiConsultationService {

    private static final Logger log = LoggerFactory.getLogger(AiConsultationServiceImpl.class);
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    @Autowired
    private LlmClient llmClient;

    @Autowired
    private AiProperties aiProperties;

    @Autowired
    private AiContextBuilder aiContextBuilder;

    @Autowired
    private AiKnowledgeRetriever aiKnowledgeRetriever;

    @Autowired
    private AiPromptBuilder aiPromptBuilder;

    @Autowired
    private AiSafetyGuard aiSafetyGuard;

    @Autowired
    private AiFallbackService aiFallbackService;

    @Autowired
    private AiRateLimiter aiRateLimiter;

    @Autowired
    private AiUserInfoSupport aiUserInfoSupport;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiAnswerVO ask(AiAskDTO dto) {
        String question = validateQuestion(dto);

        Long userId = SecurityUtils.getUserId();
        aiRateLimiter.checkConsult(userId);
        String sessionId = normalizeSessionId(dto.getSessionId());
        LocalDateTime now = LocalDateTime.now(ZONE);
        long start = System.currentTimeMillis();

        AiConsultation record = new AiConsultation();
        record.setUserId(userId);
        record.setSessionId(sessionId);
        record.setQuestion(question);
        record.setConsultTime(now);
        record.setAnswerStatus(0);
        record.setFeedback(0);
        record.setEmergency(0);
        record.setPromptTokens(0);
        record.setCompletionTokens(0);
        record.setLatencyMs(0);
        record.setKnowledgeSource("");
        record.setRetrievedIds("");
        record.setModelName("");
        record.setProvider("");

        // 紧急问题不进入模型，直接给出安全指引
        if (aiSafetyGuard.isEmergency(question)) {
            record.setAnswer(aiSafetyGuard.emergencyAnswer());
            record.setModelName("rule");
            record.setProvider("rule");
            record.setAnswerStatus(1);
            record.setEmergency(1);
            record.setLatencyMs((int) (System.currentTimeMillis() - start));
            if (!save(record)) {
                throw ServiceException.fail("AI问答记录保存失败");
            }
            return AiConverter.toAnswerVO(record, Collections.emptyList());
        }

        AiHealthContext context = aiContextBuilder.build(userId);
        List<AiRetrievedMaterial> materials = aiKnowledgeRetriever.retrieve(
                question, aiProperties.getRetrieveLimit());
        List<AiConsultation> history = loadHistory(userId, sessionId);
        List<LlmMessage> messages = aiPromptBuilder.buildQaMessages(
                question, context, materials, history);

        String answer;
        String provider;
        String modelName;
        int promptTokens = 0;
        int completionTokens = 0;
        boolean fallback = false;
        String errorMsg = null;
        String fallbackReason = null;

        try {
            LlmResponse response = llmClient.chat(messages);
            answer = response.getContent();
            provider = response.getProvider();
            modelName = response.getModel();
            promptTokens = response.getPromptTokens() == null ? 0 : response.getPromptTokens();
            completionTokens = response.getCompletionTokens() == null ? 0 : response.getCompletionTokens();
            fallbackReason = response.getFallbackReason();
            if (!ValidateUtils.hasText(answer)) {
                throw new LlmException("模型返回空答案");
            }
        } catch (Exception e) {
            log.warn("AI 问答调用失败，使用规则兜底：{}", e.getMessage());
            answer = aiFallbackService.buildAnswer(question, materials, context);
            provider = "rule";
            modelName = "rule";
            fallback = true;
            errorMsg = "模型调用失败，已使用规则兜底";
            if (ValidateUtils.hasText(e.getMessage())) {
                errorMsg = errorMsg + "：" + e.getMessage();
            }
            errorMsg = truncate(errorMsg, 500);
        }

        if (!fallback && isFallbackProvider(provider)) {
            fallback = true;
            errorMsg = ValidateUtils.hasText(fallbackReason)
                    ? truncate("主模型不可用，已使用备用模型；" + fallbackReason, 500)
                    : "主模型不可用，已使用备用模型";
        }

        answer = aiSafetyGuard.ensureDisclaimer(answer);
        record.setAnswer(answer);
        record.setKnowledgeSource(buildKnowledgeSource(materials));
        record.setRetrievedIds(buildRetrievedIds(materials));
        record.setModelName(truncate(modelName, 50));
        record.setProvider(truncate(provider, 20));
        record.setAnswerStatus(fallback ? 3 : 1);
        record.setErrorMsg(errorMsg);
        record.setPromptTokens(promptTokens);
        record.setCompletionTokens(completionTokens);
        record.setLatencyMs((int) (System.currentTimeMillis() - start));

        if (!save(record)) {
            throw ServiceException.fail("AI问答记录保存失败");
        }
        return AiConverter.toAnswerVO(record, buildSourceList(materials));
    }

    @Override
    public void askStream(AiAskDTO dto, Long userId, AiStreamSink sink) {
        ValidateUtils.requireNotNull(sink, "流式输出");
        long start = System.currentTimeMillis();
        AiConsultation record = null;
        StringBuilder answer = new StringBuilder();
        // 完成落库后即使客户端断开，也不要把成功记录覆盖成失败
        boolean finalized = false;

        try {
            String question = validateQuestion(dto);
            if (userId == null) {
                throw ServiceException.unauthorized("请先登录");
            }
            if (sink.isCancelled()) {
                return;
            }
            aiRateLimiter.checkConsult(userId);

            String sessionId = normalizeSessionId(dto.getSessionId());
            LocalDateTime consultTime = LocalDateTime.now(ZONE);

            // 紧急问题不进入模型，直接返回安全指引
            if (aiSafetyGuard.isEmergency(question)) {
                record = buildRecord(userId, sessionId, question, consultTime);
                String emergencyAnswer = aiSafetyGuard.emergencyAnswer();
                answer.append(emergencyAnswer);
                record.setAnswer(emergencyAnswer);
                record.setProvider("rule");
                record.setModelName("rule");
                record.setAnswerStatus(1);
                record.setEmergency(1);
                record.setLatencyMs((int) (System.currentTimeMillis() - start));
                saveRecordOrThrow(record);
                finalized = true;

                sink.sendMeta(buildMeta(record, Collections.emptyList(), true));
                sink.sendDelta(emergencyAnswer);
                sink.sendDone(AiConverter.toAnswerVO(record, Collections.emptyList()));
                return;
            }

            AiHealthContext context = aiContextBuilder.build(userId);
            List<AiRetrievedMaterial> materials = aiKnowledgeRetriever.retrieve(
                    question, aiProperties.getRetrieveLimit());
            List<AiConsultation> history = loadHistory(userId, sessionId);
            List<LlmMessage> messages = aiPromptBuilder.buildQaMessages(
                    question, context, materials, history);
            List<String> sources = buildSourceList(materials);

            if (sink.isCancelled()) {
                return;
            }

            record = buildRecord(userId, sessionId, question, consultTime);
            record.setKnowledgeSource(buildKnowledgeSource(materials));
            record.setRetrievedIds(buildRetrievedIds(materials));
            saveRecordOrThrow(record);
            sink.sendMeta(buildMeta(record, sources, false));

            String provider = "";
            String modelName = "";
            int promptTokens = 0;
            int completionTokens = 0;
            boolean fallback = false;
            String errorMsg = null;
            String fallbackReason = null;

            try {
                LlmResponse response = llmClient.streamChat(messages, delta -> {
                    if (!ValidateUtils.hasText(delta)) {
                        return;
                    }
                    answer.append(delta);
                    sink.sendDelta(delta);
                });
                if (response == null || !ValidateUtils.hasText(response.getContent())) {
                    throw new LlmException("模型返回空答案");
                }
                // 兜底处理“只返回结果、没有回调”的客户端
                if (answer.length() == 0) {
                    answer.append(response.getContent());
                    sink.sendDelta(response.getContent());
                }
                provider = response.getProvider();
                modelName = response.getModel();
                promptTokens = safeInt(response.getPromptTokens());
                completionTokens = safeInt(response.getCompletionTokens());
                fallbackReason = response.getFallbackReason();
            } catch (StreamCancelledException e) {
                throw e;
            } catch (Exception e) {
                if (answer.length() > 0) {
                    // 已经输出部分内容，不能再切换到其他模型拼接
                    String reason = "回答生成中断：" + safeMessage(e);
                    log.warn("AI 流式问答中断，已保留部分回答：{}", e.getMessage());
                    markRecordFailed(record, answer.toString(), reason, start);
                    safeSendError(sink, ResultCode.FAIL.getCode(),
                            "回答生成中断，请重试", record.getId(), true);
                    return;
                }
                // 首字之前失败：规则兜底，并一次性推送完整答案
                log.warn("AI 流式调用失败，使用规则兜底：{}", e.getMessage());
                String fallbackAnswer = aiFallbackService.buildAnswer(question, materials, context);
                answer.append(fallbackAnswer);
                provider = "rule";
                modelName = "rule";
                fallback = true;
                errorMsg = "模型调用失败，已使用规则兜底";
                if (ValidateUtils.hasText(e.getMessage())) {
                    errorMsg = errorMsg + "：" + e.getMessage();
                }
                errorMsg = truncate(errorMsg, 500);
                sink.sendDelta(fallbackAnswer);
            }

            if (!fallback && isFallbackProvider(provider)) {
                fallback = true;
                if (!ValidateUtils.hasText(errorMsg)) {
                    errorMsg = ValidateUtils.hasText(fallbackReason)
                            ? truncate("主模型不可用，已使用备用模型；" + fallbackReason, 500)
                            : "主模型不可用，已使用备用模型";
                }
            }

            String current = answer.toString();
            String safeAnswer = aiSafetyGuard.ensureDisclaimer(current);
            if (!safeAnswer.equals(current)) {
                // ensureDisclaimer 会 trim 并在末尾追加免责声明，这里把新增部分作为最后一个 delta
                String trimmedCurrent = current.trim();
                if (safeAnswer.startsWith(trimmedCurrent)) {
                    String disclaimer = safeAnswer.substring(trimmedCurrent.length());
                    if (!disclaimer.isEmpty()) {
                        answer.append(disclaimer);
                        sink.sendDelta(disclaimer);
                    }
                }
                answer.setLength(0);
                answer.append(safeAnswer);
            }

            record.setAnswer(safeAnswer);
            record.setProvider(truncate(provider, 20));
            record.setModelName(truncate(modelName, 50));
            record.setAnswerStatus(fallback ? 3 : 1);
            record.setErrorMsg(errorMsg);
            record.setPromptTokens(promptTokens);
            record.setCompletionTokens(completionTokens);
            record.setLatencyMs((int) (System.currentTimeMillis() - start));
            finalized = updateRecord(record);

            sink.sendDone(AiConverter.toAnswerVO(record, sources));
        } catch (StreamCancelledException e) {
            if (!finalized && record != null && record.getId() != null) {
                markRecordFailed(record, answer.toString(), "客户端断开连接", start);
            }
            log.debug("AI 流式问答已取消：{}", e.getMessage());
        } catch (ServiceException e) {
            if (!finalized && record != null && record.getId() != null) {
                markRecordFailed(record, answer.toString(), e.getMessage(), start);
            }
            safeSendError(sink, e.getCode(), e.getMessage(),
                    record == null ? null : record.getId(), answer.length() > 0);
        } catch (Exception e) {
            log.error("AI 流式问答异常", e);
            if (!finalized && record != null && record.getId() != null) {
                markRecordFailed(record, answer.toString(), "系统内部错误", start);
            }
            safeSendError(sink, ResultCode.SYSTEM_ERROR.getCode(),
                    "系统内部错误，请稍后重试",
                    record == null ? null : record.getId(), answer.length() > 0);
        }
    }

    @Override
    public IPage<AiConsultationVO> pageMine(AiConsultationQueryDTO query) {
        Long userId = SecurityUtils.getUserId();
        AiConsultationQueryDTO q = query == null ? new AiConsultationQueryDTO() : query;
        ValidateUtils.requirePage(q.getPage(), q.getSize());
        validateDateRange(q);

        int current = q.getPage() == null ? 1 : q.getPage();
        int size = q.getSize() == null ? 10 : q.getSize();
        LambdaQueryWrapper<AiConsultation> wrapper = buildQueryWrapper(q);
        wrapper.eq(AiConsultation::getUserId, userId)
                .orderByDesc(AiConsultation::getConsultTime)
                .orderByDesc(AiConsultation::getId);

        IPage<AiConsultation> page = page(new Page<>(current, size), wrapper);
        Page<AiConsultationVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(convertList(page.getRecords()));
        return result;
    }

    @Override
    public List<AiConsultationVO> listSession(String sessionId) {
        Long userId = SecurityUtils.getUserId();
        ValidateUtils.requireText(sessionId, "会话ID");
        ValidateUtils.requireMaxLength(sessionId, 64, "会话ID");

        List<AiConsultation> list = list(new LambdaQueryWrapper<AiConsultation>()
                .eq(AiConsultation::getUserId, userId)
                .eq(AiConsultation::getSessionId, sessionId.trim())
                .orderByAsc(AiConsultation::getConsultTime)
                .orderByAsc(AiConsultation::getId)
                .last("LIMIT 100"));
        return convertList(list);
    }

    @Override
    public AiConsultationVO getMine(Long id) {
        Long userId = SecurityUtils.getUserId();
        AiConsultation entity = getById(id);
        if (entity == null || !userId.equals(entity.getUserId())) {
            throw ServiceException.notFound("AI问答记录不存在");
        }
        return AiConverter.toConsultationVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteMine(List<Long> ids) {
        Long userId = SecurityUtils.getUserId();
        ValidateUtils.requireIds(ids, "问答记录ID");
        List<AiConsultation> list = listByIds(ids);
        if (list.size() != ids.size()) {
            throw ServiceException.notFound("部分AI问答记录不存在");
        }
        for (AiConsultation entity : list) {
            if (!userId.equals(entity.getUserId())) {
                throw ServiceException.forbidden("无权删除他人的AI问答记录");
            }
        }
        return removeByIds(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiConsultationVO feedback(Long id, AiFeedbackDTO dto) {
        Long userId = SecurityUtils.getUserId();
        ValidateUtils.requireNotNull(dto, "请求参数");
        if (dto.getHelpful() == null) {
            throw ServiceException.badRequest("反馈结果不能为空");
        }
        AiConsultation entity = getById(id);
        if (entity == null || !userId.equals(entity.getUserId())) {
            throw ServiceException.notFound("AI问答记录不存在");
        }
        int feedback = Boolean.TRUE.equals(dto.getHelpful()) ? 1 : 2;
        update(new LambdaUpdateWrapper<AiConsultation>()
                .eq(AiConsultation::getId, id)
                .set(AiConsultation::getFeedback, feedback)
                .set(AiConsultation::getFeedbackRemark, trimToNull(dto.getRemark()))
                .set(AiConsultation::getUpdateBy, userId)
                .set(AiConsultation::getUpdateTime, LocalDateTime.now(ZONE)));
        AiConsultation updated = getById(id);
        if (updated == null) {
            throw ServiceException.notFound("AI问答记录不存在");
        }
        return AiConverter.toConsultationVO(updated);
    }

    @Override
    public IPage<AiConsultationVO> pageAll(AiConsultationQueryDTO query) {
        requireManager();
        AiConsultationQueryDTO q = query == null ? new AiConsultationQueryDTO() : query;
        ValidateUtils.requirePage(q.getPage(), q.getSize());
        validateDateRange(q);

        int current = q.getPage() == null ? 1 : q.getPage();
        int size = q.getSize() == null ? 10 : q.getSize();
        LambdaQueryWrapper<AiConsultation> wrapper = buildQueryWrapper(q);
        if (q.getUserId() != null) {
            wrapper.eq(AiConsultation::getUserId, q.getUserId());
        }
        wrapper.orderByDesc(AiConsultation::getConsultTime)
                .orderByDesc(AiConsultation::getId);

        IPage<AiConsultation> page = page(new Page<>(current, size), wrapper);
        Page<AiConsultationVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(toAdminVOList(page.getRecords()));
        return result;
    }

    @Override
    public AiConsultationVO getDetail(Long id) {
        requireManager();
        AiConsultation entity = getById(id);
        if (entity == null) {
            throw ServiceException.notFound("AI问答记录不存在");
        }
        AiConsultationVO vo = AiConverter.toConsultationVO(entity);
        SysUser user = aiUserInfoSupport.loadUsers(Collections.singletonList(entity.getUserId()))
                .get(entity.getUserId());
        aiUserInfoSupport.fillIdentity(vo, user, aiUserInfoSupport.canViewFullIdentity());
        return vo;
    }

    @Override
    public AiStatsVO stats(Integer days) {
        requireManager();
        int safeDays = days == null ? 7 : days;
        if (safeDays < 1) {
            safeDays = 1;
        }
        if (safeDays > 90) {
            safeDays = 90;
        }
        LocalDate today = LocalDate.now(ZONE);
        LocalDateTime start = today.minusDays(safeDays - 1L).atStartOfDay();
        List<AiConsultation> list = list(new LambdaQueryWrapper<AiConsultation>()
                .ge(AiConsultation::getConsultTime, start));

        long todayCount = 0;
        long fallbackCount = 0;
        long emergencyCount = 0;
        long feedbackCount = 0;
        long helpfulCount = 0;
        long latencySum = 0;
        int latencyCount = 0;

        for (AiConsultation entity : list) {
            if (entity.getConsultTime() != null
                    && today.equals(entity.getConsultTime().toLocalDate())) {
                todayCount++;
            }
            if (Integer.valueOf(3).equals(entity.getAnswerStatus())) {
                fallbackCount++;
            }
            if (Integer.valueOf(1).equals(entity.getEmergency())) {
                emergencyCount++;
            }
            if (entity.getFeedback() != null && entity.getFeedback() > 0) {
                feedbackCount++;
                if (Integer.valueOf(1).equals(entity.getFeedback())) {
                    helpfulCount++;
                }
            }
            if (entity.getLatencyMs() != null && entity.getLatencyMs() > 0) {
                latencySum += entity.getLatencyMs();
                latencyCount++;
            }
        }

        AiStatsVO vo = new AiStatsVO();
        vo.setDays(safeDays);
        vo.setTotal((long) list.size());
        vo.setTodayCount(todayCount);
        vo.setFallbackCount(fallbackCount);
        vo.setEmergencyCount(emergencyCount);
        vo.setFeedbackCount(feedbackCount);
        vo.setHelpfulCount(helpfulCount);
        vo.setFallbackRate(rate(fallbackCount, list.size()));
        vo.setSatisfactionRate(rate(helpfulCount, feedbackCount));
        vo.setAvgLatencyMs(latencyCount == 0 ? 0 : (int) (latencySum / latencyCount));
        vo.setUnansweredCount(countUnanswered(start));
        return vo;
    }

    @Override
    public List<AiConsultationVO> listUnanswered(Integer limit) {
        requireManager();
        int safeLimit = limit == null ? 20 : limit;
        if (safeLimit < 1) {
            safeLimit = 1;
        }
        if (safeLimit > 100) {
            safeLimit = 100;
        }
        List<AiConsultation> list = list(new LambdaQueryWrapper<AiConsultation>()
                .and(w -> w.eq(AiConsultation::getAnswerStatus, 3)
                        .or()
                        .eq(AiConsultation::getFeedback, 2))
                .orderByDesc(AiConsultation::getConsultTime)
                .orderByDesc(AiConsultation::getId)
                .last("LIMIT " + safeLimit));
        return toAdminVOList(list);
    }

    private String validateQuestion(AiAskDTO dto) {
        ValidateUtils.requireNotNull(dto, "请求参数");
        String question = aiSafetyGuard.sanitizeQuestion(dto.getQuestion());
        if (!ValidateUtils.hasText(question)) {
            throw ServiceException.badRequest("问题不能为空");
        }
        int maxInputLength = aiProperties.getMaxInputLength() == null
                ? 500 : aiProperties.getMaxInputLength();
        if (question.length() > maxInputLength) {
            throw ServiceException.badRequest("问题长度不能超过" + maxInputLength + "个字符");
        }
        return question;
    }

    private AiConsultation buildRecord(Long userId, String sessionId, String question,
                                       LocalDateTime consultTime) {
        AiConsultation record = new AiConsultation();
        record.setUserId(userId);
        record.setSessionId(sessionId);
        record.setQuestion(question);
        record.setAnswer("");
        record.setConsultTime(consultTime);
        record.setAnswerStatus(0);
        record.setFeedback(0);
        record.setEmergency(0);
        record.setPromptTokens(0);
        record.setCompletionTokens(0);
        record.setLatencyMs(0);
        record.setKnowledgeSource("");
        record.setRetrievedIds("");
        record.setModelName("");
        record.setProvider("");
        // 异步线程没有 SecurityContext，显式写入操作人，避免自动填充为 0
        record.setCreateBy(userId);
        record.setUpdateBy(userId);
        record.setCreateTime(consultTime);
        record.setUpdateTime(consultTime);
        return record;
    }

    private void saveRecordOrThrow(AiConsultation record) {
        if (!save(record)) {
            throw ServiceException.fail("AI问答记录保存失败");
        }
    }

    private boolean updateRecord(AiConsultation record) {
        if (record == null || record.getId() == null) {
            return false;
        }
        record.setUpdateBy(record.getUserId());
        record.setUpdateTime(LocalDateTime.now(ZONE));
        try {
            if (!updateById(record)) {
                log.warn("AI问答记录更新失败，id={}", record.getId());
                return false;
            }
            return true;
        } catch (Exception e) {
            log.warn("AI问答记录更新异常，id={}：{}", record.getId(), e.getMessage());
            return false;
        }
    }

    private void markRecordFailed(AiConsultation record, String partialAnswer,
                                  String errorMsg, long start) {
        if (record == null || record.getId() == null) {
            return;
        }
        try {
            record.setAnswer(partialAnswer == null ? "" : partialAnswer);
            record.setAnswerStatus(2);
            record.setErrorMsg(truncate(errorMsg, 500));
            record.setLatencyMs((int) (System.currentTimeMillis() - start));
            record.setUpdateBy(record.getUserId());
            record.setUpdateTime(LocalDateTime.now(ZONE));
            if (!updateById(record)) {
                log.warn("AI问答失败状态更新失败，id={}", record.getId());
            }
        } catch (Exception e) {
            log.warn("AI问答失败状态更新异常，id={}：{}", record.getId(), e.getMessage());
        }
    }

    private AiStreamMetaVO buildMeta(AiConsultation record, List<String> sources, boolean emergency) {
        AiStreamMetaVO meta = new AiStreamMetaVO();
        meta.setConsultationId(record.getId());
        meta.setSessionId(record.getSessionId());
        meta.setKnowledgeSource(record.getKnowledgeSource());
        meta.setSources(sources == null ? Collections.emptyList() : sources);
        meta.setEmergency(emergency);
        meta.setConsultTime(record.getConsultTime());
        return meta;
    }

    private void safeSendError(AiStreamSink sink, Integer code, String message,
                               Long consultationId, boolean partial) {
        try {
            sink.sendError(code, message, consultationId, partial);
        } catch (Exception e) {
            log.debug("AI 流式错误事件发送失败：{}", e.getMessage());
        }
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : Math.max(0, value);
    }

    private String safeMessage(Throwable e) {
        if (e == null) {
            return "未知错误";
        }
        String message = e.getMessage();
        if (!ValidateUtils.hasText(message)) {
            message = e.getClass().getSimpleName();
        }
        return truncate(message, 200);
    }

    private LambdaQueryWrapper<AiConsultation> buildQueryWrapper(AiConsultationQueryDTO q) {
        LambdaQueryWrapper<AiConsultation> wrapper = new LambdaQueryWrapper<>();
        if (ValidateUtils.hasText(q.getKeyword())) {
            String keyword = q.getKeyword().trim();
            wrapper.and(w -> w.like(AiConsultation::getQuestion, keyword)
                    .or()
                    .like(AiConsultation::getAnswer, keyword));
        }
        if (q.getAnswerStatus() != null) {
            wrapper.eq(AiConsultation::getAnswerStatus, q.getAnswerStatus());
        }
        if (q.getFeedback() != null) {
            wrapper.eq(AiConsultation::getFeedback, q.getFeedback());
        }
        if (q.getStartDate() != null) {
            wrapper.ge(AiConsultation::getConsultTime, q.getStartDate().atStartOfDay());
        }
        if (q.getEndDate() != null) {
            wrapper.lt(AiConsultation::getConsultTime, q.getEndDate().plusDays(1).atStartOfDay());
        }
        return wrapper;
    }

    private List<AiConsultation> loadHistory(Long userId, String sessionId) {
        int rounds = aiProperties.getHistoryRounds() == null ? 3 : aiProperties.getHistoryRounds();
        if (rounds < 1) {
            rounds = 1;
        }
        if (rounds > 10) {
            rounds = 10;
        }
        // 只带成功/降级的完整回答进入多轮上下文，避免把“处理中/中断”的半截回答喂给模型
        List<AiConsultation> history = list(new LambdaQueryWrapper<AiConsultation>()
                .eq(AiConsultation::getUserId, userId)
                .eq(AiConsultation::getSessionId, sessionId)
                .in(AiConsultation::getAnswerStatus, 1, 3)
                .ne(AiConsultation::getAnswer, "")
                .orderByDesc(AiConsultation::getConsultTime)
                .orderByDesc(AiConsultation::getId)
                .last("LIMIT " + rounds));
        Collections.reverse(history);
        return history;
    }

    private List<AiConsultationVO> convertList(List<AiConsultation> list) {
        List<AiConsultationVO> result = new ArrayList<>();
        if (list == null) {
            return result;
        }
        for (AiConsultation entity : list) {
            result.add(AiConverter.toConsultationVO(entity));
        }
        return result;
    }

    private List<AiConsultationVO> toAdminVOList(List<AiConsultation> list) {
        List<AiConsultationVO> result = new ArrayList<>();
        if (list == null || list.isEmpty()) {
            return result;
        }
        List<Long> userIds = new ArrayList<>();
        for (AiConsultation entity : list) {
            if (entity.getUserId() != null) {
                userIds.add(entity.getUserId());
            }
        }
        Map<Long, SysUser> users = aiUserInfoSupport.loadUsers(userIds);
        boolean full = aiUserInfoSupport.canViewFullIdentity();
        for (AiConsultation entity : list) {
            AiConsultationVO vo = AiConverter.toConsultationVO(entity);
            aiUserInfoSupport.fillIdentity(vo, users.get(entity.getUserId()), full);
            result.add(vo);
        }
        return result;
    }

    private void validateDateRange(AiConsultationQueryDTO q) {
        if (q.getStartDate() != null && q.getEndDate() != null
                && q.getStartDate().isAfter(q.getEndDate())) {
            throw ServiceException.badRequest("开始日期不能晚于结束日期");
        }
    }

    private long countUnanswered(LocalDateTime start) {
        return count(new LambdaQueryWrapper<AiConsultation>()
                .ge(AiConsultation::getConsultTime, start)
                .and(w -> w.eq(AiConsultation::getAnswerStatus, 3)
                        .or()
                        .eq(AiConsultation::getFeedback, 2)));
    }

    private BigDecimal rate(long part, long total) {
        if (total <= 0) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(part * 100.0 / total).setScale(1, RoundingMode.HALF_UP);
    }

    private void requireManager() {
        if (!SecurityUtils.isDoctorOrAdmin()) {
            throw ServiceException.forbidden("仅校医或超级管理员可管理 AI 问答记录");
        }
    }

    private boolean isFallbackProvider(String provider) {
        if (!ValidateUtils.hasText(provider)) {
            return true;
        }
        if ("rule".equalsIgnoreCase(provider)) {
            return true;
        }
        return "ollama".equalsIgnoreCase(provider)
                && "bailian".equalsIgnoreCase(aiProperties.getProvider());
    }

    private String normalizeSessionId(String sessionId) {
        if (!ValidateUtils.hasText(sessionId)) {
            return UUID.randomUUID().toString().replace("-", "");
        }
        String value = sessionId.trim();
        if (!value.matches("[A-Za-z0-9_-]{1,64}")) {
            throw ServiceException.badRequest("会话ID格式不正确");
        }
        return value;
    }

    private String buildKnowledgeSource(List<AiRetrievedMaterial> materials) {
        List<String> sources = buildSourceList(materials);
        if (sources.isEmpty()) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        for (String source : sources) {
            if (result.length() > 0) {
                result.append("；");
            }
            if (result.length() + source.length() > 255) {
                break;
            }
            result.append(source);
        }
        return result.toString();
    }

    private List<String> buildSourceList(List<AiRetrievedMaterial> materials) {
        List<String> sources = new ArrayList<>();
        if (materials == null || materials.isEmpty()) {
            return sources;
        }
        for (AiRetrievedMaterial item : materials) {
            if (item == null || item.getMaterial() == null) {
                continue;
            }
            HealthKnowledgeMaterial material = item.getMaterial();
            StringBuilder source = new StringBuilder("《")
                    .append(ValidateUtils.hasText(material.getTitle())
                            ? material.getTitle() : "未命名素材")
                    .append("》");
            if (ValidateUtils.hasText(material.getSource())) {
                source.append("（").append(material.getSource()).append("）");
            }
            sources.add(source.toString());
        }
        return sources;
    }

    private String buildRetrievedIds(List<AiRetrievedMaterial> materials) {
        if (materials == null || materials.isEmpty()) {
            return "";
        }
        StringBuilder ids = new StringBuilder();
        for (AiRetrievedMaterial item : materials) {
            if (item == null || item.getMaterial() == null
                    || item.getMaterial().getId() == null) {
                continue;
            }
            String id = String.valueOf(item.getMaterial().getId());
            if (ids.length() > 0) {
                if (ids.length() + id.length() + 1 > 255) {
                    break;
                }
                ids.append(",");
            }
            ids.append(id);
        }
        return ids.toString();
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
