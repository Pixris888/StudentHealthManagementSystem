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
import com.health.management.common.exception.ServiceException;
import com.health.management.common.util.ValidateUtils;
import com.health.management.config.AiProperties;
import com.health.management.convert.AiConverter;
import com.health.management.dto.ai.AiPlanGenerateDTO;
import com.health.management.dto.ai.AiPlanQueryDTO;
import com.health.management.entity.AiHealthPlan;
import com.health.management.mapper.AiHealthPlanMapper;
import com.health.management.security.SecurityUtils;
import com.health.management.service.AiHealthPlanService;
import com.health.management.service.ai.AiContextBuilder;
import com.health.management.service.ai.AiFallbackService;
import com.health.management.service.ai.AiHealthContext;
import com.health.management.service.ai.AiPromptBuilder;
import com.health.management.service.ai.AiRateLimiter;
import com.health.management.service.ai.AiSafetyGuard;
import com.health.management.vo.ai.AiHealthPlanVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * AI 个性化健康计划业务实现。
 */
@Service
public class AiHealthPlanServiceImpl extends ServiceImpl<AiHealthPlanMapper, AiHealthPlan>
        implements AiHealthPlanService {

    private static final Logger log = LoggerFactory.getLogger(AiHealthPlanServiceImpl.class);
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    @Autowired
    private LlmClient llmClient;

    @Autowired
    private AiProperties aiProperties;

    @Autowired
    private AiContextBuilder aiContextBuilder;

    @Autowired
    private AiPromptBuilder aiPromptBuilder;

    @Autowired
    private AiSafetyGuard aiSafetyGuard;

    @Autowired
    private AiFallbackService aiFallbackService;

    @Autowired
    private AiRateLimiter aiRateLimiter;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiHealthPlanVO generate(AiPlanGenerateDTO dto) {
        ValidateUtils.requireNotNull(dto, "请求参数");
        ValidateUtils.requireRange(dto.getPlanType(), 1, 3, "计划类型");

        Long userId = SecurityUtils.getUserId();
        aiRateLimiter.checkPlan(userId);
        AiHealthContext context = aiContextBuilder.build(userId);
        List<LlmMessage> messages = aiPromptBuilder.buildPlanMessages(dto.getPlanType(), context);

        String content;
        String provider;
        String modelName;
        String fallbackReason = null;
        boolean fallback = false;

        try {
            LlmResponse response = llmClient.chat(messages);
            content = response.getContent();
            provider = response.getProvider();
            modelName = response.getModel();
            fallbackReason = response.getFallbackReason();
            if (!ValidateUtils.hasText(content)) {
                throw new LlmException("模型返回空计划");
            }
        } catch (Exception e) {
            log.warn("AI 计划生成失败，使用规则模板兜底：{}", e.getMessage());
            content = aiFallbackService.buildPlan(dto.getPlanType(), context);
            provider = "rule";
            modelName = "rule";
            fallback = true;
        }

        if (!fallback && isFallbackProvider(provider)) {
            fallback = true;
            if (ValidateUtils.hasText(fallbackReason)) {
                log.warn("AI 计划使用备用模型：{}", fallbackReason);
            }
        }

        content = aiSafetyGuard.ensureDisclaimer(content);
        LocalDate startDate = LocalDate.now(ZONE);

        AiHealthPlan plan = new AiHealthPlan();
        plan.setUserId(userId);
        plan.setPlanType(dto.getPlanType());
        plan.setPlanTitle(planTitle(dto.getPlanType()));
        plan.setPlanContent(content);
        plan.setPlanData(null);
        plan.setModelName(truncate(modelName, 50));
        plan.setProvider(truncate(provider, 20));
        plan.setBaseCheckinDate(context.getLatestCheckinDate());
        plan.setStartDate(startDate);
        plan.setEndDate(startDate.plusDays(6));
        plan.setStatus(0);

        if (!save(plan)) {
            throw ServiceException.fail("健康计划保存失败，请稍后重试");
        }
        return AiConverter.toPlanVO(plan);
    }

    @Override
    public IPage<AiHealthPlanVO> pageMine(AiPlanQueryDTO query) {
        Long userId = SecurityUtils.getUserId();
        AiPlanQueryDTO q = query == null ? new AiPlanQueryDTO() : query;
        ValidateUtils.requirePage(q.getPage(), q.getSize());

        int current = q.getPage() == null ? 1 : q.getPage();
        int size = q.getSize() == null ? 10 : q.getSize();
        LambdaQueryWrapper<AiHealthPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiHealthPlan::getUserId, userId);
        if (q.getPlanType() != null) {
            wrapper.eq(AiHealthPlan::getPlanType, q.getPlanType());
        }
        if (q.getStatus() != null) {
            wrapper.eq(AiHealthPlan::getStatus, q.getStatus());
        }
        wrapper.orderByDesc(AiHealthPlan::getCreateTime)
                .orderByDesc(AiHealthPlan::getId);

        IPage<AiHealthPlan> page = page(new Page<>(current, size), wrapper);
        Page<AiHealthPlanVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<AiHealthPlanVO> records = new ArrayList<>();
        for (AiHealthPlan plan : page.getRecords()) {
            records.add(AiConverter.toPlanVO(plan));
        }
        result.setRecords(records);
        return result;
    }

    @Override
    public AiHealthPlanVO getMine(Long id) {
        Long userId = SecurityUtils.getUserId();
        AiHealthPlan plan = getById(id);
        if (plan == null || !userId.equals(plan.getUserId())) {
            throw ServiceException.notFound("健康计划不存在");
        }
        return AiConverter.toPlanVO(plan);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiHealthPlanVO adopt(Long id) {
        Long userId = SecurityUtils.getUserId();
        AiHealthPlan plan = getById(id);
        if (plan == null || !userId.equals(plan.getUserId())) {
            throw ServiceException.notFound("健康计划不存在");
        }
        if (!Integer.valueOf(1).equals(plan.getStatus())) {
            LocalDateTime now = LocalDateTime.now(ZONE);
            update(new LambdaUpdateWrapper<AiHealthPlan>()
                    .eq(AiHealthPlan::getId, id)
                    .set(AiHealthPlan::getStatus, 1)
                    .set(AiHealthPlan::getAdoptedTime, now)
                    .set(AiHealthPlan::getUpdateBy, userId)
                    .set(AiHealthPlan::getUpdateTime, now));
            plan = getById(id);
            if (plan == null) {
                throw ServiceException.notFound("健康计划不存在");
            }
            if (!Integer.valueOf(1).equals(plan.getStatus())) {
                throw ServiceException.fail("计划采纳失败，请稍后重试");
            }
        }
        return AiConverter.toPlanVO(plan);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteMine(List<Long> ids) {
        Long userId = SecurityUtils.getUserId();
        ValidateUtils.requireIds(ids, "计划ID");
        List<AiHealthPlan> list = listByIds(ids);
        if (list.size() != ids.size()) {
            throw ServiceException.notFound("部分健康计划不存在");
        }
        for (AiHealthPlan plan : list) {
            if (!userId.equals(plan.getUserId())) {
                throw ServiceException.forbidden("无权删除他人的健康计划");
            }
        }
        return removeByIds(ids);
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

    private String planTitle(Integer planType) {
        if (Integer.valueOf(1).equals(planType)) {
            return "未来7天作息计划";
        }
        if (Integer.valueOf(2).equals(planType)) {
            return "未来7天运动计划";
        }
        return "未来7天综合健康计划";
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
}
