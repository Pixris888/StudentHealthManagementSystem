package com.health.management.controller.health;

import com.health.management.dto.HealthArticleCategorySaveDTO;
import com.health.management.service.HealthArticleCategoryService;
import com.health.management.vo.HealthArticleCategoryVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * 健康科普文章分类接口。
 * 学生查看启用分类；校医/超级管理员维护分类。
 */
@RestController
@RequestMapping("/api/health/article_category")
public class HealthArticleCategoryController {

    @Autowired
    private HealthArticleCategoryService healthArticleCategoryService;

    /**
     * 启用分类列表（学生端文章筛选）。
     */
    @GetMapping("/list")
    public List<HealthArticleCategoryVO> list() {
        return healthArticleCategoryService.listCategories(false);
    }

    /**
     * 启用分类树（学生端）。
     */
    @GetMapping("/tree")
    public List<HealthArticleCategoryVO> tree() {
        return healthArticleCategoryService.tree(false);
    }

    /**
     * 全部分类列表（含停用分类和文章数量，管理端）。
     */
    @GetMapping("/manage/list")
    @PreAuthorize("hasAnyRole('DOCTOR_ADMIN','SUPER_ADMIN')")
    public List<HealthArticleCategoryVO> manageList() {
        return healthArticleCategoryService.listCategories(true);
    }

    /**
     * 全部分类树（含停用分类，管理端）。
     */
    @GetMapping("/manage/tree")
    @PreAuthorize("hasAnyRole('DOCTOR_ADMIN','SUPER_ADMIN')")
    public List<HealthArticleCategoryVO> manageTree() {
        return healthArticleCategoryService.tree(true);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DOCTOR_ADMIN','SUPER_ADMIN')")
    public HealthArticleCategoryVO add(@Valid @RequestBody HealthArticleCategorySaveDTO dto) {
        return healthArticleCategoryService.createCategory(dto);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR_ADMIN','SUPER_ADMIN')")
    public HealthArticleCategoryVO update(@PathVariable Long id,
                                          @Valid @RequestBody HealthArticleCategorySaveDTO dto) {
        return healthArticleCategoryService.updateCategory(id, dto);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR_ADMIN','SUPER_ADMIN')")
    public void remove(@PathVariable Long id) {
        healthArticleCategoryService.deleteCategory(id);
    }
}
