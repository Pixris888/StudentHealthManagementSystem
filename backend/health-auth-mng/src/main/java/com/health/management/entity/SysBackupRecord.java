package com.health.management.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.health.management.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 数据库备份记录。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_backup_record")
public class SysBackupRecord extends BaseEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("file_name")
    private String fileName;

    @TableField("file_path")
    private String filePath;

    @TableField("file_size")
    private Long fileSize;

    @TableField("table_count")
    private Integer tableCount;

    @TableField("row_count")
    private Long rowCount;

    /** 0-失败，1-成功 */
    @TableField("status")
    private Integer status;

    @TableField("error_msg")
    private String errorMsg;
}
