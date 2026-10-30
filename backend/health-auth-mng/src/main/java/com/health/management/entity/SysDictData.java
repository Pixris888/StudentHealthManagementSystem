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
@TableName("sys_dict_data")
public class SysDictData extends BaseEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("dict_type")
    private String dictType; // 字典类型
    @TableField("dict_label")
    private String dictLabel; // 字典标签
    @TableField("dict_value")
    private String dictValue; // 字典键值
    @TableField("dict_sort")
    private Integer dictSort; // 显示顺序
    @TableField("status")
    private Integer status; // 状态 0-停用 1-正常
}
