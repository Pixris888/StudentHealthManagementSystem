package com.health.management.controller.health;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.health.management.audit.AuditLogRecorder;
import com.health.management.dto.report.DashboardQueryDTO;
import com.health.management.dto.report.LedgerQueryDTO;
import com.health.management.dto.report.ReportHistoryQueryDTO;
import com.health.management.dto.report.ReportMonthQueryDTO;
import com.health.management.export.LedgerExcelWriter;
import com.health.management.export.MonthlyReportExcelWriter;
import com.health.management.export.PlatformReportExcelWriter;
import com.health.management.export.ReportExportUtils;
import com.health.management.security.SecurityUtils;
import com.health.management.service.HealthReportService;
import com.health.management.vo.report.DashboardVO;
import com.health.management.vo.report.FilterOptionsVO;
import com.health.management.vo.report.HealthReportHistoryVO;
import com.health.management.vo.report.LedgerRowVO;
import com.health.management.vo.report.MonthlyReportVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.validation.Valid;
import java.time.YearMonth;
import java.util.List;

/**
 * 统计报表接口：
 * 学生个人月度报告/下载；校医脱敏看板/台账/导出；超管全平台导出。
 */
@RestController
@RequestMapping("/api/health/report")
public class HealthReportController {

    @Autowired
    private HealthReportService healthReportService;

    @Autowired
    private MonthlyReportExcelWriter monthlyReportExcelWriter;

    @Autowired
    private LedgerExcelWriter ledgerExcelWriter;

    @Autowired
    private PlatformReportExcelWriter platformReportExcelWriter;

    @Autowired
    private AuditLogRecorder auditLogRecorder;

    /**
     * 学生生成/查看个人月度健康报告。
     */
    @GetMapping("/mine/monthly")
    @PreAuthorize("hasAuthority('report:list')")
    public MonthlyReportVO monthly(@Valid ReportMonthQueryDTO query) {
        return healthReportService.getMonthlyReport(query.getMonth());
    }

    /**
     * 学生下载个人月度报告 Excel。
     */
    @GetMapping("/mine/monthly/download")
    @PreAuthorize("hasAuthority('report:list')")
    public ResponseEntity<StreamingResponseBody> downloadMonthly(@Valid ReportMonthQueryDTO query) {
        MonthlyReportVO report = healthReportService.getMonthlyReport(query.getMonth());
        String filename = ReportExportUtils.safeFilename(
                "个人健康报告_" + report.getMonth() + "_" + safe(report.getRealName()) + ".xlsx");
        Long userId = SecurityUtils.getUserId();
        String username = SecurityUtils.getUsername();
        String auditContent = "下载个人月度健康报告 month=" + report.getMonth();
        StreamingResponseBody body = outputStream -> {
            try {
                monthlyReportExcelWriter.write(outputStream, report);
                auditLogRecorder.record("统计报表", AuditLogRecorder.TYPE_EXPORT,
                        userId, username, auditContent, true, null);
            } catch (Exception e) {
                auditLogRecorder.record("统计报表", AuditLogRecorder.TYPE_EXPORT,
                        userId, username, auditContent, false, e.getMessage());
                throw e;
            }
        };
        return excelResponse(filename, body);
    }

    /**
     * 学生历史报告。
     */
    @GetMapping("/mine/history/page")
    @PreAuthorize("hasAuthority('report:list')")
    public IPage<HealthReportHistoryVO> history(@Valid ReportHistoryQueryDTO query) {
        return healthReportService.pageHistory(query);
    }

    /**
     * 全校健康汇总看板（校医脱敏，超管完整）。
     */
    @GetMapping("/dashboard")
    @PreAuthorize("hasAuthority('report:list') and hasAnyRole('DOCTOR_ADMIN','SUPER_ADMIN')")
    public DashboardVO dashboard(@Valid DashboardQueryDTO query) {
        return healthReportService.getDashboard(query);
    }

