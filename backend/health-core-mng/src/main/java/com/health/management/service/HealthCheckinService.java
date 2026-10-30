package com.health.management.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.health.management.dto.HealthCheckinQueryDTO;
import com.health.management.dto.HealthCheckinSaveDTO;
import com.health.management.entity.HealthCheckin;
import com.health.management.entity.HealthCheckinSession;
import com.health.management.vo.HealthCheckinSummaryVO;
import com.health.management.vo.HealthCheckinVO;

/**
 * 每日健康打卡业务接口。
 */
public interface HealthCheckinService extends IService<HealthCheckin> {

    /** 今日打卡状态（未打卡时返回 checkedIn=false 的空结构） */
    HealthCheckinVO getToday();

    /** 手动填写/补录打卡；不存在则新增，存在则更新 */
    HealthCheckinVO saveManual(HealthCheckinSaveDTO dto);

    /** 修改指定打卡记录（仅本人） */
    HealthCheckinVO updateManual(Long id, HealthCheckinSaveDTO dto);

    /** 当前学生分页查询历史打卡 */
    IPage<HealthCheckinVO> pageMine(HealthCheckinQueryDTO query);

    /** 打卡详情：本人返回照片；校医/管理员脱敏且不返回照片 */
    HealthCheckinVO getDetail(Long id);

    /** 校医/超级管理员分页查询打卡 */
    IPage<HealthCheckinVO> pageAll(HealthCheckinQueryDTO query);

    /** 个人月度打卡概览，month 格式 yyyy-MM，为空取当前月 */
    HealthCheckinSummaryVO summary(String month);

    /**
     * 计时会话结束后同步每日汇总。由 HealthCheckinSessionService 调用。
     */
    HealthCheckinVO syncFromSession(HealthCheckinSession session);
}
