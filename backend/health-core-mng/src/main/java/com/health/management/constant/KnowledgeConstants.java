package com.health.management.constant;

/**
 * 健康知识库模块状态常量。
 * 文章与知识素材都需要经过“校医提交、超级管理员审核”的内容合规流程。
 */
public final class KnowledgeConstants {

    /** 文章状态：草稿（未发布） */
    public static final int ARTICLE_STATUS_DRAFT = 0;
    /** 文章状态：已发布 */
    public static final int ARTICLE_STATUS_PUBLISHED = 1;
    /** 文章状态：已下架 */
    public static final int ARTICLE_STATUS_OFFLINE = 2;

    /** 审核状态：待审核 */
    public static final int AUDIT_STATUS_PENDING = 0;
    /** 审核状态：审核通过 */
    public static final int AUDIT_STATUS_APPROVED = 1;
    /** 审核状态：审核不通过 */
    public static final int AUDIT_STATUS_REJECTED = 2;

    /** 通用启用状态：停用 */
    public static final int STATUS_DISABLED = 0;
    /** 通用启用状态：启用 */
    public static final int STATUS_ENABLED = 1;

    private KnowledgeConstants() {
    }
}
