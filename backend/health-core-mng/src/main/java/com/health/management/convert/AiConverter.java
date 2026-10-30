package com.health.management.convert;

import com.health.management.entity.AiConsultation;
import com.health.management.entity.AiHealthPlan;
import com.health.management.vo.ai.AiAnswerVO;
import com.health.management.vo.ai.AiConsultationVO;
import com.health.management.vo.ai.AiHealthPlanVO;
import org.springframework.beans.BeanUtils;

import java.util.List;

/**
 * AI 模块实体/展示对象转换。
 */
public final class AiConverter {

    private AiConverter() {
    }

    public static AiConsultationVO toConsultationVO(AiConsultation entity) {
        if (entity == null) {
            return null;
        }
        AiConsultationVO vo = new AiConsultationVO();
        BeanUtils.copyProperties(entity, vo);
        vo.setFallback(Integer.valueOf(3).equals(entity.getAnswerStatus()));
        return vo;
    }

    public static AiAnswerVO toAnswerVO(AiConsultation entity, List<String> sources) {
        if (entity == null) {
            return null;
        }
        AiAnswerVO vo = new AiAnswerVO();
        vo.setConsultationId(entity.getId());
        vo.setSessionId(entity.getSessionId());
        vo.setAnswer(entity.getAnswer());
        vo.setKnowledgeSource(entity.getKnowledgeSource());
        vo.setModelName(entity.getModelName());
        vo.setProvider(entity.getProvider());
        vo.setAnswerStatus(entity.getAnswerStatus());
        vo.setFallback(Integer.valueOf(3).equals(entity.getAnswerStatus()));
        vo.setEmergency(Integer.valueOf(1).equals(entity.getEmergency()));
        vo.setConsultTime(entity.getConsultTime());
        if (sources != null && !sources.isEmpty()) {
            vo.setSources(sources);
        }
        return vo;
    }

    public static AiHealthPlanVO toPlanVO(AiHealthPlan entity) {
        if (entity == null) {
            return null;
        }
        AiHealthPlanVO vo = new AiHealthPlanVO();
        BeanUtils.copyProperties(entity, vo);
        vo.setPlanTypeName(planTypeName(entity.getPlanType()));
        return vo;
    }

    private static String planTypeName(Integer planType) {
        if (Integer.valueOf(1).equals(planType)) {
            return "作息计划";
        }
        if (Integer.valueOf(2).equals(planType)) {
            return "运动计划";
        }
        return "综合计划";
    }
}
