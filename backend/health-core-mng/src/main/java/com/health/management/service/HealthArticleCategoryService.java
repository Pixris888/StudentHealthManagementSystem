package com.health.management.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.health.management.dto.HealthArticleCategorySaveDTO;
import com.health.management.entity.HealthArticleCategory;
import com.health.management.vo.HealthArticleCategoryVO;

import java.util.List;

/**
 * 健康科普文章分类业务接口。
 */
public interface HealthArticleCategoryService extends IService<HealthArticleCategory> {

    /** 分类列表（含文章数量）；includeDisabled=false 时学生只看到启用分类 */
    List<HealthArticleCategoryVO> listCategories(boolean includeDisabled);

    /** 分类树；学生端只返回启用分类 */
    List<HealthArticleCategoryVO> tree(boolean includeDisabled);

    HealthArticleCategoryVO createCategory(HealthArticleCategorySaveDTO dto);

    HealthArticleCategoryVO updateCategory(Long id, HealthArticleCategorySaveDTO dto);

    /** 删除分类：存在子分类或已被文章/素材引用时禁止删除 */
    void deleteCategory(Long id);
}
