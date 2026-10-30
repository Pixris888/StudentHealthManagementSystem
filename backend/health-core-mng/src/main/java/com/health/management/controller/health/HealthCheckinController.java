package com.health.management.controller.health;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.health.management.dto.HealthCheckinQueryDTO;
import com.health.management.dto.HealthCheckinSaveDTO;
import com.health.management.service.HealthCheckinService;
import com.health.management.vo.HealthCheckinSummaryVO;
import com.health.management.vo.HealthCheckinVO;
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

import javax.validation.Valid;

/**
 * 每日健康打卡接口。
 * 睡眠/运动计时接口在 HealthCheckinSessionController，照片接口在 HealthCheckinPhotoController。
 */
@RestController
@RequestMapping("/api/health/checkin")
public class HealthCheckinController {

    @Autowired
    private HealthCheckinService healthCheckinService;

    /**
     * 今日打卡状态。
     */
    @GetMapping("/today")
    public HealthCheckinVO today() {
        return healthCheckinService.getToday();
    }

    /**
     * 手动填写/补录打卡（饮食、情绪、手动睡眠/运动；不存在则新增，存在则更新）。
     */
    @PostMapping
    public HealthCheckinVO save(@Valid @RequestBody HealthCheckinSaveDTO dto) {
        return healthCheckinService.saveManual(dto);
    }

    /**
     * 修改自己的打卡记录。
     */
    @PutMapping("/{id}")
    public HealthCheckinVO update(@PathVariable Long id,
                                  @Valid @RequestBody HealthCheckinSaveDTO dto) {
        return healthCheckinService.updateManual(id, dto);
    }

    /**
     * 当前学生历史打卡分页。
     */
    @GetMapping("/mine/page")
    public IPage<HealthCheckinVO> pageMine(@Valid HealthCheckinQueryDTO query) {
        return healthCheckinService.pageMine(query);
    }

    /**
     * 个人月度打卡概览。
     */
    @GetMapping("/summary")
    public HealthCheckinSummaryVO summary(@RequestParam(required = false) String month) {
        return healthCheckinService.summary(month);
    }

    /**
     * 校医/超级管理员分页查询打卡记录。
     */
    @GetMapping("/page")
    @PreAuthorize("hasAnyRole('DOCTOR_ADMIN','SUPER_ADMIN')")
    public IPage<HealthCheckinVO> pageAll(@Valid HealthCheckinQueryDTO query) {
        return healthCheckinService.pageAll(query);
    }

    /**
     * 打卡详情：本人返回照片；校医/管理员脱敏且不返回照片。
     */
    @GetMapping("/{id}")
    public HealthCheckinVO detail(@PathVariable Long id) {
        return healthCheckinService.getDetail(id);
    }
}
