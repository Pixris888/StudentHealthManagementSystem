package com.health.management.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.health.management.common.util.ValidateUtils;
import com.health.management.dto.SysDictDataQueryDTO;
import com.health.management.dto.SysDictDataSaveDTO;
import com.health.management.entity.SysDictData;
import com.health.management.service.SysDictDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * 字典数据管理。
 * 管理接口需要 system:dict:* 权限；type/{dictType} 接口供所有登录用户做下拉框。
 */
@RestController
@RequestMapping("/api/system/dict_data")
public class SysDictDataController {

    @Autowired
    private SysDictDataService sysDictDataService;

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('system:dict:list')")
    public IPage<SysDictData> page(@Valid SysDictDataQueryDTO query) {
        return sysDictDataService.pageData(query);
    }

    @GetMapping("/type/{dictType}")
    public List<SysDictData> listByType(@PathVariable String dictType) {
        return sysDictDataService.listEnabledByType(dictType);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:dict:list')")
    public SysDictData getById(@PathVariable Long id) {
        return sysDictDataService.getData(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('system:dict:add')")
    public boolean add(@Valid @RequestBody SysDictDataSaveDTO dto) {
        sysDictDataService.createData(dto);
        return true;
    }

    @PutMapping
    @PreAuthorize("hasAuthority('system:dict:edit')")
    public boolean update(@Valid @RequestBody SysDictDataSaveDTO dto) {
        sysDictDataService.updateData(dto);
        return true;
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("hasAuthority('system:dict:remove')")
    public boolean remove(@PathVariable String ids) {
        sysDictDataService.deleteData(ValidateUtils.parseIds(ids));
        return true;
    }
}
