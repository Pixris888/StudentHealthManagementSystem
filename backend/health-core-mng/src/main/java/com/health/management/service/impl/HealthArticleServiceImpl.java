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
import com.health.management.dto.HealthArticleQueryDTO;
import com.health.management.dto.HealthArticleSaveDTO;
import com.health.management.entity.HealthArticle;
import com.health.management.entity.HealthArticleCategory;
import com.health.management.entity.SysUser;
import com.health.management.mapper.HealthArticleCategoryMapper;
import com.health.management.mapper.HealthArticleMapper;
import com.health.management.mapper.SysUserMapper;
import com.health.management.security.SecurityUtils;
import com.health.management.service.HealthArticleService;
import com.health.management.vo.HealthArticleVO;
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
 * 健康科普文章业务实现。
 *
 * 内容流转：校医新增/编辑（待审核）→ 超级管理员审核 → 校医发布 → 可下架。
 * 已发布文章必须先下架才能再次编辑，避免线上内容绕过审核。
 */
@Service
public class HealthArticleServiceImpl extends ServiceImpl<HealthArticleMapper, HealthArticle>
        implements HealthArticleService {

    @Autowired
    private HealthArticleCategoryMapper healthArticleCategoryMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Override
    public IPage<HealthArticleVO> pagePublished(HealthArticleQueryDTO query) {
        HealthArticleQueryDTO q = query == null ? new HealthArticleQueryDTO() : query;
        ValidateUtils.requirePage(q.getPage(), q.getSize());
        int current = q.getPage() == null ? 1 : q.getPage();
        int size = q.getSize() == null ? 10 : q.getSize();

        LambdaQueryWrapper<HealthArticle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HealthArticle::getStatus, KnowledgeConstants.ARTICLE_STATUS_PUBLISHED)
                .eq(HealthArticle::getAuditStatus, KnowledgeConstants.AUDIT_STATUS_APPROVED);
        applyCommonFilter(wrapper, q);
        wrapper.select(HealthArticle.class, info -> !"content".equals(info.getColumn()))
                .orderByDesc(HealthArticle::getPublishTime)
                .orderByDesc(HealthArticle::getId);

        IPage<HealthArticle> page = page(new Page<>(current, size), wrapper);
        return toVOPage(page);
    }

    @Override
    public List<HealthArticleVO> listHot(Integer limit) {
        int safeLimit = limit == null ? 5 : limit;
        if (safeLimit < 1) {
            safeLimit = 1;
        }
        if (safeLimit > 20) {
            safeLimit = 20;
        }
        LambdaQueryWrapper<HealthArticle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HealthArticle::getStatus, KnowledgeConstants.ARTICLE_STATUS_PUBLISHED)
                .eq(HealthArticle::getAuditStatus, KnowledgeConstants.AUDIT_STATUS_APPROVED)
                .select(HealthArticle.class, info -> !"content".equals(info.getColumn()))
                .orderByDesc(HealthArticle::getViewCount)
                .orderByDesc(HealthArticle::getPublishTime)
                .orderByDesc(HealthArticle::getId);
        IPage<HealthArticle> page = page(new Page<>(1, safeLimit, false), wrapper);
        return toVOList(page.getRecords());
    }

    @Override
    public HealthArticleVO getDetail(Long id) {
        HealthArticle article = requireArticle(id);
        boolean manager = SecurityUtils.isDoctorOrAdmin();
        boolean visible = Integer.valueOf(KnowledgeConstants.ARTICLE_STATUS_PUBLISHED)
                .equals(article.getStatus())
                && Integer.valueOf(KnowledgeConstants.AUDIT_STATUS_APPROVED)
                .equals(article.getAuditStatus());
        if (!visible && !manager) {
            throw ServiceException.notFound("文章不存在或已下架");
        }
        if (visible) {
            // 原子自增，避免并发下读改写丢失浏览量
            update(new LambdaUpdateWrapper<HealthArticle>()
                    .eq(HealthArticle::getId, id)
                    .setSql("view_count = view_count + 1"));
            article.setViewCount(article.getViewCount() == null ? 1 : article.getViewCount() + 1);
        }
        return KnowledgeConverter.toArticleVO(article, categoryName(article.getCategoryId()));
    }

    @Override
    public IPage<HealthArticleVO> pageManage(HealthArticleQueryDTO query) {
        requireManager();
        HealthArticleQueryDTO q = query == null ? new HealthArticleQueryDTO() : query;
        ValidateUtils.requirePage(q.getPage(), q.getSize());
        int current = q.getPage() == null ? 1 : q.getPage();
        int size = q.getSize() == null ? 10 : q.getSize();

        LambdaQueryWrapper<HealthArticle> wrapper = new LambdaQueryWrapper<>();
        applyCommonFilter(wrapper, q);
        if (q.getStatus() != null) {
            wrapper.eq(HealthArticle::getStatus, q.getStatus());
        }
        if (q.getAuditStatus() != null) {
            wrapper.eq(HealthArticle::getAuditStatus, q.getAuditStatus());
        }
        wrapper.select(HealthArticle.class, info -> !"content".equals(info.getColumn()))
                .orderByDesc(HealthArticle::getCreateTime)
                .orderByDesc(HealthArticle::getId);

        IPage<HealthArticle> page = page(new Page<>(current, size), wrapper);
        return toVOPage(page);
    }

    @Override
    public HealthArticleVO getManageDetail(Long id) {
        requireManager();
        HealthArticle article = requireArticle(id);
        return KnowledgeConverter.toArticleVO(article, categoryName(article.getCategoryId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HealthArticleVO createArticle(HealthArticleSaveDTO dto) {
        requireManager();
        ValidateUtils.requireNotNull(dto, "请求参数");
        ValidateUtils.requireText(dto.getTitle(), "文章标题");
        ValidateUtils.requireText(dto.getContent(), "文章正文");
        HealthArticleCategory category = requireEnabledCategory(dto.getCategoryId());

        HealthArticle article = new HealthArticle();
        applyEditableFields(article, dto);
        article.setAuthor(resolveAuthor(dto.getAuthor()));
        article.setViewCount(0);
        article.setLikeCount(0);
        article.setStatus(KnowledgeConstants.ARTICLE_STATUS_DRAFT);
        article.setAuditStatus(KnowledgeConstants.AUDIT_STATUS_PENDING);
        if (!save(article)) {
            throw ServiceException.fail("文章保存失败，请稍后重试");
        }
        return KnowledgeConverter.toArticleVO(article, category.getCategoryName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HealthArticleVO updateArticle(Long id, HealthArticleSaveDTO dto) {
        requireManager();
        ValidateUtils.requireNotNull(dto, "请求参数");
        ValidateUtils.requireText(dto.getTitle(), "文章标题");
        ValidateUtils.requireText(dto.getContent(), "文章正文");
        HealthArticle article = requireArticle(id);
        if (Integer.valueOf(KnowledgeConstants.ARTICLE_STATUS_PUBLISHED).equals(article.getStatus())) {
            throw ServiceException.conflict("已发布文章请先下架再编辑");
        }
        HealthArticleCategory category = requireEnabledCategory(dto.getCategoryId());

        LambdaUpdateWrapper<HealthArticle> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(HealthArticle::getId, id)
                .set(HealthArticle::getCategoryId, dto.getCategoryId())
                .set(HealthArticle::getTitle, dto.getTitle().trim())
                .set(HealthArticle::getCoverUrl, trimToNull(dto.getCoverUrl()))
                .set(HealthArticle::getAuthor, resolveAuthor(dto.getAuthor()))
                .set(HealthArticle::getSummary, trimToNull(dto.getSummary()))
                .set(HealthArticle::getContent, dto.getContent())
                .set(HealthArticle::getSource, trimToNull(dto.getSource()))
                .set(HealthArticle::getDocUrl, trimToNull(dto.getDocUrl()))
                .set(HealthArticle::getStatus, KnowledgeConstants.ARTICLE_STATUS_DRAFT)
                // 内容变更后必须重新审核，并清除上一次审核意见
                .set(HealthArticle::getAuditStatus, KnowledgeConstants.AUDIT_STATUS_PENDING)
                .set(HealthArticle::getAuditBy, null)
                .set(HealthArticle::getAuditTime, null)
                .set(HealthArticle::getAuditRemark, null)
                .set(HealthArticle::getRemark, trimToNull(dto.getRemark()))
                .set(HealthArticle::getUpdateBy, SecurityUtils.getUserId())
                .set(HealthArticle::getUpdateTime, LocalDateTime.now());
        if (!update(wrapper)) {
            throw ServiceException.fail("文章更新失败，请稍后重试");
        }
        HealthArticle updated = requireArticle(id);
        return KnowledgeConverter.toArticleVO(updated, category.getCategoryName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteArticles(List<Long> ids) {
        requireManager();
        ValidateUtils.requireIds(ids, "文章ID");
        for (Long id : ids) {
            HealthArticle article = requireArticle(id);
            if (Integer.valueOf(KnowledgeConstants.ARTICLE_STATUS_PUBLISHED).equals(article.getStatus())) {
                throw ServiceException.conflict("文章《" + article.getTitle() + "》已发布，请先下架再删除");
            }
        }
        return removeByIds(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HealthArticleVO publish(Long id) {
        requireManager();
        HealthArticle article = requireArticle(id);
        if (!Integer.valueOf(KnowledgeConstants.AUDIT_STATUS_APPROVED).equals(article.getAuditStatus())) {
            throw ServiceException.badRequest("文章尚未通过审核，不能发布");
        }
        if (Integer.valueOf(KnowledgeConstants.ARTICLE_STATUS_PUBLISHED).equals(article.getStatus())) {
            throw ServiceException.badRequest("文章已发布，请勿重复操作");
        }
        HealthArticleCategory category = healthArticleCategoryMapper.selectById(article.getCategoryId());
        if (category == null
                || !Integer.valueOf(KnowledgeConstants.STATUS_ENABLED).equals(category.getStatus())) {
            throw ServiceException.badRequest("文章分类不存在或已停用，不能发布");
        }
        LocalDateTime now = LocalDateTime.now();
        Long operatorId = SecurityUtils.getUserId();
        update(new LambdaUpdateWrapper<HealthArticle>()
                .eq(HealthArticle::getId, id)
                .set(HealthArticle::getStatus, KnowledgeConstants.ARTICLE_STATUS_PUBLISHED)
                .set(HealthArticle::getPublishBy, operatorId)
                .set(HealthArticle::getPublishTime, now)
                .set(HealthArticle::getUpdateBy, operatorId)
                .set(HealthArticle::getUpdateTime, now));
        article.setStatus(KnowledgeConstants.ARTICLE_STATUS_PUBLISHED);
        article.setPublishBy(operatorId);
        article.setPublishTime(now);
        return KnowledgeConverter.toArticleVO(article, category.getCategoryName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HealthArticleVO offline(Long id) {
        requireManager();
        HealthArticle article = requireArticle(id);
        if (!Integer.valueOf(KnowledgeConstants.ARTICLE_STATUS_PUBLISHED).equals(article.getStatus())) {
            throw ServiceException.badRequest("只有已发布文章才能下架");
        }
        LocalDateTime now = LocalDateTime.now();
        update(new LambdaUpdateWrapper<HealthArticle>()
                .eq(HealthArticle::getId, id)
                .set(HealthArticle::getStatus, KnowledgeConstants.ARTICLE_STATUS_OFFLINE)
                .set(HealthArticle::getUpdateBy, SecurityUtils.getUserId())
                .set(HealthArticle::getUpdateTime, now));
        article.setStatus(KnowledgeConstants.ARTICLE_STATUS_OFFLINE);
        article.setUpdateTime(now);
        return KnowledgeConverter.toArticleVO(article, categoryName(article.getCategoryId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HealthArticleVO audit(Long id, ContentAuditDTO dto) {
        if (!SecurityUtils.hasRole(RoleConstants.SUPER_ADMIN)) {
            throw ServiceException.forbidden("仅超级管理员可审核科普内容");
        }
        ValidateUtils.requireNotNull(dto, "请求参数");
        ValidateUtils.requireNotNull(dto.getApproved(), "审核结果");
        HealthArticle article = requireArticle(id);
        if (!Integer.valueOf(KnowledgeConstants.AUDIT_STATUS_PENDING).equals(article.getAuditStatus())) {
            throw ServiceException.conflict("该文章不在待审核状态");
        }
        int auditStatus = Boolean.TRUE.equals(dto.getApproved())
                ? KnowledgeConstants.AUDIT_STATUS_APPROVED
                : KnowledgeConstants.AUDIT_STATUS_REJECTED;
        LocalDateTime now = LocalDateTime.now();
        Long operatorId = SecurityUtils.getUserId();
        update(new LambdaUpdateWrapper<HealthArticle>()
                .eq(HealthArticle::getId, id)
                .set(HealthArticle::getAuditStatus, auditStatus)
                .set(HealthArticle::getAuditBy, operatorId)
                .set(HealthArticle::getAuditTime, now)
                .set(HealthArticle::getAuditRemark, trimToNull(dto.getRemark()))
                .set(HealthArticle::getUpdateBy, operatorId)
                .set(HealthArticle::getUpdateTime, now));
        article.setAuditStatus(auditStatus);
        article.setAuditBy(operatorId);
        article.setAuditTime(now);
        article.setAuditRemark(trimToNull(dto.getRemark()));
        return KnowledgeConverter.toArticleVO(article, categoryName(article.getCategoryId()));
    }

    private void applyCommonFilter(LambdaQueryWrapper<HealthArticle> wrapper, HealthArticleQueryDTO query) {
        if (query.getCategoryId() != null) {
            wrapper.eq(HealthArticle::getCategoryId, query.getCategoryId());
        }
        if (ValidateUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(w -> w.like(HealthArticle::getTitle, keyword)
                    .or()
                    .like(HealthArticle::getSummary, keyword));
        }
    }

    private IPage<HealthArticleVO> toVOPage(IPage<HealthArticle> page) {
        Page<HealthArticleVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(toVOList(page.getRecords()));
        return result;
    }

    private List<HealthArticleVO> toVOList(List<HealthArticle> records) {
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, String> categoryNames = loadCategoryNames(records);
        List<HealthArticleVO> result = new ArrayList<>();
        for (HealthArticle article : records) {
            result.add(KnowledgeConverter.toArticleVO(article, categoryNames.get(article.getCategoryId())));
        }
        return result;
    }

    private Map<Long, String> loadCategoryNames(List<HealthArticle> articles) {
        Set<Long> categoryIds = new HashSet<>();
        for (HealthArticle article : articles) {
            if (article.getCategoryId() != null) {
                categoryIds.add(article.getCategoryId());
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

    private void applyEditableFields(HealthArticle article, HealthArticleSaveDTO dto) {
        article.setCategoryId(dto.getCategoryId());
        article.setTitle(dto.getTitle().trim());
        article.setCoverUrl(trimToNull(dto.getCoverUrl()));
        article.setSummary(trimToNull(dto.getSummary()));
        article.setContent(dto.getContent());
        article.setSource(trimToNull(dto.getSource()));
        article.setDocUrl(trimToNull(dto.getDocUrl()));
        article.setRemark(trimToNull(dto.getRemark()));
    }

    private String resolveAuthor(String author) {
        if (ValidateUtils.hasText(author)) {
            return author.trim();
        }
        SysUser user = sysUserMapper.selectById(SecurityUtils.getUserId());
        if (user != null && ValidateUtils.hasText(user.getRealName())) {
            return user.getRealName();
        }
        return "校医";
    }

    private HealthArticleCategory requireEnabledCategory(Long categoryId) {
        ValidateUtils.requireNotNull(categoryId, "文章分类");
        HealthArticleCategory category = healthArticleCategoryMapper.selectById(categoryId);
        if (category == null) {
            throw ServiceException.badRequest("文章分类不存在");
        }
        if (!Integer.valueOf(KnowledgeConstants.STATUS_ENABLED).equals(category.getStatus())) {
            throw ServiceException.badRequest("文章分类已停用，请先启用分类");
        }
        return category;
    }

    private HealthArticle requireArticle(Long id) {
        ValidateUtils.requireNotNull(id, "文章ID");
        HealthArticle article = getById(id);
        if (article == null) {
            throw ServiceException.notFound("文章不存在");
        }
        return article;
    }

    private void requireManager() {
        if (!SecurityUtils.isDoctorOrAdmin()) {
            throw ServiceException.forbidden("仅校医或超级管理员可管理科普文章");
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
