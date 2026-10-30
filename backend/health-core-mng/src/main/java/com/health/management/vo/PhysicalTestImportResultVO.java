package com.health.management.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 体测成绩 Excel 导入结果。
 */
@Data
public class PhysicalTestImportResultVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 有效数据行数（不含空行） */
    private int total;

    /** 新增条数 */
    private int inserted;

    /** 覆盖更新条数 */
    private int updated;

    /** 失败条数 */
    private int failed;

    /** 失败原因（最多返回前 20 条） */
    private List<String> errors = new ArrayList<>();
}
