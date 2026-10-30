package com.health.management.controller.health;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.health.management.dto.HealthPhysicalTestQueryDTO;
import com.health.management.dto.HealthPhysicalTestSaveDTO;
import com.health.management.service.HealthPhysicalTestService;
import com.health.management.vo.HealthPhysicalTestVO;
import com.health.management.vo.PhysicalTestImportResultVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.util.List;

/**
 * 体测成绩接口：
 * 学生维护自己的体测记录、导入 Excel、查看健康指标趋势；
 * 校医/超级管理员按权限查询（校医学号脱敏）。
 */
@RestController
@RequestMapping("/api/health/physical_test")
public class HealthPhysicalTestController {

    @Autowired
    private HealthPhysicalTestService healthPhysicalTestService;

    /**
     * 当前学生分页查询自己的体测记录。
     */
    @GetMapping("/mine/page")
    public IPage<HealthPhysicalTestVO> pageMine(@Valid HealthPhysicalTestQueryDTO query) {
        return healthPhysicalTestService.pageMine(query);
    }

    /**
     * 当前学生新增一条体测记录。
     */
    @PostMapping("/mine")
    public HealthPhysicalTestVO addMine(@Valid @RequestBody HealthPhysicalTestSaveDTO dto) {
        return healthPhysicalTestService.addMine(dto);
    }

    /**
     * 当前学生修改自己的体测记录。
     */
    @PutMapping("/mine/{id}")
    public HealthPhysicalTestVO updateMine(@PathVariable Long id,
                                           @Valid @RequestBody HealthPhysicalTestSaveDTO dto) {
        return healthPhysicalTestService.updateMine(id, dto);
    }

    /**
     * 当前学生通过 Excel 导入体测成绩。
     * 列顺序：学号、测试年份、学期、身高、体重、肺活量、50米跑、坐位体前屈、
     * 立定跳远、耐力跑、力量项目、总分、等级、测试日期。
     */
    @PostMapping("/import")
    public PhysicalTestImportResultVO importExcel(@RequestParam("file") MultipartFile file) {
        return healthPhysicalTestService.importMine(file);
    }

    /**
     * 当前学生的健康指标趋势。
     */
    @GetMapping("/trend")
    public List<HealthPhysicalTestVO> trend(@RequestParam(required = false) Integer startYear,
                                            @RequestParam(required = false) Integer endYear) {
        return healthPhysicalTestService.trendMine(startYear, endYear);
    }

    /**
     * 校医/超级管理员分页查询体测成绩。
     */
    @GetMapping("/page")
    @PreAuthorize("hasAnyRole('DOCTOR_ADMIN','SUPER_ADMIN')")
    public IPage<HealthPhysicalTestVO> pageAll(@Valid HealthPhysicalTestQueryDTO query) {
        return healthPhysicalTestService.pageAll(query);
    }

    /**
     * 校医/超级管理员查看体测记录详情。
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR_ADMIN','SUPER_ADMIN')")
    public HealthPhysicalTestVO detail(@PathVariable Long id) {
        return healthPhysicalTestService.getDetail(id);
    }
}
