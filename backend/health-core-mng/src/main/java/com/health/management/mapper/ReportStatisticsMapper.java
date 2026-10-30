package com.health.management.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.health.management.vo.report.CollegeDistributionRow;
import com.health.management.vo.report.DashboardOverviewRow;
import com.health.management.vo.report.EmotionDistributionVO;
import com.health.management.vo.report.LedgerRowVO;
import com.health.management.vo.report.TrendPointVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 统计报表聚合查询。
 * 复杂 SQL 放在 resources/mapper/report/ReportStatisticsMapper.xml。
 */
@Mapper
public interface ReportStatisticsMapper {

    DashboardOverviewRow selectOverview(@Param("start") LocalDate start,
                                        @Param("end") LocalDate end,
                                        @Param("college") String college,
                                        @Param("grade") String grade);

    List<TrendPointVO> selectTrend(@Param("start") LocalDate start,
                                   @Param("end") LocalDate end,
                                   @Param("college") String college,
                                   @Param("grade") String grade);

    List<CollegeDistributionRow> selectCollegeDistribution(@Param("start") LocalDate start,
                                                           @Param("end") LocalDate end,
                                                           @Param("college") String college,
                                                           @Param("grade") String grade);

    List<EmotionDistributionVO> selectEmotionDistribution(@Param("start") LocalDate start,
                                                          @Param("end") LocalDate end,
                                                          @Param("college") String college,
                                                          @Param("grade") String grade);

    List<LedgerRowVO> selectRiskRows(@Param("start") LocalDate start,
                                     @Param("end") LocalDate end,
                                     @Param("college") String college,
                                     @Param("grade") String grade);

    IPage<LedgerRowVO> selectLedgerPage(Page<LedgerRowVO> page,
                                        @Param("start") LocalDate start,
                                        @Param("end") LocalDate end,
                                        @Param("college") String college,
                                        @Param("grade") String grade,
                                        @Param("className") String className,
                                        @Param("keyword") String keyword);

    List<LedgerRowVO> selectLedgerList(@Param("start") LocalDate start,
                                       @Param("end") LocalDate end,
                                       @Param("college") String college,
                                       @Param("grade") String grade,
                                       @Param("className") String className,
                                       @Param("keyword") String keyword);

    List<String> selectColleges();

    List<String> selectGrades();

    List<String> selectClasses();
}
