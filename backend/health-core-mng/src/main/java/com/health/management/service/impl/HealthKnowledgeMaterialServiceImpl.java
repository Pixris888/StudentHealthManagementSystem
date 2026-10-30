package com.health.management.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.health.management.common.constant.RoleConstants;
import com.health.management.common.exception.ServiceException;
import com.health.management.common.util.ValidateUtils;
import com.health.management.constant.KnowledgeConstants;
import com.health.management.convert.KnowledgeConverter;
import com.health.management.dto.ContentAuditDTO;
import com.health.management.dto.HealthKnowledgeMaterialQueryDTO;
import com.health.management.dto.HealthKnowledgeMaterialSaveDTO;
import com.health.management.entity.HealthArticleCategory;
import com.health.management.entity.HealthKnowledgeMaterial;
import com.health.management.mapper.HealthArticleCategoryMapper;
import com.health.management.mapper.HealthKnowledgeMaterialMapper;
import com.health.management.security.SecurityUtils;
import com.health.management.service.HealthKnowledgeMaterialService;
import com.health.management.vo.HealthKnowledgeMaterialVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * AI 问答知识素材业务实现。
 * 素材由校医维护并送审，超级管理员审核通过后，校医可启用；AI 只召回“审核通过 + 启用”的素材。
 */
