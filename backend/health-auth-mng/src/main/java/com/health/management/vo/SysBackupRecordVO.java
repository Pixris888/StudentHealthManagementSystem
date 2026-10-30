package com.health.management.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 数据备份记录展示对象（不暴露服务器文件路径）。
 */
@Data
public class SysBackupRecordVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String fileName;

    private Long fileSize;

    private Integer tableCount;

    private Long rowCount;

    private Integer status;

    private String statusName;

    private String errorMsg;

    private Long createBy;

    private LocalDateTime createTime;
}
