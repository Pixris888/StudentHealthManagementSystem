package com.health.management.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.exception.ExcelDataConvertException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.health.management.common.constant.RoleConstants;
import com.health.management.common.exception.ServiceException;
import com.health.management.common.util.ValidateUtils;
import com.health.management.convert.HealthConverter;
import com.health.management.dto.HealthPhysicalTestQueryDTO;
import com.health.management.dto.HealthPhysicalTestSaveDTO;
import com.health.management.dto.PhysicalTestImportRow;
import com.health.management.entity.HealthPhysicalTest;
import com.health.management.entity.SysUser;
import com.health.management.mapper.HealthPhysicalTestMapper;
import com.health.management.security.SecurityUtils;
import com.health.management.service.HealthPhysicalTestService;
import com.health.management.service.support.HealthUserSupport;
import com.health.management.util.HealthMetricUtils;
import com.health.management.vo.HealthPhysicalTestVO;
import com.health.management.vo.PhysicalTestImportResultVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * 体测成绩业务实现：个人录入、Excel 导入、趋势查询和校医脱敏查询。
 */
@Service
public class HealthPhysicalTestServiceImpl extends ServiceImpl<HealthPhysicalTestMapper, HealthPhysicalTest>
        implements HealthPhysicalTestService {

    /** 单次导入最大行数，防止超大文件拖垮服务 */
    private static final int MAX_IMPORT_ROWS = 500;

    /** 导入失败原因最多返回条数 */
    private static final int MAX_ERROR_MESSAGES = 20;

    /** Excel 中可能出现的日期文本格式 */
    private static final DateTimeFormatter[] DATE_FORMATS = new DateTimeFormatter[]{
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy-M-d"),
            DateTimeFormatter.ofPattern("yyyy/M/d"),
            DateTimeFormatter.ofPattern("yyyy.M.d"),
            DateTimeFormatter.ofPattern("yyyy年M月d日"),
            DateTimeFormatter.ofPattern("yyyyMMdd")
    };

    @Autowired
    private HealthUserSupport healthUserSupport;

    @Override
    public IPage<HealthPhysicalTestVO> pageMine(HealthPhysicalTestQueryDTO query) {
        SysUser user = healthUserSupport.requireStudent();
        if (query == null) {
            query = new HealthPhysicalTestQueryDTO();
        }
        ValidateUtils.requirePage(query.getPage(), query.getSize());
        int current = query.getPage() == null ? 1 : query.getPage();
        int size = query.getSize() == null ? 10 : query.getSize();

        LambdaQueryWrapper<HealthPhysicalTest> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HealthPhysicalTest::getUserId, user.getId());
        if (query.getTestYear() != null) {
            wrapper.eq(HealthPhysicalTest::getTestYear, query.getTestYear());
        }
        String term = trimToNull(query.getTestTerm());
        if (term != null) {
            wrapper.eq(HealthPhysicalTest::getTestTerm, term);
        }
        wrapper.orderByDesc(HealthPhysicalTest::getTestYear)
                .orderByDesc(HealthPhysicalTest::getId);

        IPage<HealthPhysicalTest> page = page(new Page<>(current, size), wrapper);
        Page<HealthPhysicalTestVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<HealthPhysicalTestVO> records = new ArrayList<>();
        for (HealthPhysicalTest test : page.getRecords()) {
            records.add(HealthConverter.toPhysicalTestVO(test, false));
        }
        voPage.setRecords(records);
        return voPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HealthPhysicalTestVO addMine(HealthPhysicalTestSaveDTO dto) {
        SysUser user = healthUserSupport.requireStudent();
        ValidateUtils.requireNotNull(dto, "请求参数");
        String term = normalizeTerm(dto.getTestTerm());
        validatePhysicalValues(dto.getTestYear(), term, dto.getHeight(), dto.getWeight(),
                dto.getVitalCapacity(), dto.getRun50m(), dto.getSitAndReach(), dto.getStandingJump(),
                dto.getEnduranceRun(), dto.getStrengthItem(), dto.getTotalScore(),
                dto.getGradeLevel(), dto.getTestDate());
        if (findByUnique(user.getId(), dto.getTestYear(), term) != null) {
            throw ServiceException.conflict("该学年学期的体测成绩已存在，请直接修改");
        }

        HealthPhysicalTest test = new HealthPhysicalTest();
        test.setUserId(user.getId());
        test.setStudentNo(user.getStudentNo());
        applyDto(test, dto, term);
        save(test);
        return HealthConverter.toPhysicalTestVO(test, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HealthPhysicalTestVO updateMine(Long id, HealthPhysicalTestSaveDTO dto) {
        SysUser user = healthUserSupport.requireStudent();
        ValidateUtils.requireNotNull(id, "记录ID");
        ValidateUtils.requireNotNull(dto, "请求参数");

        HealthPhysicalTest existing = getById(id);
        if (existing == null || !user.getId().equals(existing.getUserId())) {
            throw ServiceException.notFound("体测记录不存在");
        }
        String term = normalizeTerm(dto.getTestTerm());
        validatePhysicalValues(dto.getTestYear(), term, dto.getHeight(), dto.getWeight(),
                dto.getVitalCapacity(), dto.getRun50m(), dto.getSitAndReach(), dto.getStandingJump(),
                dto.getEnduranceRun(), dto.getStrengthItem(), dto.getTotalScore(),
                dto.getGradeLevel(), dto.getTestDate());

        HealthPhysicalTest duplicate = findByUnique(user.getId(), dto.getTestYear(), term);
        if (duplicate != null && !duplicate.getId().equals(id)) {
            throw ServiceException.conflict("该学年学期的体测成绩已存在，请勿重复录入");
        }

        HealthPhysicalTest updated = new HealthPhysicalTest();
        applyDto(updated, dto, term);
        updated.setStudentNo(user.getStudentNo());
        Long operatorId = SecurityUtils.getUserId();
        LocalDateTime now = LocalDateTime.now();

        LambdaUpdateWrapper<HealthPhysicalTest> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(HealthPhysicalTest::getId, id)
                .set(HealthPhysicalTest::getStudentNo, updated.getStudentNo())
                .set(HealthPhysicalTest::getTestYear, updated.getTestYear())
                .set(HealthPhysicalTest::getTestTerm, updated.getTestTerm())
                .set(HealthPhysicalTest::getHeight, updated.getHeight())
                .set(HealthPhysicalTest::getWeight, updated.getWeight())
                .set(HealthPhysicalTest::getBmi, updated.getBmi())
                .set(HealthPhysicalTest::getVitalCapacity, updated.getVitalCapacity())
                .set(HealthPhysicalTest::getRun50m, updated.getRun50m())
                .set(HealthPhysicalTest::getSitAndReach, updated.getSitAndReach())
                .set(HealthPhysicalTest::getStandingJump, updated.getStandingJump())
                .set(HealthPhysicalTest::getEnduranceRun, updated.getEnduranceRun())
                .set(HealthPhysicalTest::getStrengthItem, updated.getStrengthItem())
                .set(HealthPhysicalTest::getTotalScore, updated.getTotalScore())
                .set(HealthPhysicalTest::getGradeLevel, updated.getGradeLevel())
                .set(HealthPhysicalTest::getTestDate, updated.getTestDate())
                .set(HealthPhysicalTest::getRemark, updated.getRemark())
                .set(HealthPhysicalTest::getUpdateBy, operatorId)
                .set(HealthPhysicalTest::getUpdateTime, now);
        update(wrapper);

        HealthPhysicalTest fresh = getById(id);
        return HealthConverter.toPhysicalTestVO(fresh, false);
    }

    @Override
    public PhysicalTestImportResultVO importMine(MultipartFile file) {
        SysUser user = healthUserSupport.requireStudent();
        validateImportFile(file);

        PhysicalTestImportResultVO result = new PhysicalTestImportResultVO();
        try (InputStream inputStream = file.getInputStream()) {
            EasyExcel.read(inputStream, PhysicalTestImportRow.class,
                            new PhysicalTestImportListener(user, result))
                    .sheet()
                    .headRowNumber(1)
                    .doRead();
        } catch (IOException e) {
            throw ServiceException.fail("读取 Excel 文件失败：" + e.getMessage());
        } catch (ServiceException e) {
            throw e;
        } catch (RuntimeException e) {
            throw ServiceException.badRequest("Excel 文件解析失败，请检查文件格式和表头");
        }
        return result;
    }

    @Override
    public List<HealthPhysicalTestVO> trendMine(Integer startYear, Integer endYear) {
        SysUser user = healthUserSupport.requireStudent();
        checkYear(startYear, "开始年份");
        checkYear(endYear, "结束年份");
        if (startYear != null && endYear != null && startYear > endYear) {
            throw ServiceException.badRequest("开始年份不能大于结束年份");
        }

        LambdaQueryWrapper<HealthPhysicalTest> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HealthPhysicalTest::getUserId, user.getId());
        if (startYear != null) {
            wrapper.ge(HealthPhysicalTest::getTestYear, startYear);
        }
        if (endYear != null) {
            wrapper.le(HealthPhysicalTest::getTestYear, endYear);
        }
        wrapper.orderByAsc(HealthPhysicalTest::getTestYear)
                .orderByAsc(HealthPhysicalTest::getId);

        List<HealthPhysicalTestVO> result = new ArrayList<>();
        for (HealthPhysicalTest test : list(wrapper)) {
            result.add(HealthConverter.toPhysicalTestVO(test, false));
        }
        return result;
    }

    @Override
    public IPage<HealthPhysicalTestVO> pageAll(HealthPhysicalTestQueryDTO query) {
        if (!SecurityUtils.isDoctorOrAdmin()) {
            throw ServiceException.forbidden("仅校医或超级管理员可查看体测成绩");
        }
        if (query == null) {
            query = new HealthPhysicalTestQueryDTO();
        }
        ValidateUtils.requirePage(query.getPage(), query.getSize());
        int current = query.getPage() == null ? 1 : query.getPage();
        int size = query.getSize() == null ? 10 : query.getSize();

        LambdaQueryWrapper<HealthPhysicalTest> wrapper = new LambdaQueryWrapper<>();
        if (ValidateUtils.hasText(query.getStudentNo())) {
            wrapper.like(HealthPhysicalTest::getStudentNo, query.getStudentNo().trim());
        }
        if (query.getTestYear() != null) {
            wrapper.eq(HealthPhysicalTest::getTestYear, query.getTestYear());
        }
        String term = trimToNull(query.getTestTerm());
        if (term != null) {
            wrapper.eq(HealthPhysicalTest::getTestTerm, term);
        }
        wrapper.orderByDesc(HealthPhysicalTest::getTestYear)
                .orderByDesc(HealthPhysicalTest::getId);

        IPage<HealthPhysicalTest> page = page(new Page<>(current, size), wrapper);
        boolean desensitize = !SecurityUtils.hasRole(RoleConstants.SUPER_ADMIN);
        Page<HealthPhysicalTestVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<HealthPhysicalTestVO> records = new ArrayList<>();
        for (HealthPhysicalTest test : page.getRecords()) {
            records.add(HealthConverter.toPhysicalTestVO(test, desensitize));
        }
        voPage.setRecords(records);
        return voPage;
    }

    @Override
    public HealthPhysicalTestVO getDetail(Long id) {
        if (!SecurityUtils.isDoctorOrAdmin()) {
            throw ServiceException.forbidden("仅校医或超级管理员可查看体测成绩");
        }
        ValidateUtils.requireNotNull(id, "记录ID");
        HealthPhysicalTest test = getById(id);
        if (test == null) {
            throw ServiceException.notFound("体测记录不存在");
        }
        boolean desensitize = !SecurityUtils.hasRole(RoleConstants.SUPER_ADMIN);
        return HealthConverter.toPhysicalTestVO(test, desensitize);
    }

    private void applyDto(HealthPhysicalTest test, HealthPhysicalTestSaveDTO dto, String term) {
        test.setTestYear(dto.getTestYear());
        test.setTestTerm(term);
        test.setHeight(dto.getHeight());
        test.setWeight(dto.getWeight());
        test.setBmi(HealthMetricUtils.calcBmi(dto.getHeight(), dto.getWeight()));
        test.setVitalCapacity(dto.getVitalCapacity());
        test.setRun50m(dto.getRun50m());
        test.setSitAndReach(dto.getSitAndReach());
        test.setStandingJump(dto.getStandingJump());
        test.setEnduranceRun(dto.getEnduranceRun());
        test.setStrengthItem(dto.getStrengthItem());
        test.setTotalScore(dto.getTotalScore());
        test.setGradeLevel(trimToNull(dto.getGradeLevel()));
        test.setTestDate(dto.getTestDate());
        test.setRemark(trimToNull(dto.getRemark()));
    }

    private HealthPhysicalTest findByUnique(Long userId, Integer testYear, String testTerm) {
        return getOne(new LambdaQueryWrapper<HealthPhysicalTest>()
                .eq(HealthPhysicalTest::getUserId, userId)
                .eq(HealthPhysicalTest::getTestYear, testYear)
                .eq(HealthPhysicalTest::getTestTerm, testTerm), false);
    }

    /**
     * 校验录入/导入的体测数值，返回 null 的字段表示“未填写”。
     */
    private void validatePhysicalValues(Integer testYear, String testTerm,
                                        BigDecimal height, BigDecimal weight,
                                        Integer vitalCapacity, BigDecimal run50m,
                                        BigDecimal sitAndReach, BigDecimal standingJump,
                                        BigDecimal enduranceRun, Integer strengthItem,
                                        BigDecimal totalScore, String gradeLevel, LocalDate testDate) {
        if (testYear == null) {
            throw ServiceException.badRequest("测试年份不能为空");
        }
        checkYear(testYear, "测试年份");
        if (!ValidateUtils.hasText(testTerm)) {
            throw ServiceException.badRequest("测试学期不能为空");
        }
        ValidateUtils.requireMaxLength(testTerm, 20, "测试学期");
        if ((height == null) != (weight == null)) {
            throw ServiceException.badRequest("身高和体重需同时填写");
        }
        checkDecimalRange(height, "50", "250", "身高");
        checkDecimalRange(weight, "10", "500", "体重");
        checkIntRange(vitalCapacity, 0, 20000, "肺活量");
        checkDecimalRange(run50m, "0", "120", "50米跑成绩");
        checkDecimalRange(sitAndReach, "-50", "100", "坐位体前屈");
        checkDecimalRange(standingJump, "0", "500", "立定跳远成绩");
        checkDecimalRange(enduranceRun, "0", "3600", "耐力跑成绩");
        checkIntRange(strengthItem, 0, 300, "力量项目成绩");
        checkDecimalRange(totalScore, "0", "200", "体测总分");
        ValidateUtils.requireMaxLength(gradeLevel, 10, "体测等级");
        if (testDate != null && testDate.isAfter(LocalDate.now())) {
            throw ServiceException.badRequest("测试日期不能晚于今天");
        }
    }

    private void checkYear(Integer year, String fieldName) {
        if (year != null && (year < 2000 || year > 2100)) {
            throw ServiceException.badRequest(fieldName + "应在2000-2100之间");
        }
    }

    private void checkIntRange(Integer value, int min, int max, String fieldName) {
        if (value != null && (value < min || value > max)) {
            throw ServiceException.badRequest(fieldName + "应在" + min + "-" + max + "之间");
        }
    }

    private void checkDecimalRange(BigDecimal value, String min, String max, String fieldName) {
        if (value == null) {
            return;
        }
        if (value.compareTo(new BigDecimal(min)) < 0 || value.compareTo(new BigDecimal(max)) > 0) {
            throw ServiceException.badRequest(fieldName + "应在" + min + "-" + max + "之间");
        }
    }

    private String normalizeTerm(String term) {
        ValidateUtils.requireText(term, "测试学期");
        String normalized = term.trim();
        ValidateUtils.requireMaxLength(normalized, 20, "测试学期");
        return normalized;
    }

    private void validateImportFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ServiceException.badRequest("请选择要导入的 Excel 文件");
        }
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw ServiceException.badRequest("文件名不能为空");
        }
        String lower = filename.toLowerCase();
        if (!lower.endsWith(".xlsx") && !lower.endsWith(".xls")) {
            throw ServiceException.badRequest("仅支持 .xlsx 或 .xls 格式的 Excel 文件");
        }
        if (file.getSize() > 5L * 1024 * 1024) {
            throw ServiceException.badRequest("文件大小不能超过5MB");
        }
    }

    /**
     * 兼容文本日期、Excel 日期序列号和常见中文日期格式。
     */
    private LocalDate parseTestDate(String value) {
        if (!ValidateUtils.hasText(value)) {
            return null;
        }
        String text = value.trim();
        int spaceIndex = text.indexOf(' ');
        if (spaceIndex > 0) {
            text = text.substring(0, spaceIndex);
        }
        int timeIndex = text.indexOf('T');
        if (timeIndex > 0) {
            text = text.substring(0, timeIndex);
        }
        for (DateTimeFormatter formatter : DATE_FORMATS) {
            try {
                return LocalDate.parse(text, formatter);
            } catch (DateTimeParseException ignored) {
                // 尝试下一种格式
            }
        }
        try {
            double serial = Double.parseDouble(text);
            if (serial > 0) {
                // Excel 1900 日期系统（对 1900-03-01 之后的日期有效）
                return LocalDate.of(1899, 12, 30).plusDays((long) serial);
            }
        } catch (NumberFormatException ignored) {
            // 不是数字，走下面的统一报错
        }
        throw ServiceException.badRequest("测试日期格式应为 yyyy-MM-dd");
    }

    private boolean isEmptyRow(PhysicalTestImportRow row) {
        return !ValidateUtils.hasText(row.getStudentNo())
                && row.getTestYear() == null
                && !ValidateUtils.hasText(row.getTestTerm())
                && row.getHeight() == null
                && row.getWeight() == null
                && row.getVitalCapacity() == null
                && row.getRun50m() == null
                && row.getSitAndReach() == null
                && row.getStandingJump() == null
                && row.getEnduranceRun() == null
                && row.getStrengthItem() == null
                && row.getTotalScore() == null
                && !ValidateUtils.hasText(row.getGradeLevel())
                && !ValidateUtils.hasText(row.getTestDate());
    }

    /**
     * 处理一行导入数据。
     *
     * @return true 表示新增，false 表示覆盖更新已有学年学期记录
     */
    private boolean importRow(SysUser user, PhysicalTestImportRow row) {
        String studentNo = normalizeStudentNo(row.getStudentNo());
        if (studentNo != null && !studentNo.equals(user.getStudentNo())) {
            throw ServiceException.badRequest("学号与当前登录账号不一致");
        }
        String term = normalizeTerm(row.getTestTerm());
        LocalDate testDate = parseTestDate(row.getTestDate());
        validatePhysicalValues(row.getTestYear(), term, row.getHeight(), row.getWeight(),
                row.getVitalCapacity(), row.getRun50m(), row.getSitAndReach(), row.getStandingJump(),
                row.getEnduranceRun(), row.getStrengthItem(), row.getTotalScore(),
                row.getGradeLevel(), testDate);

        HealthPhysicalTest existing = findByUnique(user.getId(), row.getTestYear(), term);
        boolean insert = existing == null;
        HealthPhysicalTest test;
        if (insert) {
            test = new HealthPhysicalTest();
            test.setUserId(user.getId());
            test.setStudentNo(user.getStudentNo());
            test.setTestYear(row.getTestYear());
            test.setTestTerm(term);
        } else {
            test = existing;
        }

        // Excel 空单元格视为“不修改”，保留已有值
        if (row.getHeight() != null) {
            test.setHeight(row.getHeight());
        }
        if (row.getWeight() != null) {
            test.setWeight(row.getWeight());
        }
        test.setBmi(HealthMetricUtils.calcBmi(test.getHeight(), test.getWeight()));
        if (row.getVitalCapacity() != null) {
            test.setVitalCapacity(row.getVitalCapacity());
        }
        if (row.getRun50m() != null) {
            test.setRun50m(row.getRun50m());
        }
        if (row.getSitAndReach() != null) {
            test.setSitAndReach(row.getSitAndReach());
        }
        if (row.getStandingJump() != null) {
            test.setStandingJump(row.getStandingJump());
        }
        if (row.getEnduranceRun() != null) {
            test.setEnduranceRun(row.getEnduranceRun());
        }
        if (row.getStrengthItem() != null) {
            test.setStrengthItem(row.getStrengthItem());
        }
        if (row.getTotalScore() != null) {
            test.setTotalScore(row.getTotalScore());
        }
        if (ValidateUtils.hasText(row.getGradeLevel())) {
            test.setGradeLevel(row.getGradeLevel().trim());
        }
        if (testDate != null) {
            test.setTestDate(testDate);
        }

        if (insert) {
            save(test);
        } else {
            updateById(test);
        }
        return insert;
    }

    /**
     * Excel 数字单元格可能把学号读成 20230001.0 或科学计数法，统一转成纯数字文本。
     */
    private String normalizeStudentNo(String value) {
        if (!ValidateUtils.hasText(value)) {
            return null;
        }
        String text = value.trim();
        if (text.matches("\\d+\\.0+")) {
            return text.substring(0, text.indexOf('.'));
        }
        if (text.matches("\\d+(\\.\\d+)?[eE]\\+?\\d+")) {
            try {
                return new BigDecimal(text).toPlainString();
            } catch (NumberFormatException ignored) {
                return text;
            }
        }
        return text;
    }

    private void addError(PhysicalTestImportResultVO result, String message) {
        if (result.getErrors().size() < MAX_ERROR_MESSAGES) {
            result.getErrors().add(message);
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * EasyExcel 行监听器：逐行导入，单行失败不影响其他行。
     */
    private class PhysicalTestImportListener extends AnalysisEventListener<PhysicalTestImportRow> {

        private final SysUser user;
        private final PhysicalTestImportResultVO result;
        private boolean limitReached;

        private PhysicalTestImportListener(SysUser user, PhysicalTestImportResultVO result) {
            this.user = user;
            this.result = result;
        }

        @Override
        public void invoke(PhysicalTestImportRow row, AnalysisContext context) {
            if (row == null || isEmptyRow(row)) {
                return;
            }
            if (result.getTotal() >= MAX_IMPORT_ROWS) {
                if (!limitReached) {
                    limitReached = true;
                    addError(result, "单次最多导入" + MAX_IMPORT_ROWS + "条，超出部分已忽略");
                }
                return;
            }
            result.setTotal(result.getTotal() + 1);
            int rowNumber = context != null && context.readRowHolder() != null
                    ? context.readRowHolder().getRowIndex() + 1
                    : result.getTotal() + 1;
            try {
                boolean inserted = importRow(user, row);
                if (inserted) {
                    result.setInserted(result.getInserted() + 1);
                } else {
                    result.setUpdated(result.getUpdated() + 1);
                }
            } catch (ServiceException e) {
                result.setFailed(result.getFailed() + 1);
                addError(result, "第" + rowNumber + "行：" + e.getMessage());
            } catch (Exception e) {
                result.setFailed(result.getFailed() + 1);
                addError(result, "第" + rowNumber + "行：数据格式错误");
            }
        }

        @Override
        public void onException(Exception exception, AnalysisContext context) {
            if (result.getTotal() >= MAX_IMPORT_ROWS) {
                return;
            }
            int rowNumber;
            if (exception instanceof ExcelDataConvertException) {
                Integer rowIndex = ((ExcelDataConvertException) exception).getRowIndex();
                rowNumber = rowIndex == null ? result.getTotal() + 1 : rowIndex + 1;
            } else if (context != null && context.readRowHolder() != null) {
                rowNumber = context.readRowHolder().getRowIndex() + 1;
            } else {
                rowNumber = result.getTotal() + 1;
            }
            result.setTotal(result.getTotal() + 1);
            result.setFailed(result.getFailed() + 1);
            addError(result, "第" + rowNumber + "行：单元格数据格式错误");
        }

        @Override
        public void doAfterAllAnalysed(AnalysisContext context) {
            // 无需额外处理
        }
    }
}
