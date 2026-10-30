package com.health.management.controller.ai;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.health.management.common.util.ValidateUtils;
import com.health.management.dto.ai.AiPlanGenerateDTO;
import com.health.management.dto.ai.AiPlanQueryDTO;
import com.health.management.service.AiHealthPlanService;
import com.health.management.vo.ai.AiHealthPlanVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * AI 个性化健康计划接口。
 */
@RestController
@RequestMapping("/api/ai/health_plan")
public class AiHealthPlanController {

    @Autowired
    private AiHealthPlanService aiHealthPlanService;

    /**
     * 生成未来7天个性化计划。
     */
    @PostMapping("/generate")
    public AiHealthPlanVO generate(@Valid @RequestBody AiPlanGenerateDTO dto) {
        return aiHealthPlanService.generate(dto);
    }

    /**
     * 当前学生计划历史。
     */
    @GetMapping("/mine/page")
    public IPage<AiHealthPlanVO> pageMine(@Valid AiPlanQueryDTO query) {
        return aiHealthPlanService.pageMine(query);
    }

    /**
     * 当前学生计划详情。
     */
    @GetMapping("/{id}")
    public AiHealthPlanVO getMine(@PathVariable Long id) {
        return aiHealthPlanService.getMine(id);
    }

    /**
     * 采纳计划。
     */
    @PostMapping("/{id}/adopt")
    public AiHealthPlanVO adopt(@PathVariable Long id) {
        return aiHealthPlanService.adopt(id);
    }

    /**
     * 删除计划。
     */
    @DeleteMapping("/{ids}")
    public boolean deleteMine(@PathVariable String ids) {
        return aiHealthPlanService.deleteMine(ValidateUtils.parseIds(ids));
    }
}
