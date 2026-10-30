package com.health.management.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.health.management.dto.SysBackupQueryDTO;
import com.health.management.vo.SysBackupRecordVO;
import org.springframework.core.io.Resource;

import java.util.List;

/**
 * 数据备份业务接口。
 * 当前实现为逻辑备份：按表导出 CSV 并打包为 ZIP，不依赖 mysqldump。
 */
public interface SysBackupService {

    IPage<SysBackupRecordVO> pageBackups(SysBackupQueryDTO query);

    SysBackupRecordVO createBackup();

    SysBackupRecordVO getBackup(Long id);

    Resource loadBackupResource(Long id);

    boolean deleteBackups(List<Long> ids);
}
