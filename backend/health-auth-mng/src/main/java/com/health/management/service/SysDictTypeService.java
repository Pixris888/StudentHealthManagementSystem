package com.health.management.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.health.management.dto.SysDictTypeQueryDTO;
import com.health.management.dto.SysDictTypeSaveDTO;
import com.health.management.entity.SysDictType;

import java.util.List;

/**
 * 字典类型业务接口。
 */
public interface SysDictTypeService extends IService<SysDictType> {

    IPage<SysDictType> pageTypes(SysDictTypeQueryDTO query);

    /** 前端下拉框使用：启用中的字典类型 */
    List<SysDictType> listEnabledTypes();

    SysDictType getType(Long id);

    void createType(SysDictTypeSaveDTO dto);

    void updateType(SysDictTypeSaveDTO dto);

    void deleteTypes(List<Long> ids);
}
