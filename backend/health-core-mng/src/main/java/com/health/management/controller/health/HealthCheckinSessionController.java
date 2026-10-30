package com.health.management.controller.health;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.health.management.dto.HealthCheckinSessionQueryDTO;
import com.health.management.dto.HealthCheckinSessionStartDTO;
import com.health.management.service.HealthCheckinSessionService;
import com.health.management.vo.HealthCheckinSessionEndVO;
import com.health.management.vo.HealthCheckinSessionVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 睡眠/运动计时接口：开始 → 查询进行中 → 结束。
 * 开始和结束时间由服务端记录，前端只负责按钮触发。
 */
@RestController
@RequestMapping("/api/health/checkin/session")
public class HealthCheckinSessionController {

    @Autowired
    private HealthCheckinSessionService healthCheckinSessionService;

    /**
     * 开始睡眠/运动计时。
     * 如果已有进行中的同类型会话，直接返回已有会话（幂等）。
     */
    @PostMapping("/start")
    public HealthCheckinSessionVO start(@Valid @RequestBody HealthCheckinSessionStartDTO dto) {
        return healthCheckinSessionService.start(dto);
    }

    /**
     * 结束计时，返回会话和同步后的每日打卡。
     */
    @PostMapping("/{id}/end")
    public HealthCheckinSessionEndVO end(@PathVariable Long id) {
        return healthCheckinSessionService.end(id);
    }

    /**
     * 查询当前进行中的会话，没有返回 null。
     */
    @GetMapping("/active")
    public HealthCheckinSessionVO active(@RequestParam Integer sessionType) {
        return healthCheckinSessionService.getActive(sessionType);
    }

    /**
     * 当前学生计时会话历史分页。
     */
    @GetMapping("/mine/page")
    public IPage<HealthCheckinSessionVO> pageMine(@Valid HealthCheckinSessionQueryDTO query) {
        return healthCheckinSessionService.pageMine(query);
    }
}
