package com.health.management.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.health.management.dto.ContentAuditDTO;
import com.health.management.dto.HealthKnowledgeMaterialQueryDTO;
import com.health.management.dto.HealthKnowledgeMaterialSaveDTO;
import com.health.management.entity.HealthKnowledgeMaterial;
import com.health.management.vo.HealthKnowledgeMaterialVO;

import java.util.List;

/**
 * AI 问答知识素材业务接口。
 * 素材审核通过且启用后，才会被 AI 健康助手作为权威知识召回。
 */
public interface HealthKnowledgeMaterialService extends IService<HealthKnowledgeMaterial> {

    IPage<HealthKnowledgeMaterialVO> pageMaterials(HealthKnowledgeMaterialQueryDTO query);

    HealthKnowledgeMaterialVO getMaterialDetail(Long id);

    HealthKnowledgeMaterialVO createMaterial(HealthKnowledgeMaterialSaveDTO dto);

    HealthKnowledgeMaterialVO updateMaterial(Long id, HealthKnowledgeMaterialSaveDTO dto);

    boolean deleteMaterials(List<Long> ids);

    /** 超级管理员审核素材 */
    HealthKnowledgeMaterialVO audit(Long id, ContentAuditDTO dto);

    /** 启用/停用素材；启用前必须审核通过 */
    HealthKnowledgeMaterialVO changeStatus(Long id, Integer status);

    /**
     * 供 AI 健康助手召回：审核通过且启用的素材，按关键词模糊匹配标题/内容/关键词。
     */
    List<HealthKnowledgeMaterial> findUsableMaterials(String keyword, Integer limit);
}
