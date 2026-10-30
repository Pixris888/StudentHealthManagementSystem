package com.health.management.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.health.management.dto.HealthPhysicalTestQueryDTO;
import com.health.management.dto.HealthPhysicalTestSaveDTO;
import com.health.management.entity.HealthPhysicalTest;
import com.health.management.vo.HealthPhysicalTestVO;
import com.health.management.vo.PhysicalTestImportResultVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 体测成绩业务接口。
 */
public interface HealthPhysicalTestService extends IService<HealthPhysicalTest> {

    /**
     * 当前学生分页查询自己的体测记录。
     */
    IPage<HealthPhysicalTestVO> pageMine(HealthPhysicalTestQueryDTO query);

    /**
     * 当前学生新增一条体测记录（同一学年学期唯一）。
     */
    HealthPhysicalTestVO addMine(HealthPhysicalTestSaveDTO dto);

    /**
     * 当前学生修改自己的体测记录。
     */
    HealthPhysicalTestVO updateMine(Long id, HealthPhysicalTestSaveDTO dto);

    /**
     * 当前学生通过 Excel 导入自己的体测成绩，同一学年学期存在时覆盖更新。
     */
    PhysicalTestImportResultVO importMine(MultipartFile file);

    /**
     * 当前学生的健康指标趋势（按学年学期升序）。
     */
    List<HealthPhysicalTestVO> trendMine(Integer startYear, Integer endYear);

    /**
     * 校医/超级管理员分页查询体测成绩，校医返回脱敏学号。
     */
    IPage<HealthPhysicalTestVO> pageAll(HealthPhysicalTestQueryDTO query);

    /**
     * 校医/超级管理员查看体测记录详情。
     */
    HealthPhysicalTestVO getDetail(Long id);
}
