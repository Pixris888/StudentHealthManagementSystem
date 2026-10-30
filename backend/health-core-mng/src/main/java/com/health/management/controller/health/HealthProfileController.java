package com.health.management.controller.health;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.health.management.dto.HealthProfileQueryDTO;
import com.health.management.dto.HealthProfileSaveDTO;
import com.health.management.service.HealthProfileService;
import com.health.management.vo.HealthProfileVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 健康档案接口：
 * 学生维护自己的档案；校医/超级管理员按权限查看（校医数据脱敏）。
 */
@RestController
@RequestMapping("/api/health/profile")
public class HealthProfileController {

    @Autowired
    private HealthProfileService healthProfileService;

    /**
     * 当前登录学生查看自己的档案；未建档时返回账号基础信息。
     */
    @GetMapping("/mine")
    public HealthProfileVO mine() {
        return healthProfileService.getMyProfile();
    }

    /**
     * 当前登录学生新增或更新自己的档案（BMI、年龄由服务端计算）。
     */
    @PutMapping("/mine")
    public HealthProfileVO saveMine(@Valid @RequestBody HealthProfileSaveDTO dto) {
        return healthProfileService.saveMyProfile(dto);
    }

    /**
     * 校医/超级管理员分页查询学生档案。
     */
    @GetMapping("/page")
    @PreAuthorize("hasAnyRole('DOCTOR_ADMIN','SUPER_ADMIN')")
    public IPage<HealthProfileVO> page(@Valid HealthProfileQueryDTO query) {
        return healthProfileService.pageProfiles(query);
    }

    /**
     * 校医/超级管理员查看指定学生的档案详情。
     */
    @GetMapping("/{userId}")
    @PreAuthorize("hasAnyRole('DOCTOR_ADMIN','SUPER_ADMIN')")
    public HealthProfileVO detail(@PathVariable Long userId) {
        return healthProfileService.getProfileDetail(userId);
    }
}
