package com.health.management.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.health.management.common.constant.RoleConstants;
import com.health.management.common.exception.ServiceException;
import com.health.management.common.util.ValidateUtils;
import com.health.management.convert.HealthConverter;
import com.health.management.dto.report.DashboardQueryDTO;
import com.health.management.dto.report.LedgerQueryDTO;
import com.health.management.dto.report.ReportHistoryQueryDTO;
import com.health.management.entity.HealthCheckin;
import com.health.management.entity.HealthPhysicalTest;
import com.health.management.entity.HealthProfile;
import com.health.management.entity.HealthReport;
import com.health.management.entity.SysUser;
import com.health.management.mapper.HealthCheckinMapper;
import com.health.management.mapper.HealthPhysicalTestMapper;
import com.health.management.mapper.HealthProfileMapper;
import com.health.management.mapper.HealthReportMapper;
import com.health.management.mapper.ReportStatisticsMapper;
import com.health.management.security.SecurityUtils;
import com.health.management.service.HealthReportService;
import com.health.management.service.support.HealthUserSupport;
import com.health.management.service.support.ReportAdviceService;
import com.health.management.util.HealthMetricUtils;
import com.health.management.vo.report.CollegeDistributionRow;
import com.health.management.vo.report.CollegeDistributionVO;
import com.health.management.vo.report.DailyHealthPointVO;
import com.health.management.vo.report.DashboardOverviewRow;
import com.health.management.vo.report.DashboardOverviewVO;
import com.health.management.vo.report.DashboardVO;
import com.health.management.vo.report.EmotionDistributionVO;
import com.health.management.vo.report.FilterOptionsVO;
import com.health.management.vo.report.HealthReportHistoryVO;
import com.health.management.vo.report.LedgerRowVO;
import com.health.management.vo.report.MonthlyReportVO;
import com.health.management.vo.report.RiskStudentVO;
import com.health.management.vo.report.TrendPointVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 统计报表业务实现。
 */
