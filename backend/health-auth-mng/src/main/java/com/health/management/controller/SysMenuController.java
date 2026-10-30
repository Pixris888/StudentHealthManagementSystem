package com.health.management.controller;

import com.health.management.common.vo.MenuTree;
import com.health.management.dto.MenuSaveDTO;
import com.health.management.entity.SysMenu;
import com.health.management.service.system.SysMenuService;
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
 * 菜单管理：所有业务校验/事务在 SysMenuServiceImpl。
 */
@RestController
@RequestMapping("/api/system/menu")
public class SysMenuController {

    @Autowired
    private SysMenuService sysMenuService;

    @GetMapping("/tree")
    @PreAuthorize("hasAuthority('system:menu:list')")
    public List<MenuTree> tree() {
        return sysMenuService.listMenuTree();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:menu:list')")
    public SysMenu getById(@PathVariable Long id) {
        return sysMenuService.getMenuDetail(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('system:menu:add')")
    public Long add(@Valid @RequestBody MenuSaveDTO dto) {
        return sysMenuService.createMenu(dto);
    }

    @PutMapping
    @PreAuthorize("hasAuthority('system:menu:edit')")
    public boolean update(@Valid @RequestBody MenuSaveDTO dto) {
        sysMenuService.updateMenu(dto);
        return true;
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:menu:remove')")
    public boolean remove(@PathVariable Long id) {
        sysMenuService.deleteMenu(id);
        return true;
    }
}
