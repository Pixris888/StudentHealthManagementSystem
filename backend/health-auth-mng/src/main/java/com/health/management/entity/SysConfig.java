package com.health.management.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.IdType;
import com.health.management.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;


@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_config")
public class SysConfig extends BaseEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("config_name")
    private String configName; // 参数名称
    @TableField("config_key")
    private String configKey; // 参数键名
    @TableField("config_value")
    private String configValue; // 参数值
    @TableField("config_type")
    private Integer configType; // 参数类型 0-系统内置 1-自定义
}