    /**
     * 校园健康台账分页。
     */
    @GetMapping("/ledger/page")
    @PreAuthorize("hasAuthority('report:list') and hasAnyRole('DOCTOR_ADMIN','SUPER_ADMIN')")
    public IPage<LedgerRowVO> ledgerPage(@Valid LedgerQueryDTO query) {
        return healthReportService.pageLedger(query);
    }

    /**
     * 台账筛选选项。
     */
    @GetMapping("/ledger/filters")
    @PreAuthorize("hasAuthority('report:list') and hasAnyRole('DOCTOR_ADMIN','SUPER_ADMIN')")
    public FilterOptionsVO filters() {
        return healthReportService.getFilterOptions();
    }

    /**
     * 导出校园健康台账（校医脱敏，超管完整）。
     */
    @GetMapping("/ledger/export")
    @PreAuthorize("hasAuthority('report:export') and hasAnyRole('DOCTOR_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<StreamingResponseBody> exportLedger(@Valid LedgerQueryDTO query) {
        List<LedgerRowVO> rows = healthReportService.listLedger(query);
        String month = query.getMonth() == null ? YearMonth.now().toString() : query.getMonth();
        String filename = ReportExportUtils.safeFilename("校园健康台账_" + month + ".xlsx");
        Long userId = SecurityUtils.getUserId();
        String username = SecurityUtils.getUsername();
        String auditContent = "导出校园健康台账 month=" + month
                + ", college=" + safe(query.getCollege())
                + ", grade=" + safe(query.getGrade())
                + ", rows=" + rows.size();
        StreamingResponseBody body = outputStream -> {
            try {
                ledgerExcelWriter.write(outputStream, rows);
                auditLogRecorder.record("统计报表", AuditLogRecorder.TYPE_EXPORT,
                        userId, username, auditContent, true, null);
            } catch (Exception e) {
                auditLogRecorder.record("统计报表", AuditLogRecorder.TYPE_EXPORT,
                        userId, username, auditContent, false, e.getMessage());
                throw e;
            }
        };
        return excelResponse(filename, body);
    }

    /**
     * 超管全平台多 Sheet 报表导出。
     */
    @GetMapping("/platform/export")
    @PreAuthorize("hasAuthority('report:export') and hasRole('SUPER_ADMIN')")
    public ResponseEntity<StreamingResponseBody> exportPlatform(@Valid DashboardQueryDTO query) {
        DashboardVO dashboard = healthReportService.getDashboard(query);
        LedgerQueryDTO ledgerQuery = new LedgerQueryDTO();
        ledgerQuery.setPage(1);
        ledgerQuery.setSize(100);
        ledgerQuery.setMonth(query.getMonth());
        ledgerQuery.setCollege(query.getCollege());
        ledgerQuery.setGrade(query.getGrade());
        List<LedgerRowVO> ledger = healthReportService.listLedger(ledgerQuery);
        String month = dashboard.getMonth() == null ? YearMonth.now().toString() : dashboard.getMonth();
        String filename = ReportExportUtils.safeFilename("全平台健康报表_" + month + ".xlsx");
        Long userId = SecurityUtils.getUserId();
        String username = SecurityUtils.getUsername();
        String auditContent = "导出全平台健康报表 month=" + month
                + ", college=" + safe(query.getCollege())
                + ", grade=" + safe(query.getGrade());
        StreamingResponseBody body = outputStream -> {
            try {
                platformReportExcelWriter.write(outputStream, dashboard, ledger,
                        dashboard.getRiskStudents());
                auditLogRecorder.record("统计报表", AuditLogRecorder.TYPE_EXPORT,
                        userId, username, auditContent, true, null);
            } catch (Exception e) {
                auditLogRecorder.record("统计报表", AuditLogRecorder.TYPE_EXPORT,
                        userId, username, auditContent, false, e.getMessage());
                throw e;
            }
        };
        return excelResponse(filename, body);
    }

    private ResponseEntity<StreamingResponseBody> excelResponse(String filename,
                                                                 StreamingResponseBody body) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ReportExportUtils.contentDisposition(filename))
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .contentType(ReportExportUtils.excelMediaType())
                .body(body);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
