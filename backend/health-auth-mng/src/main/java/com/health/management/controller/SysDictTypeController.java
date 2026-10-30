package com.health.management.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.health.management.common.util.ValidateUtils;
import com.health.management.dto.SysDictTypeQueryDTO;
import com.health.management.dto.SysDictTypeSaveDTO;
import com.health.management.entity.SysDictType;
import com.health.management.service.SysDictTypeService;
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
 * 字典类型管理。
 * 管理接口需要 system:dict:* 权限；enabled 接口供所有登录用户做下拉框。
 */
@RestController
@RequestMapping("/api/system/dict_type")
public class SysDictTypeController {

    @Autowired
    private SysDictTypeService sysDictTypeService;

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('system:dict:list')")
    public IPage<SysDictType> page(@Valid SysDictTypeQueryDTO query) {
        return sysDictTypeService.pageTypes(query);
    }

    @GetMapping("/enabled")
    public List<SysDictType> enabled() {
        return sysDictTypeService.listEnabledTypes();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:dict:list')")
    public SysDictType getById(@PathVariable Long id) {
        return sysDictTypeService.getType(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('system:dict:add')")
    public boolean add(@Valid @RequestBody SysDictTypeSaveDTO dto) {
        sysDictTypeService.createType(dto);
        return true;
    }

    @PutMapping
    @PreAuthorize("hasAuthority('system:dict:edit')")
    public boolean update(@Valid @RequestBody SysDictTypeSaveDTO dto) {
        sysDictTypeService.updateType(dto);
        return true;
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("hasAuthority('system:dict:remove')")
    public boolean remove(@PathVariable String ids) {
        sysDictTypeService.deleteTypes(ValidateUtils.parseIds(ids));
        return true;
    }
}