@Service
public class HealthReportServiceImpl extends ServiceImpl<HealthReportMapper, HealthReport>
        implements HealthReportService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final int RISK_LEVEL_HIGH = 5;

    @Autowired
    private HealthCheckinMapper healthCheckinMapper;

    @Autowired
    private HealthProfileMapper healthProfileMapper;

    @Autowired
    private HealthPhysicalTestMapper healthPhysicalTestMapper;

    @Autowired
    private ReportStatisticsMapper reportStatisticsMapper;

    @Autowired
    private HealthUserSupport healthUserSupport;

    @Autowired
    private ReportAdviceService reportAdviceService;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MonthlyReportVO getMonthlyReport(String month) {
        SysUser user = healthUserSupport.requireStudent();
        YearMonth yearMonth = parseMonth(month);
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();

        List<HealthCheckin> checkins = healthCheckinMapper.selectList(
                new LambdaQueryWrapper<HealthCheckin>()
                        .eq(HealthCheckin::getUserId, user.getId())
                        .ge(HealthCheckin::getCheckinDate, start)
                        .le(HealthCheckin::getCheckinDate, end)
                        .orderByAsc(HealthCheckin::getCheckinDate));

        HealthProfile profile = healthProfileMapper.selectOne(
                new LambdaQueryWrapper<HealthProfile>()
                        .eq(HealthProfile::getUserId, user.getId()));

        HealthPhysicalTest latestTest = healthPhysicalTestMapper.selectOne(
                new LambdaQueryWrapper<HealthPhysicalTest>()
                        .eq(HealthPhysicalTest::getUserId, user.getId())
                        .orderByDesc(HealthPhysicalTest::getTestYear)
                        .orderByDesc(HealthPhysicalTest::getTestDate)
                        .orderByDesc(HealthPhysicalTest::getId)
                        .last("LIMIT 1"));

        MonthlyReportVO report = buildMonthlyReport(user, profile, checkins, latestTest, yearMonth);
        report.setGenerateTime(LocalDateTime.now());
        report.setSummary(buildSummary(report));
        upsertReport(report, user);
        return report;
    }

    @Override
    public IPage<HealthReportHistoryVO> pageHistory(ReportHistoryQueryDTO query) {
        SysUser user = healthUserSupport.requireStudent();
        ReportHistoryQueryDTO q = query == null ? new ReportHistoryQueryDTO() : query;
        ValidateUtils.requirePage(q.getPage(), q.getSize());
        int current = q.getPage() == null ? 1 : q.getPage();
        int size = q.getSize() == null ? 10 : q.getSize();

        IPage<HealthReport> page = page(new Page<>(current, size),
                new LambdaQueryWrapper<HealthReport>()
                        .eq(HealthReport::getUserId, user.getId())
                        .orderByDesc(HealthReport::getReportYear)
                        .orderByDesc(HealthReport::getReportMonth));
        Page<HealthReportHistoryVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<HealthReportHistoryVO> records = new ArrayList<>();
        for (HealthReport report : page.getRecords()) {
            HealthReportHistoryVO vo = new HealthReportHistoryVO();
            vo.setId(report.getId());
            vo.setMonth(formatMonth(report.getReportYear(), report.getReportMonth()));
            vo.setReportTitle(report.getReportTitle());
            vo.setCheckinDays(report.getCheckinDays());
            vo.setAvgSleepDuration(report.getAvgSleepDuration());
            vo.setStayUpLateCount(report.getStayUpLateCount());
            vo.setAvgExerciseDuration(report.getAvgExerciseDuration());
            vo.setExerciseInsufficientCount(report.getExerciseInsufficientCount());
            vo.setAvgMoodScore(report.getAvgMoodScore());
            vo.setGenerateTime(report.getGenerateTime());
            records.add(vo);
        }
        result.setRecords(records);
        return result;
    }

    @Override
    public DashboardVO getDashboard(DashboardQueryDTO query) {
        requireManager();
        DashboardQueryDTO q = query == null ? new DashboardQueryDTO() : query;
        YearMonth yearMonth = parseMonth(q.getMonth());
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();
        LocalDate effectiveEnd = end.isAfter(LocalDate.now()) ? LocalDate.now() : end;
        String college = trimToNull(q.getCollege());
        String grade = trimToNull(q.getGrade());

        DashboardVO dashboard = new DashboardVO();
        dashboard.setMonth(formatMonth(yearMonth));
        dashboard.setCollege(college);
        dashboard.setGrade(grade);
        dashboard.setOverview(toOverview(reportStatisticsMapper.selectOverview(
                start, effectiveEnd, college, grade)));
        dashboard.setTrend(fillTrend(reportStatisticsMapper.selectTrend(
                start, effectiveEnd, college, grade), start, effectiveEnd));
        dashboard.setCollegeDistribution(toCollegeDistribution(
                reportStatisticsMapper.selectCollegeDistribution(start, effectiveEnd, college, grade)));
        dashboard.setEmotionDistribution(toEmotionDistribution(
                reportStatisticsMapper.selectEmotionDistribution(start, effectiveEnd, college, grade)));
        dashboard.setRiskStudents(toRiskStudents(reportStatisticsMapper.selectRiskRows(
                start, effectiveEnd, college, grade)));
        return dashboard;
    }

    @Override
    public IPage<LedgerRowVO> pageLedger(LedgerQueryDTO query) {
        requireManager();
        LedgerQueryDTO q = query == null ? new LedgerQueryDTO() : query;
        ValidateUtils.requirePage(q.getPage(), q.getSize());
        int current = q.getPage() == null ? 1 : q.getPage();
        int size = q.getSize() == null ? 10 : q.getSize();
        YearMonth yearMonth = parseMonth(q.getMonth());
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();
        LocalDate effectiveEnd = end.isAfter(LocalDate.now()) ? LocalDate.now() : end;

        IPage<LedgerRowVO> page = reportStatisticsMapper.selectLedgerPage(
                new Page<>(current, size), start, effectiveEnd,
                trimToNull(q.getCollege()), trimToNull(q.getGrade()),
                trimToNull(q.getClassName()), trimToNull(q.getKeyword()));
        boolean fullIdentity = SecurityUtils.hasRole(RoleConstants.SUPER_ADMIN);
        for (LedgerRowVO row : page.getRecords()) {
            decorateLedgerRow(row, fullIdentity);
        }
        return page;
    }

    @Override
    public List<LedgerRowVO> listLedger(LedgerQueryDTO query) {
        requireManager();
        LedgerQueryDTO q = query == null ? new LedgerQueryDTO() : query;
        YearMonth yearMonth = parseMonth(q.getMonth());
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();
        LocalDate effectiveEnd = end.isAfter(LocalDate.now()) ? LocalDate.now() : end;
        List<LedgerRowVO> rows = reportStatisticsMapper.selectLedgerList(
                start, effectiveEnd, trimToNull(q.getCollege()), trimToNull(q.getGrade()),
                trimToNull(q.getClassName()), trimToNull(q.getKeyword()));
        boolean fullIdentity = SecurityUtils.hasRole(RoleConstants.SUPER_ADMIN);
        for (LedgerRowVO row : rows) {
            decorateLedgerRow(row, fullIdentity);
        }
        return rows;
    }

    @Override
    public FilterOptionsVO getFilterOptions() {
        requireManager();
        FilterOptionsVO vo = new FilterOptionsVO();
        vo.setColleges(reportStatisticsMapper.selectColleges());
        vo.setGrades(reportStatisticsMapper.selectGrades());
        vo.setClasses(reportStatisticsMapper.selectClasses());
        return vo;
    }

    private MonthlyReportVO buildMonthlyReport(SysUser user, HealthProfile profile,
                                                List<HealthCheckin> checkins,
                                                HealthPhysicalTest latestTest, YearMonth yearMonth) {
        MonthlyReportVO report = new MonthlyReportVO();
        report.setMonth(formatMonth(yearMonth));
        report.setUserId(user.getId());
        report.setStudentNo(user.getStudentNo());
        report.setRealName(user.getRealName());
        report.setCollege(profile != null && ValidateUtils.hasText(profile.getCollege())
                ? profile.getCollege() : user.getDeptName());
        if (profile != null) {
            report.setMajor(profile.getMajor());
            report.setGrade(profile.getGrade());
            report.setClassName(profile.getClassName());
        }

        int validDays = 0;
        int abnormalDays = 0;
        int sleepCount = 0;
        int lateNightCount = 0;
        int sleepFilledCount = 0;
        int exerciseCount = 0;
        int exerciseInsufficientCount = 0;
        int exerciseSufficientCount = 0;
        int exerciseFilledCount = 0;
        int moodCount = 0;
        int breakfastCount = 0;
        int breakfastFilledCount = 0;
        int waterCount = 0;
        BigDecimal sleepSum = BigDecimal.ZERO;
        BigDecimal exerciseSum = BigDecimal.ZERO;
        BigDecimal moodSum = BigDecimal.ZERO;
        BigDecimal waterSum = BigDecimal.ZERO;
        Set<LocalDate> distinctDays = new HashSet<>();
        List<DailyHealthPointVO> daily = new ArrayList<>();

        for (HealthCheckin checkin : checkins) {
            if (checkin.getCheckinDate() != null) {
                distinctDays.add(checkin.getCheckinDate());
            }
            boolean valid = Integer.valueOf(0).equals(checkin.getDataQuality());
            if (valid) {
                validDays++;
            } else if (Integer.valueOf(1).equals(checkin.getDataQuality())) {
                abnormalDays++;
            }

            DailyHealthPointVO point = new DailyHealthPointVO();
            point.setDate(checkin.getCheckinDate());
            point.setSleepTime(checkin.getSleepTime());
            point.setSleepDuration(checkin.getSleepDuration());
            point.setStayUpLate(checkin.getStayUpLate());
            point.setExerciseDuration(checkin.getExerciseDuration());
            point.setExerciseSufficient(checkin.getExerciseSufficient());
            point.setDietBreakfast(checkin.getDietBreakfast());
            point.setWaterIntake(checkin.getWaterIntake());
            point.setEmotionType(checkin.getEmotionType());
            point.setMoodScore(checkin.getMoodScore());
            point.setDataQuality(checkin.getDataQuality());
            daily.add(point);

            if (!valid) {
                continue;
            }
            if (checkin.getSleepDuration() != null) {
                sleepSum = sleepSum.add(checkin.getSleepDuration());
                sleepCount++;
            }
            if (checkin.getStayUpLate() != null) {
                sleepFilledCount++;
                if (Integer.valueOf(1).equals(checkin.getStayUpLate())) {
                    lateNightCount++;
                }
            }
            if (checkin.getExerciseDuration() != null) {
                exerciseSum = exerciseSum.add(BigDecimal.valueOf(checkin.getExerciseDuration()));
                exerciseCount++;
            }
            if (checkin.getExerciseSufficient() != null) {
                exerciseFilledCount++;
                if (Integer.valueOf(0).equals(checkin.getExerciseSufficient())) {
                    exerciseInsufficientCount++;
                } else if (Integer.valueOf(1).equals(checkin.getExerciseSufficient())) {
                    exerciseSufficientCount++;
                }
            }
            if (checkin.getMoodScore() != null) {
                moodSum = moodSum.add(BigDecimal.valueOf(checkin.getMoodScore()));
                moodCount++;
            }
            if (checkin.getDietBreakfast() != null) {
                breakfastFilledCount++;
                if (Integer.valueOf(1).equals(checkin.getDietBreakfast())) {
                    breakfastCount++;
                }
            }
            if (checkin.getWaterIntake() != null) {
                waterSum = waterSum.add(BigDecimal.valueOf(checkin.getWaterIntake()));
                waterCount++;
            }
        }

        report.setCheckinDays(distinctDays.size());
        report.setValidDays(validDays);
        report.setAbnormalDays(abnormalDays);
        report.setAvgSleepDuration(average(sleepSum, sleepCount, 1));
        report.setStayUpLateCount(lateNightCount);
        report.setLateNightRate(rate(lateNightCount, sleepFilledCount));
        report.setAvgExerciseDuration(average(exerciseSum, exerciseCount, 1));
        report.setExerciseInsufficientCount(exerciseInsufficientCount);
        report.setExerciseSufficientRate(rate(exerciseSufficientCount, exerciseFilledCount));
        report.setAvgMoodScore(average(moodSum, moodCount, 1));
        report.setBreakfastRate(rate(breakfastCount, breakfastFilledCount));
        report.setAvgWaterIntake(average(waterSum, waterCount, 0));

        BigDecimal bmi = null;
        if (profile != null) {
            bmi = profile.getBmi();
            if (bmi == null) {
                bmi = HealthMetricUtils.calcBmi(profile.getHeight(), profile.getWeight());
            }
        }
        report.setBmi(bmi);
        report.setBmiLevel(HealthMetricUtils.bmiLevel(bmi));
        if (latestTest != null) {
            report.setLatestPhysicalScore(latestTest.getTotalScore());
            report.setLatestPhysicalGradeLevel(latestTest.getGradeLevel());
        }
        report.setDaily(daily);
        report.setSuggestions(reportAdviceService.buildSuggestions(report));
        return report;
    }

    private String buildSummary(MonthlyReportVO report) {
        StringBuilder sb = new StringBuilder();
        sb.append(report.getMonth()).append(" 健康统计报告\n");
        sb.append("学生：").append(nullToDash(report.getRealName()))
                .append("（").append(nullToDash(report.getStudentNo())).append("）\n");
        sb.append("学院：").append(nullToDash(report.getCollege()))
                .append("；专业：").append(nullToDash(report.getMajor()))
                .append("；班级：").append(nullToDash(report.getClassName())).append("\n");
        sb.append("打卡天数：").append(report.getCheckinDays())
                .append("（有效 ").append(report.getValidDays())
                .append(" 天，异常 ").append(report.getAbnormalDays()).append(" 天）\n");
        sb.append("平均睡眠：").append(formatNumber(report.getAvgSleepDuration())).append(" 小时")
                .append("；熬夜次数：").append(report.getStayUpLateCount()).append("\n");
        sb.append("平均运动：").append(formatNumber(report.getAvgExerciseDuration())).append(" 分钟")
                .append("；运动不足次数：").append(report.getExerciseInsufficientCount()).append("\n");
        sb.append("平均心情评分：").append(formatNumber(report.getAvgMoodScore())).append(" 分")
                .append("；早餐率：").append(formatPercent(report.getBreakfastRate())).append("\n");
        sb.append("\n健康建议：\n");
        List<String> suggestions = report.getSuggestions();
        for (int i = 0; i < suggestions.size(); i++) {
            sb.append(i + 1).append(". ").append(suggestions.get(i)).append("\n");
        }
        return sb.toString();
    }

    private void upsertReport(MonthlyReportVO report, SysUser user) {
        LambdaQueryWrapper<HealthReport> wrapper = new LambdaQueryWrapper<HealthReport>()
                .eq(HealthReport::getUserId, user.getId())
                .eq(HealthReport::getReportYear, Integer.parseInt(report.getMonth().substring(0, 4)))
                .eq(HealthReport::getReportMonth, Integer.parseInt(report.getMonth().substring(5, 7)));
        HealthReport existing = getOne(wrapper, false);
        HealthReport entity = existing == null ? new HealthReport() : existing;
        entity.setUserId(user.getId());
        entity.setStudentNo(user.getStudentNo());
        entity.setReportYear(Integer.parseInt(report.getMonth().substring(0, 4)));
        entity.setReportMonth(Integer.parseInt(report.getMonth().substring(5, 7)));
        entity.setReportTitle(report.getMonth().replace("-", "年") + "月健康统计报告");
        entity.setCheckinDays(report.getCheckinDays());
        entity.setAvgSleepDuration(report.getAvgSleepDuration());
        entity.setStayUpLateCount(report.getStayUpLateCount());
        entity.setAvgExerciseDuration(report.getAvgExerciseDuration() == null
                ? 0 : report.getAvgExerciseDuration().setScale(0, RoundingMode.HALF_UP).intValue());
        entity.setExerciseInsufficientCount(report.getExerciseInsufficientCount());
        entity.setAvgMoodScore(report.getAvgMoodScore());
        entity.setReportContent(report.getSummary());
        entity.setGenerateTime(report.getGenerateTime());
        entity.setFileUrl(null);
        entity.setRemark("系统自动生成");
        try {
            entity.setReportData(objectMapper.writeValueAsString(report));
        } catch (JsonProcessingException e) {
            throw ServiceException.fail("报告数据序列化失败");
        }
        if (existing == null) {
            try {
                save(entity);
            } catch (DuplicateKeyException e) {
                HealthReport concurrent = getOne(wrapper, false);
                if (concurrent == null) {
                    throw e;
                }
                entity.setId(concurrent.getId());
                updateById(entity);
            }
        } else {
            updateById(entity);
        }
    }

    private DashboardOverviewVO toOverview(DashboardOverviewRow row) {
        DashboardOverviewVO vo = new DashboardOverviewVO();
        if (row == null) {
            vo.setStudentTotal(0L);
            vo.setCheckinStudents(0L);
            vo.setCheckinRecords(0L);
            vo.setStayUpLateCount(0L);
            vo.setAbnormalRecords(0L);
            return vo;
        }
        long studentTotal = nvl(row.getStudentTotal());
        long checkinStudents = nvl(row.getCheckinStudents());
        long checkinRecords = nvl(row.getCheckinRecords());
        long stayUpLateCount = nvl(row.getStayUpLateCount());
        long sleepFilled = nvl(row.getSleepFilledCount());
        long exerciseSufficient = nvl(row.getExerciseSufficientCount());
        long exerciseFilled = nvl(row.getExerciseFilledCount());
        long breakfastCount = nvl(row.getBreakfastCount());
        long breakfastFilled = nvl(row.getBreakfastFilledCount());
        vo.setStudentTotal(studentTotal);
        vo.setCheckinStudents(checkinStudents);
        vo.setCheckinRate(rate(checkinStudents, studentTotal));
        vo.setCheckinRecords(checkinRecords);
        vo.setAvgSleepDuration(round(row.getAvgSleepDuration(), 1));
        vo.setStayUpLateCount(stayUpLateCount);
        vo.setLateNightRate(rate(stayUpLateCount, sleepFilled));
        vo.setAvgExerciseDuration(round(row.getAvgExerciseDuration(), 1));
        vo.setExerciseSufficientRate(rate(exerciseSufficient, exerciseFilled));
        vo.setAvgMoodScore(round(row.getAvgMoodScore(), 1));
        vo.setBreakfastRate(rate(breakfastCount, breakfastFilled));
        vo.setAbnormalRecords(nvl(row.getAbnormalRecords()));
        return vo;
    }

    private List<TrendPointVO> fillTrend(List<TrendPointVO> rows, LocalDate start, LocalDate end) {
        Map<LocalDate, TrendPointVO> indexed = new LinkedHashMap<>();
        if (rows != null) {
            for (TrendPointVO row : rows) {
                if (row.getDate() != null) {
                    indexed.put(row.getDate(), row);
                }
            }
        }
        List<TrendPointVO> result = new ArrayList<>();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            TrendPointVO point = indexed.get(date);
            if (point == null) {
                point = new TrendPointVO();
                point.setDate(date);
                point.setCheckinStudents(0L);
                point.setCheckinRecords(0L);
                point.setStayUpLateCount(0L);
            } else {
                point.setAvgSleepDuration(round(point.getAvgSleepDuration(), 1));
                point.setAvgExerciseDuration(round(point.getAvgExerciseDuration(), 1));
                point.setAvgMoodScore(round(point.getAvgMoodScore(), 1));
            }
            result.add(point);
        }
        return result;
    }

    private List<CollegeDistributionVO> toCollegeDistribution(List<CollegeDistributionRow> rows) {
        List<CollegeDistributionVO> result = new ArrayList<>();
        if (rows == null) {
            return result;
        }
        for (CollegeDistributionRow row : rows) {
            CollegeDistributionVO vo = new CollegeDistributionVO();
            long studentCount = nvl(row.getStudentCount());
            long checkinStudents = nvl(row.getCheckinStudents());
            vo.setCollege(row.getCollege());
            vo.setStudentCount(studentCount);
            vo.setCheckinStudents(checkinStudents);
            vo.setCheckinRate(rate(checkinStudents, studentCount));
            vo.setAvgSleepDuration(round(row.getAvgSleepDuration(), 1));
            vo.setLateNightRate(rate(nvl(row.getStayUpLateCount()), nvl(row.getSleepFilledCount())));
            vo.setExerciseSufficientRate(rate(nvl(row.getExerciseSufficientCount()),
                    nvl(row.getExerciseFilledCount())));
            result.add(vo);
        }
        return result;
    }

    private List<EmotionDistributionVO> toEmotionDistribution(List<EmotionDistributionVO> rows) {
        List<EmotionDistributionVO> result = rows == null ? new ArrayList<>() : rows;
        long total = 0L;
        for (EmotionDistributionVO row : result) {
            total += nvl(row.getCount());
        }
        for (EmotionDistributionVO row : result) {
            row.setRate(rate(nvl(row.getCount()), total));
        }
        return result;
    }

    private List<RiskStudentVO> toRiskStudents(List<LedgerRowVO> rows) {
        List<RiskStudentVO> result = new ArrayList<>();
        if (rows == null) {
            return result;
        }
        boolean fullIdentity = SecurityUtils.hasRole(RoleConstants.SUPER_ADMIN);
        for (LedgerRowVO row : rows) {
            RiskStudentVO vo = new RiskStudentVO();
            vo.setUserId(row.getUserId());
            vo.setStudentNo(fullIdentity ? row.getStudentNo() : HealthConverter.maskStudentNo(row.getStudentNo()));
            vo.setRealName(fullIdentity ? row.getRealName() : HealthConverter.maskName(row.getRealName()));
            vo.setCollege(row.getCollege());
            vo.setGrade(row.getGrade());
            vo.setClassName(row.getClassName());
            vo.setCheckinDays(row.getCheckinDays());
            vo.setStayUpLateCount(row.getStayUpLateCount());
            vo.setExerciseInsufficientCount(row.getExerciseInsufficientCount());
            vo.setAbnormalDays(row.getAbnormalDays());
            vo.setRiskLevel(riskLevel(row));
            result.add(vo);
        }
        return result;
    }

    private void decorateLedgerRow(LedgerRowVO row, boolean fullIdentity) {
        row.setAvgSleepDuration(round(row.getAvgSleepDuration(), 1));
        row.setAvgExerciseDuration(round(row.getAvgExerciseDuration(), 1));
        row.setAvgMoodScore(round(row.getAvgMoodScore(), 1));
        row.setAvgWaterIntake(round(row.getAvgWaterIntake(), 0));
        row.setLateNightRate(rate(nvl(row.getStayUpLateCount()), nvl(row.getSleepFilledCount())));
        row.setExerciseSufficientRate(rate(nvl(row.getExerciseSufficientCount()),
                nvl(row.getExerciseFilledCount())));
        row.setBreakfastRate(rate(nvl(row.getBreakfastCount()), nvl(row.getBreakfastFilledCount())));
        row.setBmiLevel(HealthMetricUtils.bmiLevel(row.getBmi()));
        row.setRiskLevel(riskLevel(row));
        if (!fullIdentity) {
            row.setStudentNo(HealthConverter.maskStudentNo(row.getStudentNo()));
            row.setRealName(HealthConverter.maskName(row.getRealName()));
        }
    }

    private String riskLevel(LedgerRowVO row) {
        int lateNight = row.getStayUpLateCount() == null ? 0 : row.getStayUpLateCount();
        int exerciseInsufficient = row.getExerciseInsufficientCount() == null
                ? 0 : row.getExerciseInsufficientCount();
        int abnormal = row.getAbnormalDays() == null ? 0 : row.getAbnormalDays();
        if (lateNight >= RISK_LEVEL_HIGH || exerciseInsufficient >= RISK_LEVEL_HIGH * 2
                || abnormal >= RISK_LEVEL_HIGH) {
            return "较高";
        }
        return "需关注";
    }

    private YearMonth parseMonth(String month) {
        YearMonth yearMonth;
        if (!ValidateUtils.hasText(month)) {
            yearMonth = YearMonth.now();
        } else {
            try {
                yearMonth = YearMonth.parse(month.trim());
            } catch (DateTimeParseException e) {
                throw ServiceException.badRequest("月份格式应为 yyyy-MM");
            }
        }
        if (yearMonth.isAfter(YearMonth.now())) {
            throw ServiceException.badRequest("不能生成未来月份的统计报表");
        }
        return yearMonth;
    }

    private void requireManager() {
        if (!SecurityUtils.isDoctorOrAdmin()) {
            throw ServiceException.forbidden("仅校医或超级管理员可查看统计报表");
        }
    }

    private BigDecimal average(BigDecimal sum, int count, int scale) {
        if (count <= 0 || sum == null) {
            return null;
        }
        return sum.divide(BigDecimal.valueOf(count), scale, RoundingMode.HALF_UP);
    }

    private BigDecimal rate(long numerator, long denominator) {
        if (denominator <= 0) {
            return null;
        }
        return BigDecimal.valueOf(numerator)
                .multiply(HUNDRED)
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal round(BigDecimal value, int scale) {
        return value == null ? null : value.setScale(scale, RoundingMode.HALF_UP);
    }

    private long nvl(Number value) {
        return value == null ? 0L : value.longValue();
    }

    private String formatMonth(YearMonth yearMonth) {
        return String.format("%04d-%02d", yearMonth.getYear(), yearMonth.getMonthValue());
    }

    private String formatMonth(Integer year, Integer month) {
        return String.format("%04d-%02d", year == null ? 0 : year, month == null ? 0 : month);
    }

    private String formatNumber(BigDecimal value) {
        return value == null ? "--" : value.stripTrailingZeros().toPlainString();
    }

    private String formatPercent(BigDecimal value) {
        return value == null ? "--" : value.stripTrailingZeros().toPlainString() + "%";
    }

    private String nullToDash(String value) {
        return ValidateUtils.hasText(value) ? value : "--";
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
