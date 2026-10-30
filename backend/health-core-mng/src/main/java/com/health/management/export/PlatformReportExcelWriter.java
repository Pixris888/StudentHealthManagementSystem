package com.health.management.export;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.health.management.vo.report.CollegeDistributionVO;
import com.health.management.vo.report.DashboardOverviewVO;
import com.health.management.vo.report.DashboardVO;
import com.health.management.vo.report.EmotionDistributionVO;
import com.health.management.vo.report.LedgerRowVO;
import com.health.management.vo.report.RiskStudentVO;
import com.health.management.vo.report.TrendPointVO;
import org.springframework.stereotype.Component;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 超管全平台报表：概览、学院分布、每日趋势、情绪分布、学生台账、需关注学生。
 */
@Component
public class PlatformReportExcelWriter {

    public void write(OutputStream outputStream, DashboardVO dashboard,
                      List<LedgerRowVO> ledger, List<RiskStudentVO> riskStudents) {
        ExcelWriter writer = EasyExcel.write(outputStream).build();
        try {
            WriteSheet overview = EasyExcel.writerSheet(0, "平台概览")
                    .head(ReportMetricRow.class).build();
            writer.write(buildOverviewRows(dashboard), overview);

            List<CollegeExportRow> colleges = new ArrayList<>();
            List<CollegeDistributionVO> collegeList = dashboard.getCollegeDistribution() == null
                    ? Collections.emptyList() : dashboard.getCollegeDistribution();
            for (CollegeDistributionVO row : collegeList) {
                colleges.add(ReportExportUtils.toCollegeExportRow(row));
            }
            WriteSheet collegeSheet = EasyExcel.writerSheet(1, "学院分布")
                    .head(CollegeExportRow.class).build();
            writer.write(colleges, collegeSheet);

            List<TrendExportRow> trends = new ArrayList<>();
            List<TrendPointVO> trendList = dashboard.getTrend() == null
                    ? Collections.emptyList() : dashboard.getTrend();
            for (TrendPointVO row : trendList) {
                trends.add(ReportExportUtils.toTrendExportRow(row));
            }
            WriteSheet trendSheet = EasyExcel.writerSheet(2, "每日趋势")
                    .head(TrendExportRow.class).build();
            writer.write(trends, trendSheet);

            List<EmotionExportRow> emotions = new ArrayList<>();
            List<EmotionDistributionVO> emotionList = dashboard.getEmotionDistribution() == null
                    ? Collections.emptyList() : dashboard.getEmotionDistribution();
            for (EmotionDistributionVO row : emotionList) {
                emotions.add(ReportExportUtils.toEmotionExportRow(row));
            }
            WriteSheet emotionSheet = EasyExcel.writerSheet(3, "情绪分布")
                    .head(EmotionExportRow.class).build();
            writer.write(emotions, emotionSheet);

            List<LedgerExportRow> ledgerRows = new ArrayList<>();
            if (ledger != null) {
                for (LedgerRowVO row : ledger) {
                    ledgerRows.add(ReportExportUtils.toLedgerExportRow(row));
                }
            }
            WriteSheet ledgerSheet = EasyExcel.writerSheet(4, "学生健康台账")
                    .head(LedgerExportRow.class).build();
            writer.write(ledgerRows, ledgerSheet);

            List<RiskExportRow> riskRows = new ArrayList<>();
            if (riskStudents != null) {
                for (RiskStudentVO row : riskStudents) {
                    riskRows.add(ReportExportUtils.toRiskExportRow(row));
                }
            }
            WriteSheet riskSheet = EasyExcel.writerSheet(5, "需关注学生")
                    .head(RiskExportRow.class).build();
            writer.write(riskRows, riskSheet);
        } finally {
            writer.finish();
        }
    }

    private List<ReportMetricRow> buildOverviewRows(DashboardVO dashboard) {
        List<ReportMetricRow> rows = new ArrayList<>();
        DashboardOverviewVO overview = dashboard.getOverview();
        rows.add(new ReportMetricRow("统计月份", dashboard.getMonth(), ""));
        rows.add(new ReportMetricRow("学院筛选", dashboard.getCollege() == null ? "全部" : dashboard.getCollege(), ""));
        rows.add(new ReportMetricRow("年级筛选", dashboard.getGrade() == null ? "全部" : dashboard.getGrade(), ""));
        if (overview == null) {
            return rows;
        }
        rows.add(new ReportMetricRow("学生总数", value(overview.getStudentTotal()), "启用状态的学生"));
        rows.add(new ReportMetricRow("打卡学生数", value(overview.getCheckinStudents()), ""));
        rows.add(new ReportMetricRow("打卡率", ReportExportUtils.formatPercent(overview.getCheckinRate()), "打卡学生/学生总数"));
        rows.add(new ReportMetricRow("打卡记录数", value(overview.getCheckinRecords()), ""));
        rows.add(new ReportMetricRow("平均睡眠(小时)", ReportExportUtils.formatNumber(overview.getAvgSleepDuration()), ""));
        rows.add(new ReportMetricRow("熬夜次数", value(overview.getStayUpLateCount()), ""));
        rows.add(new ReportMetricRow("熬夜率", ReportExportUtils.formatPercent(overview.getLateNightRate()), ""));
        rows.add(new ReportMetricRow("平均运动(分钟)", ReportExportUtils.formatNumber(overview.getAvgExerciseDuration()), ""));
        rows.add(new ReportMetricRow("运动达标率", ReportExportUtils.formatPercent(overview.getExerciseSufficientRate()), ""));
        rows.add(new ReportMetricRow("平均心情", ReportExportUtils.formatNumber(overview.getAvgMoodScore()), ""));
        rows.add(new ReportMetricRow("早餐率", ReportExportUtils.formatPercent(overview.getBreakfastRate()), ""));
        rows.add(new ReportMetricRow("异常记录数", value(overview.getAbnormalRecords()), "data_quality=1"));
        return rows;
    }

    private String value(Number value) {
        return value == null ? "0" : String.valueOf(value);
    }
}
