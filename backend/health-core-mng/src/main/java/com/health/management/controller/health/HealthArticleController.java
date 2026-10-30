package com.health.management.controller.health;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.health.management.common.util.ValidateUtils;
import com.health.management.dto.ContentAuditDTO;
import com.health.management.dto.HealthArticleQueryDTO;
import com.health.management.dto.HealthArticleSaveDTO;
import com.health.management.service.HealthArticleService;
import com.health.management.vo.HealthArticleVO;
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
import java.util.List;

/**
 * 健康科普文章接口。
 * 学生浏览已发布内容；校医维护文章；超级管理员审核合规性。
 */
@RestController
@RequestMapping("/api/health/article")
public class HealthArticleController {

    @Autowired
    private HealthArticleService healthArticleService;

    /**
     * 学生端：分页浏览已发布文章（支持分类、关键词筛选）。
     */
    @GetMapping("/page")
    public IPage<HealthArticleVO> page(@Valid HealthArticleQueryDTO query) {
        return healthArticleService.pagePublished(query);
    }

    /**
     * 学生端：热门文章榜单。
     */
    @GetMapping("/hot")
    public List<HealthArticleVO> hot(@RequestParam(required = false) Integer limit) {
        return healthArticleService.listHot(limit);
    }

    /**
     * 管理端：分页查询全部状态文章（可按状态、审核状态筛选）。
     */
    @GetMapping("/manage/page")
    @PreAuthorize("hasAnyRole('DOCTOR_ADMIN','SUPER_ADMIN')")
    public IPage<HealthArticleVO> pageManage(@Valid HealthArticleQueryDTO query) {
        return healthArticleService.pageManage(query);
    }

    /**
     * 管理端：查看文章详情（未发布内容可预览）。
     */
    @GetMapping("/manage/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR_ADMIN','SUPER_ADMIN')")
    public HealthArticleVO manageDetail(@PathVariable Long id) {
        return healthArticleService.getManageDetail(id);
    }

    /**
     * 文章详情：学生只能查看审核通过且已发布的文章。
     */
    @GetMapping("/{id}")
    public HealthArticleVO detail(@PathVariable Long id) {
        return healthArticleService.getDetail(id);
    }

    /**
     * 校医新增文章，提交后自动进入待审核状态。
     */
    @PostMapping
    @PreAuthorize("hasAuthority('health:article:add')")
    public HealthArticleVO add(@Valid @RequestBody HealthArticleSaveDTO dto) {
        return healthArticleService.createArticle(dto);
    }

    /**
     * 校医编辑文章；已发布文章需先下架。
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('health:article:edit')")
    public HealthArticleVO update(@PathVariable Long id,
                                  @Valid @RequestBody HealthArticleSaveDTO dto) {
        return healthArticleService.updateArticle(id, dto);
    }

    /**
     * 批量删除文章，多个ID用逗号分隔。
     */
    @DeleteMapping("/{ids}")
    @PreAuthorize("hasAuthority('health:article:remove')")
    public boolean remove(@PathVariable String ids) {
        return healthArticleService.deleteArticles(ValidateUtils.parseIds(ids));
    }

    /**
     * 发布文章：必须审核通过。
     */
    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('health:article:edit')")
    public HealthArticleVO publish(@PathVariable Long id) {
        return healthArticleService.publish(id);
    }

    /**
     * 下架文章。
     */
    @PostMapping("/{id}/offline")
    @PreAuthorize("hasAuthority('health:article:edit')")
    public HealthArticleVO offline(@PathVariable Long id) {
        return healthArticleService.offline(id);
    }

    /**
     * 超级管理员审核文章。
     */
    @PostMapping("/{id}/audit")
    @PreAuthorize("hasAuthority('health:article:audit')")
    public HealthArticleVO audit(@PathVariable Long id,
                                 @Valid @RequestBody ContentAuditDTO dto) {
        return healthArticleService.audit(id, dto);
    }
}
