package com.health.management.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.health.management.dto.ContentAuditDTO;
import com.health.management.dto.HealthArticleQueryDTO;
import com.health.management.dto.HealthArticleSaveDTO;
import com.health.management.entity.HealthArticle;
import com.health.management.vo.HealthArticleVO;

import java.util.List;

/**
 * 健康科普文章业务接口。
 * 学生只能浏览审核通过且已发布的文章；校医负责维护内容，超级管理员负责合规审核。
 */
public interface HealthArticleService extends IService<HealthArticle> {

    /** 学生端：分页浏览已发布文章（不含正文，减少响应体积） */
    IPage<HealthArticleVO> pagePublished(HealthArticleQueryDTO query);

    /** 学生端：按浏览量获取热门文章 */
    List<HealthArticleVO> listHot(Integer limit);

    /** 文章详情；学生只能查看已发布文章，校医/管理员可预览未发布内容 */
    HealthArticleVO getDetail(Long id);

    /** 管理端：分页查询全部状态的文章 */
    IPage<HealthArticleVO> pageManage(HealthArticleQueryDTO query);

    /** 管理端：查看文章详情（含正文） */
    HealthArticleVO getManageDetail(Long id);

    /** 新增文章：进入待审核状态 */
    HealthArticleVO createArticle(HealthArticleSaveDTO dto);

    /** 编辑文章：重置审核状态；已发布文章需先下架 */
    HealthArticleVO updateArticle(Long id, HealthArticleSaveDTO dto);

    /** 批量删除文章：已发布文章需先下架 */
    boolean deleteArticles(List<Long> ids);

    /** 发布文章：必须审核通过 */
    HealthArticleVO publish(Long id);

    /** 下架文章 */
    HealthArticleVO offline(Long id);

    /** 超级管理员审核文章 */
    HealthArticleVO audit(Long id, ContentAuditDTO dto);
}
