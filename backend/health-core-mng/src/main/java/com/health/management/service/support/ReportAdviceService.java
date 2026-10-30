package com.health.management.service.support;

import com.health.management.vo.report.MonthlyReportVO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 规则化健康建议。
 * 先使用确定性规则，后续 AI 健康助手可以复用同一份月度指标生成更个性化的建议。
 */
@Service
public class ReportAdviceService {

    private static final BigDecimal SLEEP_MIN = new BigDecimal("7.0");
    private static final BigDecimal EXERCISE_MIN = new BigDecimal("30");
    private static final BigDecimal BREAKFAST_RATE_MIN = new BigDecimal("50");
    private static final BigDecimal MOOD_MIN = new BigDecimal("3.0");
    private static final BigDecimal NORMAL_BMI_MIN = new BigDecimal("18.5");
    private static final BigDecimal NORMAL_BMI_MAX = new BigDecimal("24");

    public List<String> buildSuggestions(MonthlyReportVO report) {
        List<String> suggestions = new ArrayList<>();
        if (report == null) {
            return suggestions;
        }
        if (report.getCheckinDays() == null || report.getCheckinDays() == 0) {
            suggestions.add("本月暂无打卡数据，建议从今天开始记录睡眠、运动和情绪，便于形成连续的健康趋势。");
            return suggestions;
        }

        if (report.getAvgSleepDuration() != null
                && report.getAvgSleepDuration().compareTo(SLEEP_MIN) < 0) {
            suggestions.add("本月平均睡眠不足 7 小时，建议尽量保证 7-9 小时睡眠，固定起床时间，睡前减少手机使用。");
        }
        if (report.getStayUpLateCount() != null && report.getStayUpLateCount() > 0) {
            suggestions.add("本月有 " + report.getStayUpLateCount()
                    + " 次熬夜记录，建议逐步把入睡时间提前到 23:30 前，避免长期作息紊乱。");
        }
        if (report.getExerciseInsufficientCount() != null
                && report.getExerciseInsufficientCount() > 0) {
            suggestions.add("本月有 " + report.getExerciseInsufficientCount()
                    + " 次运动未达标，建议每周累计至少 150 分钟中等强度运动，可从每天快走 30 分钟开始。");
        } else if (report.getAvgExerciseDuration() != null
                && report.getAvgExerciseDuration().compareTo(EXERCISE_MIN) < 0) {
            suggestions.add("本月平均运动时长偏少，建议逐步增加到每天 30 分钟，注意运动前热身和运动后拉伸。");
        }
        if (report.getBreakfastRate() != null
                && report.getBreakfastRate().compareTo(BREAKFAST_RATE_MIN) < 0) {
            suggestions.add("本月早餐记录比例偏低，建议规律吃早餐，可选择全麦主食搭配鸡蛋、牛奶或豆制品。");
        }
        if (report.getAvgMoodScore() != null
                && report.getAvgMoodScore().compareTo(MOOD_MIN) < 0) {
            suggestions.add("本月平均心情评分偏低，建议适当安排放松和社交活动；如持续低落，可联系学校心理健康中心。");
        }
        if (report.getAbnormalDays() != null && report.getAbnormalDays() > 0) {
            suggestions.add("本月有 " + report.getAbnormalDays()
                    + " 天打卡数据被标记为异常，建议核对睡眠、运动和饮水数据，保证记录真实完整。");
        }
        if (report.getBmi() != null) {
            if (report.getBmi().compareTo(NORMAL_BMI_MIN) < 0) {
                suggestions.add("BMI 偏低，建议保证三餐能量和优质蛋白摄入，必要时咨询校医或营养师。");
            } else if (report.getBmi().compareTo(NORMAL_BMI_MAX) >= 0) {
                suggestions.add("BMI 高于正常范围，建议控制高油高糖食物，结合有氧运动和力量训练逐步调整体重。");
            }
        }
        if (suggestions.isEmpty()) {
            suggestions.add("本月健康数据整体良好，请继续保持规律作息、适量运动和均衡饮食。");
        }
        return suggestions.size() > 6 ? new ArrayList<>(suggestions.subList(0, 6)) : suggestions;
    }
}
