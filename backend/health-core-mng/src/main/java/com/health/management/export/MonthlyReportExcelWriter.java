package com.health.management.export;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.health.management.vo.report.DailyHealthPointVO;
import com.health.management.vo.report.MonthlyReportVO;
import org.springframework.stereotype.Component;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 个人月度报告 Excel 生成器：概览、每日明细、健康建议三个 Sheet。
 */
@Component
public class MonthlyReportExcelWriter {

    public void write(OutputStream outputStream, MonthlyReportVO report) {
        ExcelWriter writer = EasyExcel.write(outputStream).build();
        try {
            List<ReportMetricRow> metrics = buildMetricRows(report);
            WriteSheet overview = EasyExcel.writerSheet(0, "月度概览")
                    .head(ReportMetricRow.class).build();
            writer.write(metrics, overview);

            List<DailyReportRow> daily = new ArrayList<>();
            if (report.getDaily() != null) {
                for (DailyHealthPointVO point : report.getDaily()) {
                    daily.add(ReportExportUtils.toDailyReportRow(point));
                }
            }
            WriteSheet dailySheet = EasyExcel.writerSheet(1, "每日明细")
                    .head(DailyReportRow.class).build();
            writer.write(daily, dailySheet);

            List<AdviceRow> advice = new ArrayList<>();
            List<String> suggestions = report.getSuggestions() == null
                    ? Collections.emptyList() : report.getSuggestions();
            for (String suggestion : suggestions) {
                advice.add(new AdviceRow(suggestion));
            }
            WriteSheet adviceSheet = EasyExcel.writerSheet(2, "健康建议")
                    .head(AdviceRow.class).build();
            writer.write(advice, adviceSheet);
        } finally {
            writer.finish();
        }
    }

    private List<ReportMetricRow> buildMetricRows(MonthlyReportVO report) {
        List<ReportMetricRow> rows = new ArrayList<>();
        add(rows, "报告月份", report.getMonth(), "");
        add(rows, "学生", report.getRealName(), "");
        add(rows, "学号", report.getStudentNo(), "");
        add(rows, "学院", report.getCollege(), "");
        add(rows, "专业", report.getMajor(), "");
        add(rows, "年级", report.getGrade(), "");
        add(rows, "班级", report.getClassName(), "");
        add(rows, "打卡天数", value(report.getCheckinDays()), "含异常数据");
        add(rows, "有效打卡天数", value(report.getValidDays()), "data_quality=0");
        add(rows, "异常打卡天数", value(report.getAbnormalDays()), "data_quality=1");
        add(rows, "平均睡眠(小时)", ReportExportUtils.formatNumber(report.getAvgSleepDuration()), "仅统计有效数据");
        add(rows, "熬夜次数", value(report.getStayUpLateCount()), "");
        add(rows, "熬夜率", ReportExportUtils.formatPercent(report.getLateNightRate()), "熬夜次数/填写睡眠记录数");
        add(rows, "平均运动(分钟)", ReportExportUtils.formatNumber(report.getAvgExerciseDuration()), "仅统计有效数据");
        add(rows, "运动不足次数", value(report.getExerciseInsufficientCount()), "");
        add(rows, "运动达标率", ReportExportUtils.formatPercent(report.getExerciseSufficientRate()), "");
        add(rows, "平均心情评分", ReportExportUtils.formatNumber(report.getAvgMoodScore()), "1-5 分");
        add(rows, "早餐率", ReportExportUtils.formatPercent(report.getBreakfastRate()), "");
        add(rows, "平均饮水(ml)", ReportExportUtils.formatNumber(report.getAvgWaterIntake()), "");
        add(rows, "BMI", ReportExportUtils.formatNumber(report.getBmi()), "");
        add(rows, "BMI等级", report.getBmiLevel(), "");
        add(rows, "最新体测总分", ReportExportUtils.formatNumber(report.getLatestPhysicalScore()), "");
        add(rows, "最新体测等级", report.getLatestPhysicalGradeLevel(), "");
        return rows;
    }

    private void add(List<ReportMetricRow> rows, String metric, String value, String note) {
        rows.add(new ReportMetricRow(metric, value == null ? "--" : value, note == null ? "" : note));
    }

    private String value(Number value) {
        return value == null ? "0" : String.valueOf(value);
    }
}
