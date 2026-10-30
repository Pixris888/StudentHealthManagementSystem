package com.health.management.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.health.management.dto.ai.AiPlanGenerateDTO;
import com.health.management.dto.ai.AiPlanQueryDTO;
import com.health.management.entity.AiHealthPlan;
import com.health.management.vo.ai.AiHealthPlanVO;

import java.util.List;

/**
 * AI 个性化健康计划业务接口。
 */
public interface AiHealthPlanService extends IService<AiHealthPlan> {

    /** 生成未来7天计划 */
    AiHealthPlanVO generate(AiPlanGenerateDTO dto);

    /** 当前学生计划历史 */
    IPage<AiHealthPlanVO> pageMine(AiPlanQueryDTO query);

    /** 当前学生计划详情 */
    AiHealthPlanVO getMine(Long id);

    /** 采纳计划 */
    AiHealthPlanVO adopt(Long id);

    /** 删除计划 */
    boolean deleteMine(List<Long> ids);
}
