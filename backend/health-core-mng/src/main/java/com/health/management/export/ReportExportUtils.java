package com.health.management.export;

import com.health.management.vo.report.CollegeDistributionVO;
import com.health.management.vo.report.DailyHealthPointVO;
import com.health.management.vo.report.EmotionDistributionVO;
import com.health.management.vo.report.LedgerRowVO;
import com.health.management.vo.report.RiskStudentVO;
import com.health.management.vo.report.TrendPointVO;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * 报表导出格式工具。
 */
public final class ReportExportUtils {

    public static final String EXCEL_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private ReportExportUtils() {
    }

    public static MediaType excelMediaType() {
        return MediaType.parseMediaType(EXCEL_CONTENT_TYPE);
    }

    public static String contentDisposition(String filename) {
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        return "attachment; filename*=UTF-8''" + encoded;
    }

    public static String safeFilename(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return "report.xlsx";
        }
        return filename.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    public static String formatDate(LocalDate date) {
        return date == null ? "" : date.toString();
    }

    public static String formatTime(LocalTime time) {
        return time == null ? "" : time.format(TIME_FORMAT);
    }

    public static String formatNumber(BigDecimal value) {
        return value == null ? "--" : value.stripTrailingZeros().toPlainString();
    }

    public static String formatPercent(BigDecimal value) {
        return value == null ? "--" : value.stripTrailingZeros().toPlainString() + "%";
    }

    public static String yesNo(Integer value) {
        if (value == null) {
            return "未填写";
        }
        return value == 1 ? "是" : "否";
    }

    public static String quality(Integer value) {
        return Integer.valueOf(1).equals(value) ? "异常" : "正常";
    }

    public static DailyReportRow toDailyReportRow(DailyHealthPointVO point) {
        return new DailyReportRow(
                formatDate(point.getDate()),
                formatTime(point.getSleepTime()),
                formatNumber(point.getSleepDuration()),
                yesNo(point.getStayUpLate()),
                point.getExerciseDuration() == null ? "未填写" : String.valueOf(point.getExerciseDuration()),
                yesNo(point.getExerciseSufficient()),
                point.getMoodScore() == null ? "未填写" : String.valueOf(point.getMoodScore()),
                yesNo(point.getDietBreakfast()),
                point.getWaterIntake() == null ? "未填写" : String.valueOf(point.getWaterIntake()),
                quality(point.getDataQuality()));
    }

    public static LedgerExportRow toLedgerExportRow(LedgerRowVO row) {
        LedgerExportRow export = new LedgerExportRow();
        export.setStudentNo(row.getStudentNo());
        export.setRealName(row.getRealName());
        export.setCollege(row.getCollege());
        export.setMajor(row.getMajor());
        export.setGrade(row.getGrade());
        export.setClassName(row.getClassName());
        export.setCheckinDays(row.getCheckinDays());
        export.setValidDays(row.getValidDays());
        export.setAbnormalDays(row.getAbnormalDays());
        export.setAvgSleepDuration(row.getAvgSleepDuration());
        export.setStayUpLateCount(row.getStayUpLateCount());
        export.setLateNightRate(row.getLateNightRate());
        export.setAvgExerciseDuration(row.getAvgExerciseDuration());
        export.setExerciseInsufficientCount(row.getExerciseInsufficientCount());
        export.setExerciseSufficientRate(row.getExerciseSufficientRate());
        export.setAvgMoodScore(row.getAvgMoodScore());
        export.setBreakfastRate(row.getBreakfastRate());
        export.setAvgWaterIntake(row.getAvgWaterIntake());
        export.setBmi(row.getBmi());
        export.setBmiLevel(row.getBmiLevel());
        export.setRiskLevel(row.getRiskLevel());
        return export;
    }

    public static CollegeExportRow toCollegeExportRow(CollegeDistributionVO row) {
        CollegeExportRow export = new CollegeExportRow();
        export.setCollege(row.getCollege());
        export.setStudentCount(row.getStudentCount());
        export.setCheckinStudents(row.getCheckinStudents());
        export.setCheckinRate(row.getCheckinRate());
        export.setAvgSleepDuration(row.getAvgSleepDuration());
        export.setLateNightRate(row.getLateNightRate());
        export.setExerciseSufficientRate(row.getExerciseSufficientRate());
        return export;
    }

    public static TrendExportRow toTrendExportRow(TrendPointVO point) {
        TrendExportRow export = new TrendExportRow();
        export.setDate(formatDate(point.getDate()));
        export.setCheckinStudents(point.getCheckinStudents());
        export.setCheckinRecords(point.getCheckinRecords());
        export.setAvgSleepDuration(point.getAvgSleepDuration());
        export.setStayUpLateCount(point.getStayUpLateCount());
        export.setAvgExerciseDuration(point.getAvgExerciseDuration());
        export.setAvgMoodScore(point.getAvgMoodScore());
        return export;
    }

    public static EmotionExportRow toEmotionExportRow(EmotionDistributionVO row) {
        EmotionExportRow export = new EmotionExportRow();
        export.setEmotionName(row.getEmotionName());
        export.setCount(row.getCount());
        export.setRate(row.getRate());
        return export;
    }

    public static RiskExportRow toRiskExportRow(RiskStudentVO row) {
        RiskExportRow export = new RiskExportRow();
        export.setStudentNo(row.getStudentNo());
        export.setRealName(row.getRealName());
        export.setCollege(row.getCollege());
        export.setGrade(row.getGrade());
        export.setClassName(row.getClassName());
        export.setCheckinDays(row.getCheckinDays());
        export.setStayUpLateCount(row.getStayUpLateCount());
        export.setExerciseInsufficientCount(row.getExerciseInsufficientCount());
        export.setAbnormalDays(row.getAbnormalDays());
        export.setRiskLevel(row.getRiskLevel());
        return export;
    }
}
