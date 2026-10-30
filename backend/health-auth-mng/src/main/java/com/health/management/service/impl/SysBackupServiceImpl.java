package com.health.management.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.health.management.audit.AuditLogRecorder;
import com.health.management.common.exception.ServiceException;
import com.health.management.common.util.ValidateUtils;
import com.health.management.config.BackupProperties;
import com.health.management.dto.SysBackupQueryDTO;
import com.health.management.entity.SysBackupRecord;
import com.health.management.mapper.SysBackupRecordMapper;
import com.health.management.service.SysBackupService;
import com.health.management.vo.SysBackupRecordVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 数据备份业务实现。
 *
 * 采用逻辑备份：逐表查询并导出 CSV，打包成 ZIP。
 * 不依赖 mysqldump，跨平台；备份文件只允许超级管理员下载。
 */
@Service
public class SysBackupServiceImpl extends ServiceImpl<SysBackupRecordMapper, SysBackupRecord>
        implements SysBackupService {

    private static final Logger log = LoggerFactory.getLogger(SysBackupServiceImpl.class);
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter FILE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");
    private static final String BACKUP_TABLE = "sys_backup_record";

    private final AtomicBoolean running = new AtomicBoolean(false);

    @Autowired
    private DataSource dataSource;

    @Autowired
    private BackupProperties backupProperties;

    @Autowired
    private AuditLogRecorder auditLogRecorder;

    @Override
    public IPage<SysBackupRecordVO> pageBackups(SysBackupQueryDTO query) {
        SysBackupQueryDTO q = query == null ? new SysBackupQueryDTO() : query;
        ValidateUtils.requirePage(q.getPage(), q.getSize());
        validateDateRange(q);
        int current = q.getPage() == null ? 1 : q.getPage();
        int size = q.getSize() == null ? 10 : q.getSize();

        LambdaQueryWrapper<SysBackupRecord> wrapper = new LambdaQueryWrapper<>();
        if (ValidateUtils.hasText(q.getKeyword())) {
            wrapper.like(SysBackupRecord::getFileName, q.getKeyword().trim());
        }
        if (q.getStatus() != null) {
            wrapper.eq(SysBackupRecord::getStatus, q.getStatus());
        }
        if (q.getStartDate() != null) {
            wrapper.ge(SysBackupRecord::getCreateTime, q.getStartDate().atStartOfDay());
        }
        if (q.getEndDate() != null) {
            wrapper.lt(SysBackupRecord::getCreateTime, q.getEndDate().plusDays(1).atStartOfDay());
        }
        wrapper.orderByDesc(SysBackupRecord::getCreateTime)
                .orderByDesc(SysBackupRecord::getId);

        IPage<SysBackupRecord> page = page(new Page<>(current, size), wrapper);
        Page<SysBackupRecordVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<SysBackupRecordVO> records = new ArrayList<>();
        for (SysBackupRecord record : page.getRecords()) {
            records.add(toVO(record));
        }
        result.setRecords(records);
        return result;
    }

    @Override
    public SysBackupRecordVO createBackup() {
        if (!running.compareAndSet(false, true)) {
            throw ServiceException.conflict("已有备份任务正在执行，请稍后再试");
        }
        Path file = null;
        String fileName = "health_management_" + FILE_FORMAT.format(LocalDateTime.now(ZONE)) + ".zip";
        try {
            Path dir = backupDir();
            file = dir.resolve(fileName);
            BackupStats stats = writeBackupFile(file);

            SysBackupRecord record = buildRecord(
                    fileName, file, Files.size(file), stats.tableCount, stats.rowCount,
                    1, "");
            if (!save(record)) {
                throw ServiceException.fail("备份记录保存失败");
            }
            auditLogRecorder.record("数据备份", AuditLogRecorder.TYPE_ADD,
                    "创建备份：" + fileName, true, "");
            return toVO(record);
        } catch (Exception e) {
            if (file != null) {
                try {
                    Files.deleteIfExists(file);
                } catch (IOException ignored) {
                    // 忽略清理失败，保留原始异常
                }
            }
            String errorMsg = truncate("备份失败：" + e.getMessage(), 1000);
            try {
                SysBackupRecord failed = buildRecord(fileName, null, 0, 0, 0, 0, errorMsg);
                save(failed);
            } catch (Exception saveError) {
                log.warn("保存失败备份记录异常：{}", saveError.getMessage());
            }
            auditLogRecorder.record("数据备份", AuditLogRecorder.TYPE_ADD,
                    "创建备份失败：" + fileName, false, errorMsg);
            log.warn("数据库备份失败：{}", errorMsg);
            throw ServiceException.fail("数据库备份失败：" + truncate(e.getMessage(), 200));
        } finally {
            running.set(false);
        }
    }

    @Override
    public SysBackupRecordVO getBackup(Long id) {
        return toVO(requireRecord(id));
    }

    @Override
    public Resource loadBackupResource(Long id) {
        SysBackupRecord record = requireRecord(id);
        if (!Integer.valueOf(1).equals(record.getStatus())
                || !ValidateUtils.hasText(record.getFilePath())) {
            throw ServiceException.notFound("备份文件不存在或备份失败");
        }
        Path base = backupDir();
        Path file = Paths.get(record.getFilePath()).toAbsolutePath().normalize();
        if (!file.startsWith(base)) {
            throw ServiceException.forbidden("备份文件路径非法");
        }
        if (!Files.isRegularFile(file)) {
            throw ServiceException.notFound("备份文件已被删除");
        }
        return new FileSystemResource(file);
    }

    @Override
    public boolean deleteBackups(List<Long> ids) {
        ValidateUtils.requireIds(ids, "备份记录ID");
        List<SysBackupRecord> records = listByIds(ids);
        if (records.size() != ids.size()) {
            throw ServiceException.notFound("部分备份记录不存在");
        }
        Path base = backupDir();
        for (SysBackupRecord record : records) {
            if (!ValidateUtils.hasText(record.getFilePath())) {
                continue;
            }
            try {
                Path file = Paths.get(record.getFilePath()).toAbsolutePath().normalize();
                if (file.startsWith(base)) {
                    Files.deleteIfExists(file);
                }
            } catch (Exception e) {
                log.warn("删除备份文件失败：{}", e.getMessage());
            }
        }
        boolean removed = removeByIds(ids);
        auditLogRecorder.record("数据备份", AuditLogRecorder.TYPE_DELETE,
                "删除备份ID：" + ids, removed, "");
        return removed;
    }

    private BackupStats writeBackupFile(Path file) throws Exception {
        BackupStats stats = new BackupStats();
        try (Connection connection = dataSource.getConnection();
             ZipOutputStream zip = new ZipOutputStream(
                     new BufferedOutputStream(Files.newOutputStream(file)))) {
            String database = connection.getCatalog();
            if (!ValidateUtils.hasText(database)) {
                throw new IOException("无法获取当前数据库名称");
            }
            List<String> tables = listTables(connection, database);
            Map<String, Long> rowCounts = new LinkedHashMap<>();
            for (String table : tables) {
                long rows = writeTableCsv(connection, zip, table);
                rowCounts.put(table, rows);
                stats.tableCount++;
                stats.rowCount += rows;
            }
            writeManifest(zip, database, file.getFileName().toString(), rowCounts, stats);
        }
        return stats;
    }

    private List<String> listTables(Connection connection, String database) throws Exception {
        List<String> tables = new ArrayList<>();
        String sql = "SELECT table_name FROM information_schema.tables "
                + "WHERE table_schema = ? AND table_type = 'BASE TABLE' "
                + "AND table_name <> ? ORDER BY table_name";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, database);
            ps.setString(2, BACKUP_TABLE);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tables.add(rs.getString(1));
                }
            }
        }
        return tables;
    }

    private long writeTableCsv(Connection connection, ZipOutputStream zip, String table) throws Exception {
        long rows = 0;
        zip.putNextEntry(new ZipEntry("data/" + table + ".csv"));
        String safeTable = table.replace("`", "``");
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT * FROM `" + safeTable + "`")) {
            ResultSetMetaData meta = rs.getMetaData();
            int columns = meta.getColumnCount();
            List<String> header = new ArrayList<>();
            for (int i = 1; i <= columns; i++) {
                header.add(meta.getColumnLabel(i));
            }
            writeCsvRow(zip, header);
            while (rs.next()) {
                List<String> row = new ArrayList<>();
                for (int i = 1; i <= columns; i++) {
                    row.add(rs.getString(i));
                }
                writeCsvRow(zip, row);
                rows++;
            }
        }
        zip.closeEntry();
        return rows;
    }

    private void writeManifest(ZipOutputStream zip, String database, String fileName,
                               Map<String, Long> rowCounts, BackupStats stats) throws IOException {
        zip.putNextEntry(new ZipEntry("manifest.txt"));
        StringBuilder manifest = new StringBuilder();
        manifest.append("database=").append(database).append('\n');
        manifest.append("file=").append(fileName).append('\n');
        manifest.append("createdAt=").append(LocalDateTime.now(ZONE)).append('\n');
        manifest.append("tableCount=").append(stats.tableCount).append('\n');
        manifest.append("rowCount=").append(stats.rowCount).append('\n');
        manifest.append("tables:\n");
        for (Map.Entry<String, Long> entry : rowCounts.entrySet()) {
            manifest.append("- ").append(entry.getKey()).append(": ")
                    .append(entry.getValue()).append(" rows\n");
        }
        zip.write(manifest.toString().getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private void writeCsvRow(ZipOutputStream zip, List<String> values) throws IOException {
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                line.append(',');
            }
            line.append(csv(values.get(i)));
        }
        line.append('\n');
        zip.write(line.toString().getBytes(StandardCharsets.UTF_8));
    }

    private String csv(String value) {
        if (value == null) {
            return "";
        }
        boolean quote = value.indexOf(',') >= 0
                || value.indexOf('"') >= 0
                || value.indexOf('\n') >= 0
                || value.indexOf('\r') >= 0;
        String escaped = value.replace("\"", "\"\"");
        return quote ? "\"" + escaped + "\"" : escaped;
    }

    private SysBackupRecord buildRecord(String fileName, Path file, long fileSize,
                                        int tableCount, long rowCount,
                                        int status, String errorMsg) {
        SysBackupRecord record = new SysBackupRecord();
        record.setFileName(fileName);
        record.setFilePath(file == null ? "" : file.toAbsolutePath().normalize().toString());
        record.setFileSize(fileSize);
        record.setTableCount(tableCount);
        record.setRowCount(rowCount);
        record.setStatus(status);
        record.setErrorMsg(errorMsg);
        return record;
    }

    private SysBackupRecord requireRecord(Long id) {
        ValidateUtils.requireNotNull(id, "备份记录ID");
        SysBackupRecord record = getById(id);
        if (record == null) {
            throw ServiceException.notFound("备份记录不存在");
        }
        return record;
    }

    private Path backupDir() {
        Path dir = Paths.get(backupProperties.getDir()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(dir);
            return dir;
        } catch (IOException e) {
            throw ServiceException.fail("备份目录创建失败：" + e.getMessage());
        }
    }

    private SysBackupRecordVO toVO(SysBackupRecord record) {
        SysBackupRecordVO vo = new SysBackupRecordVO();
        vo.setId(record.getId());
        vo.setFileName(record.getFileName());
        vo.setFileSize(record.getFileSize());
        vo.setTableCount(record.getTableCount());
        vo.setRowCount(record.getRowCount());
        vo.setStatus(record.getStatus());
        vo.setStatusName(Integer.valueOf(1).equals(record.getStatus()) ? "成功" : "失败");
        vo.setErrorMsg(record.getErrorMsg());
        vo.setCreateBy(record.getCreateBy());
        vo.setCreateTime(record.getCreateTime());
        return vo;
    }

    private void validateDateRange(SysBackupQueryDTO q) {
        if (q.getStartDate() != null && q.getEndDate() != null
                && q.getStartDate().isAfter(q.getEndDate())) {
            throw ServiceException.badRequest("开始日期不能晚于结束日期");
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private static class BackupStats {
        private int tableCount;
        private long rowCount;
    }
}
