package com.health.management.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.health.management.dto.HealthCheckinSessionQueryDTO;
import com.health.management.dto.HealthCheckinSessionStartDTO;
import com.health.management.entity.HealthCheckinSession;
import com.health.management.vo.HealthCheckinSessionEndVO;
import com.health.management.vo.HealthCheckinSessionVO;

/**
 * 睡眠/运动计时会话业务接口。
 */
public interface HealthCheckinSessionService extends IService<HealthCheckinSession> {

    /** 开始睡眠/运动计时 */
    HealthCheckinSessionVO start(HealthCheckinSessionStartDTO dto);

    /** 结束计时，计算时长并同步每日打卡 */
    HealthCheckinSessionEndVO end(Long id);

    /** 查询当前进行中的会话，没有则返回 null */
    HealthCheckinSessionVO getActive(Integer sessionType);

    /** 当前学生分页查询计时会话 */
    IPage<HealthCheckinSessionVO> pageMine(HealthCheckinSessionQueryDTO query);
}
