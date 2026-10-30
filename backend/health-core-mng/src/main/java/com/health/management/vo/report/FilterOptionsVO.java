package com.health.management.vo.report;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 台账筛选选项。
 */
@Data
public class FilterOptionsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<String> colleges;
    private List<String> grades;
    private List<String> classes;
}
