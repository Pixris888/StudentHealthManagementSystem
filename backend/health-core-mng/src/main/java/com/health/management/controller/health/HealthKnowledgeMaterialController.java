package com.health.management.controller.health;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.health.management.common.util.ValidateUtils;
import com.health.management.dto.ContentAuditDTO;
import com.health.management.dto.HealthKnowledgeMaterialQueryDTO;
import com.health.management.dto.HealthKnowledgeMaterialSaveDTO;
import com.health.management.service.HealthKnowledgeMaterialService;
import com.health.management.vo.HealthKnowledgeMaterialVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * AI 问答知识素材接口。
 * 校医维护素材，超级管理员审核内容合规性；后续 AI 健康助手只召回审核通过且启用的素材。
 */
@RestController
@RequestMapping("/api/health/knowledge_material")
public class HealthKnowledgeMaterialController {

    @Autowired
    private HealthKnowledgeMaterialService healthKnowledgeMaterialService;

    @GetMapping("/page")
    @PreAuthorize("hasAnyRole('DOCTOR_ADMIN','SUPER_ADMIN')")
    public IPage<HealthKnowledgeMaterialVO> page(@Valid HealthKnowledgeMaterialQueryDTO query) {
        return healthKnowledgeMaterialService.pageMaterials(query);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR_ADMIN','SUPER_ADMIN')")
    public HealthKnowledgeMaterialVO detail(@PathVariable Long id) {
        return healthKnowledgeMaterialService.getMaterialDetail(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DOCTOR_ADMIN','SUPER_ADMIN')")
    public HealthKnowledgeMaterialVO add(@Valid @RequestBody HealthKnowledgeMaterialSaveDTO dto) {
        return healthKnowledgeMaterialService.createMaterial(dto);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR_ADMIN','SUPER_ADMIN')")
    public HealthKnowledgeMaterialVO update(@PathVariable Long id,
                                            @Valid @RequestBody HealthKnowledgeMaterialSaveDTO dto) {
        return healthKnowledgeMaterialService.updateMaterial(id, dto);
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("hasAnyRole('DOCTOR_ADMIN','SUPER_ADMIN')")
    public boolean remove(@PathVariable String ids) {
        return healthKnowledgeMaterialService.deleteMaterials(ValidateUtils.parseIds(ids));
    }

    /**
     * 超级管理员审核知识素材。
     */
    @PostMapping("/{id}/audit")
    @PreAuthorize("hasAuthority('health:article:audit')")
    public HealthKnowledgeMaterialVO audit(@PathVariable Long id,
                                           @Valid @RequestBody ContentAuditDTO dto) {
        return healthKnowledgeMaterialService.audit(id, dto);
    }

    /**
     * 启用/停用素材；启用前必须审核通过。
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('DOCTOR_ADMIN','SUPER_ADMIN')")
    public HealthKnowledgeMaterialVO changeStatus(@PathVariable Long id,
                                                  @RequestParam Integer status) {
        return healthKnowledgeMaterialService.changeStatus(id, status);
    }
}
