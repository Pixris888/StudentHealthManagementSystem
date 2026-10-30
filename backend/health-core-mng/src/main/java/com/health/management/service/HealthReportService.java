package com.health.management.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.health.management.dto.report.DashboardQueryDTO;
import com.health.management.dto.report.LedgerQueryDTO;
import com.health.management.dto.report.ReportHistoryQueryDTO;
import com.health.management.entity.HealthReport;
import com.health.management.vo.report.DashboardVO;
import com.health.management.vo.report.FilterOptionsVO;
import com.health.management.vo.report.HealthReportHistoryVO;
import com.health.management.vo.report.LedgerRowVO;
import com.health.management.vo.report.MonthlyReportVO;

import java.util.List;

/**
 * 统计报表业务接口。
 */
public interface HealthReportService extends IService<HealthReport> {

    /** 生成/查看当前学生某月个人健康报告 */
    MonthlyReportVO getMonthlyReport(String month);

    /** 当前学生历史报告 */
    IPage<HealthReportHistoryVO> pageHistory(ReportHistoryQueryDTO query);

    /** 全校健康看板（校医脱敏、超管完整） */
    DashboardVO getDashboard(DashboardQueryDTO query);

    /** 校园健康台账分页（校医脱敏、超管完整） */
    IPage<LedgerRowVO> pageLedger(LedgerQueryDTO query);

    /** 台账导出数据（校医脱敏、超管完整） */
    List<LedgerRowVO> listLedger(LedgerQueryDTO query);

    /** 台账筛选选项 */
    FilterOptionsVO getFilterOptions();
}