@Service
public class HealthKnowledgeMaterialServiceImpl
        extends ServiceImpl<HealthKnowledgeMaterialMapper, HealthKnowledgeMaterial>
        implements HealthKnowledgeMaterialService {

    @Autowired
    private HealthArticleCategoryMapper healthArticleCategoryMapper;

    @Override
    public IPage<HealthKnowledgeMaterialVO> pageMaterials(HealthKnowledgeMaterialQueryDTO query) {
        requireManager();
        HealthKnowledgeMaterialQueryDTO q = query == null ? new HealthKnowledgeMaterialQueryDTO() : query;
        ValidateUtils.requirePage(q.getPage(), q.getSize());
        int current = q.getPage() == null ? 1 : q.getPage();
        int size = q.getSize() == null ? 10 : q.getSize();

        LambdaQueryWrapper<HealthKnowledgeMaterial> wrapper = new LambdaQueryWrapper<>();
        if (q.getCategoryId() != null) {
            wrapper.eq(HealthKnowledgeMaterial::getCategoryId, q.getCategoryId());
        }
        if (q.getStatus() != null) {
            wrapper.eq(HealthKnowledgeMaterial::getStatus, q.getStatus());
        }
        if (q.getAuditStatus() != null) {
            wrapper.eq(HealthKnowledgeMaterial::getAuditStatus, q.getAuditStatus());
        }
        if (ValidateUtils.hasText(q.getKeyword())) {
            String keyword = q.getKeyword().trim();
            wrapper.and(w -> w.like(HealthKnowledgeMaterial::getTitle, keyword)
                    .or()
                    .like(HealthKnowledgeMaterial::getContent, keyword)
                    .or()
                    .like(HealthKnowledgeMaterial::getKeyword, keyword));
        }
        wrapper.select(HealthKnowledgeMaterial.class, info -> !"content".equals(info.getColumn()))
                .orderByDesc(HealthKnowledgeMaterial::getCreateTime)
                .orderByDesc(HealthKnowledgeMaterial::getId);

        IPage<HealthKnowledgeMaterial> page = page(new Page<>(current, size), wrapper);
        Page<HealthKnowledgeMaterialVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(toVOList(page.getRecords()));
        return result;
    }

    @Override
    public HealthKnowledgeMaterialVO getMaterialDetail(Long id) {
        requireManager();
        HealthKnowledgeMaterial material = requireMaterial(id);
        return KnowledgeConverter.toMaterialVO(material, categoryName(material.getCategoryId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HealthKnowledgeMaterialVO createMaterial(HealthKnowledgeMaterialSaveDTO dto) {
        requireManager();
        ValidateUtils.requireNotNull(dto, "请求参数");
        ValidateUtils.requireText(dto.getTitle(), "素材标题");
        ValidateUtils.requireText(dto.getContent(), "素材内容");
        HealthArticleCategory category = requireEnabledCategory(dto.getCategoryId());

        HealthKnowledgeMaterial material = new HealthKnowledgeMaterial();
        applyEditableFields(material, dto);
        material.setStatus(dto.getStatus() == null ? KnowledgeConstants.STATUS_ENABLED : dto.getStatus());
        material.setAuditStatus(KnowledgeConstants.AUDIT_STATUS_PENDING);
        if (!save(material)) {
            throw ServiceException.fail("知识素材保存失败，请稍后重试");
        }
        return KnowledgeConverter.toMaterialVO(material, category.getCategoryName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HealthKnowledgeMaterialVO updateMaterial(Long id, HealthKnowledgeMaterialSaveDTO dto) {
        requireManager();
        ValidateUtils.requireNotNull(dto, "请求参数");
        ValidateUtils.requireText(dto.getTitle(), "素材标题");
        ValidateUtils.requireText(dto.getContent(), "素材内容");
        HealthKnowledgeMaterial material = requireMaterial(id);
        HealthArticleCategory category = requireEnabledCategory(dto.getCategoryId());

        LambdaUpdateWrapper<HealthKnowledgeMaterial> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(HealthKnowledgeMaterial::getId, id)
                .set(HealthKnowledgeMaterial::getCategoryId, dto.getCategoryId())
                .set(HealthKnowledgeMaterial::getTitle, dto.getTitle().trim())
                .set(HealthKnowledgeMaterial::getContent, dto.getContent())
                .set(HealthKnowledgeMaterial::getKeyword, trimToNull(dto.getKeyword()))
                .set(HealthKnowledgeMaterial::getSource, trimToNull(dto.getSource()))
                .set(HealthKnowledgeMaterial::getStatus,
                        dto.getStatus() == null ? material.getStatus() : dto.getStatus())
                // 内容变更后重新审核
                .set(HealthKnowledgeMaterial::getAuditStatus, KnowledgeConstants.AUDIT_STATUS_PENDING)
                .set(HealthKnowledgeMaterial::getAuditBy, null)
                .set(HealthKnowledgeMaterial::getAuditTime, null)
                .set(HealthKnowledgeMaterial::getAuditRemark, null)
                .set(HealthKnowledgeMaterial::getRemark, trimToNull(dto.getRemark()))
                .set(HealthKnowledgeMaterial::getUpdateBy, SecurityUtils.getUserId())
                .set(HealthKnowledgeMaterial::getUpdateTime, LocalDateTime.now());
        if (!update(wrapper)) {
            throw ServiceException.fail("知识素材更新失败，请稍后重试");
        }
        HealthKnowledgeMaterial updated = requireMaterial(id);
        return KnowledgeConverter.toMaterialVO(updated, category.getCategoryName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteMaterials(List<Long> ids) {
        requireManager();
        ValidateUtils.requireIds(ids, "素材ID");
        for (Long id : ids) {
            requireMaterial(id);
        }
        return removeByIds(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HealthKnowledgeMaterialVO audit(Long id, ContentAuditDTO dto) {
        if (!SecurityUtils.hasRole(RoleConstants.SUPER_ADMIN)) {
            throw ServiceException.forbidden("仅超级管理员可审核科普内容");
        }
        ValidateUtils.requireNotNull(dto, "请求参数");
        ValidateUtils.requireNotNull(dto.getApproved(), "审核结果");
        HealthKnowledgeMaterial material = requireMaterial(id);
        if (!Integer.valueOf(KnowledgeConstants.AUDIT_STATUS_PENDING).equals(material.getAuditStatus())) {
            throw ServiceException.conflict("该素材不在待审核状态");
        }
        int auditStatus = Boolean.TRUE.equals(dto.getApproved())
                ? KnowledgeConstants.AUDIT_STATUS_APPROVED
                : KnowledgeConstants.AUDIT_STATUS_REJECTED;
        LocalDateTime now = LocalDateTime.now();
        Long operatorId = SecurityUtils.getUserId();
        update(new LambdaUpdateWrapper<HealthKnowledgeMaterial>()
                .eq(HealthKnowledgeMaterial::getId, id)
                .set(HealthKnowledgeMaterial::getAuditStatus, auditStatus)
                .set(HealthKnowledgeMaterial::getAuditBy, operatorId)
                .set(HealthKnowledgeMaterial::getAuditTime, now)
                .set(HealthKnowledgeMaterial::getAuditRemark, trimToNull(dto.getRemark()))
                .set(HealthKnowledgeMaterial::getUpdateBy, operatorId)
                .set(HealthKnowledgeMaterial::getUpdateTime, now));
        material.setAuditStatus(auditStatus);
        material.setAuditBy(operatorId);
        material.setAuditTime(now);
        material.setAuditRemark(trimToNull(dto.getRemark()));
        return KnowledgeConverter.toMaterialVO(material, categoryName(material.getCategoryId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HealthKnowledgeMaterialVO changeStatus(Long id, Integer status) {
        requireManager();
        ValidateUtils.requireRange(status, 0, 1, "状态");
        HealthKnowledgeMaterial material = requireMaterial(id);
        if (status == KnowledgeConstants.STATUS_ENABLED
                && !Integer.valueOf(KnowledgeConstants.AUDIT_STATUS_APPROVED).equals(material.getAuditStatus())) {
            throw ServiceException.badRequest("素材未审核通过，不能启用");
        }
        LocalDateTime now = LocalDateTime.now();
        update(new LambdaUpdateWrapper<HealthKnowledgeMaterial>()
                .eq(HealthKnowledgeMaterial::getId, id)
                .set(HealthKnowledgeMaterial::getStatus, status)
                .set(HealthKnowledgeMaterial::getUpdateBy, SecurityUtils.getUserId())
                .set(HealthKnowledgeMaterial::getUpdateTime, now));
        material.setStatus(status);
        material.setUpdateTime(now);
        return KnowledgeConverter.toMaterialVO(material, categoryName(material.getCategoryId()));
    }

    @Override
    public List<HealthKnowledgeMaterial> findUsableMaterials(String keyword, Integer limit) {
        int safeLimit = limit == null ? 5 : limit;
        if (safeLimit < 1) {
            safeLimit = 1;
        }
        if (safeLimit > 50) {
            safeLimit = 50;
        }
        LambdaQueryWrapper<HealthKnowledgeMaterial> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HealthKnowledgeMaterial::getStatus, KnowledgeConstants.STATUS_ENABLED)
                .eq(HealthKnowledgeMaterial::getAuditStatus, KnowledgeConstants.AUDIT_STATUS_APPROVED);
        if (ValidateUtils.hasText(keyword)) {
            String value = keyword.trim();
            wrapper.and(w -> w.like(HealthKnowledgeMaterial::getTitle, value)
                    .or()
                    .like(HealthKnowledgeMaterial::getContent, value)
                    .or()
                    .like(HealthKnowledgeMaterial::getKeyword, value));
        }
        wrapper.orderByDesc(HealthKnowledgeMaterial::getUpdateTime)
                .orderByDesc(HealthKnowledgeMaterial::getId);
        IPage<HealthKnowledgeMaterial> page = page(new Page<>(1, safeLimit, false), wrapper);
        return page.getRecords();
    }

    private List<HealthKnowledgeMaterialVO> toVOList(List<HealthKnowledgeMaterial> materials) {
        if (materials == null || materials.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, String> categoryNames = loadCategoryNames(materials);
        List<HealthKnowledgeMaterialVO> result = new ArrayList<>();
        for (HealthKnowledgeMaterial material : materials) {
            result.add(KnowledgeConverter.toMaterialVO(material,
                    categoryNames.get(material.getCategoryId())));
        }
        return result;
    }

    private Map<Long, String> loadCategoryNames(List<HealthKnowledgeMaterial> materials) {
        Set<Long> categoryIds = new HashSet<>();
        for (HealthKnowledgeMaterial material : materials) {
            if (material.getCategoryId() != null) {
                categoryIds.add(material.getCategoryId());
            }
        }
        if (categoryIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<HealthArticleCategory> categories = healthArticleCategoryMapper.selectBatchIds(categoryIds);
        Map<Long, String> result = new HashMap<>();
        for (HealthArticleCategory category : categories) {
            result.put(category.getId(), category.getCategoryName());
        }
        return result;
    }

    private String categoryName(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        HealthArticleCategory category = healthArticleCategoryMapper.selectById(categoryId);
        return category == null ? null : category.getCategoryName();
    }

    private void applyEditableFields(HealthKnowledgeMaterial material, HealthKnowledgeMaterialSaveDTO dto) {
        material.setCategoryId(dto.getCategoryId());
        material.setTitle(dto.getTitle().trim());
        material.setContent(dto.getContent());
        material.setKeyword(trimToNull(dto.getKeyword()));
        material.setSource(trimToNull(dto.getSource()));
        material.setRemark(trimToNull(dto.getRemark()));
    }

    private HealthArticleCategory requireEnabledCategory(Long categoryId) {
        ValidateUtils.requireNotNull(categoryId, "素材分类");
        HealthArticleCategory category = healthArticleCategoryMapper.selectById(categoryId);
        if (category == null) {
            throw ServiceException.badRequest("素材分类不存在");
        }
        if (!Integer.valueOf(KnowledgeConstants.STATUS_ENABLED).equals(category.getStatus())) {
            throw ServiceException.badRequest("素材分类已停用，请先启用分类");
        }
        return category;
    }

    private HealthKnowledgeMaterial requireMaterial(Long id) {
        ValidateUtils.requireNotNull(id, "素材ID");
        HealthKnowledgeMaterial material = getById(id);
        if (material == null) {
            throw ServiceException.notFound("知识素材不存在");
        }
        return material;
    }

    private void requireManager() {
        if (!SecurityUtils.isDoctorOrAdmin()) {
            throw ServiceException.forbidden("仅校医或超级管理员可管理 AI 知识素材");
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
