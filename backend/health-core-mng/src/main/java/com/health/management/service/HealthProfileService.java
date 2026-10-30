package com.health.management.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.health.management.dto.HealthProfileQueryDTO;
import com.health.management.dto.HealthProfileSaveDTO;
import com.health.management.entity.HealthProfile;
import com.health.management.vo.HealthProfileVO;

/**
 * 健康档案业务接口。
 */
public interface HealthProfileService extends IService<HealthProfile> {

    /**
     * 查询当前登录学生的健康档案；尚未建档时返回带账号基础信息的空档案。
     */
    HealthProfileVO getMyProfile();

    /**
     * 新增或更新当前登录学生的健康档案。
     */
    HealthProfileVO saveMyProfile(HealthProfileSaveDTO dto);

    /**
     * 校医/超级管理员分页查询学生健康档案，校医返回脱敏数据。
     */
    IPage<HealthProfileVO> pageProfiles(HealthProfileQueryDTO query);

    /**
     * 校医/超级管理员查看指定学生的健康档案详情。
     */
    HealthProfileVO getProfileDetail(Long userId);
}
