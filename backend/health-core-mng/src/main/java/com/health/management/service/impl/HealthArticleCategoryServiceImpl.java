package com.health.management.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.health.management.common.exception.ServiceException;
import com.health.management.common.util.ValidateUtils;
import com.health.management.convert.KnowledgeConverter;
import com.health.management.dto.HealthArticleCategorySaveDTO;
import com.health.management.entity.HealthArticle;
import com.health.management.entity.HealthArticleCategory;
import com.health.management.entity.HealthKnowledgeMaterial;
import com.health.management.mapper.HealthArticleCategoryMapper;
import com.health.management.mapper.HealthArticleMapper;
import com.health.management.mapper.HealthKnowledgeMaterialMapper;
import com.health.management.security.SecurityUtils;
import com.health.management.service.HealthArticleCategoryService;
import com.health.management.vo.HealthArticleCategoryVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 科普文章分类业务实现。
 */
@Service
public class HealthArticleCategoryServiceImpl
        extends ServiceImpl<HealthArticleCategoryMapper, HealthArticleCategory>
        implements HealthArticleCategoryService {

    @Autowired
    private HealthArticleMapper healthArticleMapper;

    @Autowired
    private HealthKnowledgeMaterialMapper healthKnowledgeMaterialMapper;

    @Override
    public List<HealthArticleCategoryVO> listCategories(boolean includeDisabled) {
        if (includeDisabled && !SecurityUtils.isDoctorOrAdmin()) {
            throw ServiceException.forbidden("仅校医或超级管理员可查看停用分类");
        }
        LambdaQueryWrapper<HealthArticleCategory> wrapper = new LambdaQueryWrapper<>();
        if (!includeDisabled) {
            wrapper.eq(HealthArticleCategory::getStatus, 1);
        }
        wrapper.orderByAsc(HealthArticleCategory::getOrderNum)
                .orderByAsc(HealthArticleCategory::getId);

        List<HealthArticleCategory> categories = list(wrapper);
        // 学生端只统计已发布文章，管理端统计全部未删除文章
        Map<Long, Long> articleCounts = loadArticleCounts(!includeDisabled);
        List<HealthArticleCategoryVO> result = new ArrayList<>();
        for (HealthArticleCategory category : categories) {
            result.add(KnowledgeConverter.toCategoryVO(category, articleCounts.get(category.getId())));
        }
        return result;
    }

    @Override
    public List<HealthArticleCategoryVO> tree(boolean includeDisabled) {
        List<HealthArticleCategoryVO> flat = listCategories(includeDisabled);
        Map<Long, HealthArticleCategoryVO> indexed = new LinkedHashMap<>();
        for (HealthArticleCategoryVO vo : flat) {
            indexed.put(vo.getId(), vo);
        }
        List<HealthArticleCategoryVO> roots = new ArrayList<>();
        for (HealthArticleCategoryVO vo : indexed.values()) {
            Long parentId = vo.getParentId();
            if (parentId == null || parentId == 0L || !indexed.containsKey(parentId)) {
                roots.add(vo);
                continue;
            }
            HealthArticleCategoryVO parent = indexed.get(parentId);
            if (parent.getChildren() == null) {
                parent.setChildren(new ArrayList<>());
            }
            parent.getChildren().add(vo);
        }
        return roots;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HealthArticleCategoryVO createCategory(HealthArticleCategorySaveDTO dto) {
        ValidateUtils.requireNotNull(dto, "请求参数");
        ValidateUtils.requireText(dto.getCategoryName(), "分类名称");
        Long parentId = normalizeParentId(dto.getParentId());
        validateParent(parentId, null);
        String categoryName = dto.getCategoryName().trim();
        requireNameUnique(parentId, categoryName, null);

        HealthArticleCategory category = new HealthArticleCategory();
        category.setCategoryName(categoryName);
        category.setParentId(parentId);
        category.setOrderNum(dto.getOrderNum() == null ? 0 : dto.getOrderNum());
        category.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        category.setRemark(trimToNull(dto.getRemark()));
        if (!save(category)) {
            throw ServiceException.fail("分类保存失败，请稍后重试");
        }
        return KnowledgeConverter.toCategoryVO(category, 0L);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HealthArticleCategoryVO updateCategory(Long id, HealthArticleCategorySaveDTO dto) {
        ValidateUtils.requireNotNull(id, "分类ID");
        ValidateUtils.requireNotNull(dto, "请求参数");
        ValidateUtils.requireText(dto.getCategoryName(), "分类名称");
        HealthArticleCategory existing = getById(id);
        if (existing == null) {
            throw ServiceException.notFound("分类不存在");
        }
        Long parentId = normalizeParentId(dto.getParentId());
        validateParent(parentId, id);
        String categoryName = dto.getCategoryName().trim();
        requireNameUnique(parentId, categoryName, id);

        LambdaUpdateWrapper<HealthArticleCategory> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(HealthArticleCategory::getId, id)
                .set(HealthArticleCategory::getCategoryName, categoryName)
                .set(HealthArticleCategory::getParentId, parentId)
                .set(HealthArticleCategory::getOrderNum, dto.getOrderNum() == null ? 0 : dto.getOrderNum())
                .set(HealthArticleCategory::getStatus, dto.getStatus() == null ? 1 : dto.getStatus())
                .set(HealthArticleCategory::getRemark, trimToNull(dto.getRemark()))
                .set(HealthArticleCategory::getUpdateBy, SecurityUtils.getUserId())
                .set(HealthArticleCategory::getUpdateTime, LocalDateTime.now());
        if (!update(wrapper)) {
            throw ServiceException.fail("分类更新失败，请稍后重试");
        }

        HealthArticleCategory updated = getById(id);
        return KnowledgeConverter.toCategoryVO(updated, countArticles(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCategory(Long id) {
        ValidateUtils.requireNotNull(id, "分类ID");
        if (getById(id) == null) {
            throw ServiceException.notFound("分类不存在");
        }
        Long childCount = count(new LambdaQueryWrapper<HealthArticleCategory>()
                .eq(HealthArticleCategory::getParentId, id));
        if (childCount != null && childCount > 0) {
            throw ServiceException.conflict("该分类下还有子分类，不能删除");
        }
        if (countArticles(id) > 0) {
            throw ServiceException.conflict("该分类下还有科普文章，不能删除");
        }
        Long materialCount = healthKnowledgeMaterialMapper.selectCount(
                new LambdaQueryWrapper<HealthKnowledgeMaterial>()
                        .eq(HealthKnowledgeMaterial::getCategoryId, id));
        if (materialCount != null && materialCount > 0) {
            throw ServiceException.conflict("该分类下还有 AI 知识素材，不能删除");
        }
        removeById(id);
    }

    private Map<Long, Long> loadArticleCounts(boolean publishedOnly) {
        Map<Long, Long> counts = new HashMap<>();
        QueryWrapper<HealthArticle> wrapper = new QueryWrapper<>();
        wrapper.select("category_id", "COUNT(*) AS article_count")
                .groupBy("category_id");
        if (publishedOnly) {
            wrapper.eq("status", 1).eq("audit_status", 1);
        }
        List<Map<String, Object>> rows = healthArticleMapper.selectMaps(wrapper);
        for (Map<String, Object> row : rows) {
            Object categoryId = row.get("category_id");
            Object count = row.get("article_count");
            if (categoryId instanceof Number && count instanceof Number) {
                counts.put(((Number) categoryId).longValue(), ((Number) count).longValue());
            }
        }
        return counts;
    }

    private long countArticles(Long categoryId) {
        Long count = healthArticleMapper.selectCount(new LambdaQueryWrapper<HealthArticle>()
                .eq(HealthArticle::getCategoryId, categoryId));
        return count == null ? 0L : count;
    }

    private Long normalizeParentId(Long parentId) {
        return parentId == null ? 0L : parentId;
    }

    /**
     * 校验父分类存在，并防止分类树出现环。
     */
    private void validateParent(Long parentId, Long selfId) {
        if (parentId == null || parentId == 0L) {
            return;
        }
        if (getById(parentId) == null) {
            throw ServiceException.badRequest("父分类不存在");
        }
        if (selfId == null) {
            return;
        }
        Set<Long> visited = new HashSet<>();
        Long cursor = parentId;
        while (cursor != null && cursor != 0L) {
            if (cursor.equals(selfId)) {
                throw ServiceException.badRequest("父分类不能是当前分类或其子分类");
            }
            if (!visited.add(cursor)) {
                break;
            }
            HealthArticleCategory parent = getById(cursor);
            cursor = parent == null ? 0L : parent.getParentId();
        }
    }

    private void requireNameUnique(Long parentId, String categoryName, Long excludeId) {
        LambdaQueryWrapper<HealthArticleCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HealthArticleCategory::getParentId, parentId)
                .eq(HealthArticleCategory::getCategoryName, categoryName);
        if (excludeId != null) {
            wrapper.ne(HealthArticleCategory::getId, excludeId);
        }
        if (count(wrapper) > 0) {
            throw ServiceException.conflict("同一父分类下已存在同名分类");
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
