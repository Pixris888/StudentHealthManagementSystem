package com.health.management.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.health.management.dto.SysDictDataQueryDTO;
import com.health.management.dto.SysDictDataSaveDTO;
import com.health.management.entity.SysDictData;

import java.util.List;

/**
 * 字典数据业务接口。
 */
public interface SysDictDataService extends IService<SysDictData> {

    IPage<SysDictData> pageData(SysDictDataQueryDTO query);

    /** 前端下拉框使用：某个启用字典类型下的启用数据 */
    List<SysDictData> listEnabledByType(String dictType);

    SysDictData getData(Long id);

    void createData(SysDictDataSaveDTO dto);

    void updateData(SysDictDataSaveDTO dto);

    void deleteData(List<Long> ids);
}
