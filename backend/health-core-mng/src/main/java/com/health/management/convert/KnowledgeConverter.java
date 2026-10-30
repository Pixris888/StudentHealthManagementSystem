package com.health.management.convert;

import com.health.management.constant.KnowledgeConstants;
import com.health.management.entity.HealthArticle;
import com.health.management.entity.HealthArticleCategory;
import com.health.management.entity.HealthKnowledgeMaterial;
import com.health.management.vo.HealthArticleCategoryVO;
import com.health.management.vo.HealthArticleVO;
import com.health.management.vo.HealthKnowledgeMaterialVO;

/**
 * 健康知识库实体 → VO 转换，并补充状态中文名称。
 */
public final class KnowledgeConverter {

    private KnowledgeConverter() {
    }

    public static HealthArticleVO toArticleVO(HealthArticle article, String categoryName) {
        if (article == null) {
            return null;
        }
        HealthArticleVO vo = new HealthArticleVO();
        vo.setId(article.getId());
        vo.setCategoryId(article.getCategoryId());
        vo.setCategoryName(categoryName);
        vo.setTitle(article.getTitle());
        vo.setCoverUrl(article.getCoverUrl());
        vo.setAuthor(article.getAuthor());
        vo.setSummary(article.getSummary());
        vo.setContent(article.getContent());
        vo.setSource(article.getSource());
        vo.setDocUrl(article.getDocUrl());
        vo.setViewCount(article.getViewCount());
        vo.setLikeCount(article.getLikeCount());
        vo.setStatus(article.getStatus());
        vo.setStatusName(articleStatusName(article.getStatus()));
        vo.setAuditStatus(article.getAuditStatus());
        vo.setAuditStatusName(auditStatusName(article.getAuditStatus()));
        vo.setAuditBy(article.getAuditBy());
        vo.setAuditTime(article.getAuditTime());
        vo.setAuditRemark(article.getAuditRemark());
        vo.setPublishBy(article.getPublishBy());
        vo.setPublishTime(article.getPublishTime());
        vo.setCreateBy(article.getCreateBy());
        vo.setCreateTime(article.getCreateTime());
        vo.setUpdateTime(article.getUpdateTime());
        vo.setRemark(article.getRemark());
        return vo;
    }

    public static HealthArticleCategoryVO toCategoryVO(HealthArticleCategory category, Long articleCount) {
        if (category == null) {
            return null;
        }
        HealthArticleCategoryVO vo = new HealthArticleCategoryVO();
        vo.setId(category.getId());
        vo.setCategoryName(category.getCategoryName());
        vo.setParentId(category.getParentId());
        vo.setOrderNum(category.getOrderNum());
        vo.setStatus(category.getStatus());
        vo.setStatusName(enableStatusName(category.getStatus()));
        vo.setArticleCount(articleCount == null ? 0L : articleCount);
        vo.setCreateTime(category.getCreateTime());
        vo.setRemark(category.getRemark());
        return vo;
    }

    public static HealthKnowledgeMaterialVO toMaterialVO(HealthKnowledgeMaterial material, String categoryName) {
        if (material == null) {
            return null;
        }
        HealthKnowledgeMaterialVO vo = new HealthKnowledgeMaterialVO();
        vo.setId(material.getId());
        vo.setCategoryId(material.getCategoryId());
        vo.setCategoryName(categoryName);
        vo.setTitle(material.getTitle());
        vo.setContent(material.getContent());
        vo.setKeyword(material.getKeyword());
        vo.setSource(material.getSource());
        vo.setStatus(material.getStatus());
        vo.setStatusName(enableStatusName(material.getStatus()));
        vo.setAuditStatus(material.getAuditStatus());
        vo.setAuditStatusName(auditStatusName(material.getAuditStatus()));
        vo.setAuditBy(material.getAuditBy());
        vo.setAuditTime(material.getAuditTime());
        vo.setAuditRemark(material.getAuditRemark());
        vo.setCreateBy(material.getCreateBy());
        vo.setCreateTime(material.getCreateTime());
        vo.setUpdateTime(material.getUpdateTime());
        vo.setRemark(material.getRemark());
        return vo;
    }

    public static String articleStatusName(Integer status) {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case KnowledgeConstants.ARTICLE_STATUS_DRAFT:
                return "草稿";
            case KnowledgeConstants.ARTICLE_STATUS_PUBLISHED:
                return "已发布";
            case KnowledgeConstants.ARTICLE_STATUS_OFFLINE:
                return "已下架";
            default:
                return "未知";
        }
    }

    public static String auditStatusName(Integer auditStatus) {
        if (auditStatus == null) {
            return "未知";
        }
        switch (auditStatus) {
            case KnowledgeConstants.AUDIT_STATUS_PENDING:
                return "待审核";
            case KnowledgeConstants.AUDIT_STATUS_APPROVED:
                return "审核通过";
            case KnowledgeConstants.AUDIT_STATUS_REJECTED:
                return "审核不通过";
            default:
                return "未知";
        }
    }

    public static String enableStatusName(Integer status) {
        if (status == null) {
            return "未知";
        }
        return status == KnowledgeConstants.STATUS_ENABLED ? "启用" : "停用";
    }
}
