package com.health.management.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.health.management.dto.AssignIdsDTO;
import com.health.management.dto.RoleQueryDTO;
import com.health.management.dto.RoleSaveDTO;
import com.health.management.service.system.SysRoleService;
import com.health.management.vo.RoleDetailVO;
import com.health.management.vo.RoleVO;
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
 * 角色管理：所有业务校验/事务在 SysRoleServiceImpl。
 */
@RestController
@RequestMapping("/api/system/role")
public class SysRoleController {

    @Autowired
    private SysRoleService sysRoleService;

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('system:role:list')")
    public IPage<RoleVO> page(@Valid RoleQueryDTO query) {
        return sysRoleService.pageRoles(query);
    }

    @GetMapping("/enabled")
    @PreAuthorize("hasAuthority('system:role:list')")
    public List<RoleVO> listEnabled() {
        return sysRoleService.listEnabledRoles();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:role:list')")
    public RoleDetailVO getById(@PathVariable Long id) {
        return sysRoleService.getRoleDetail(id);
    }

    @GetMapping("/{id}/menus")
    @PreAuthorize("hasAuthority('system:role:list')")
    public List<Long> getMenuIds(@PathVariable Long id) {
        return sysRoleService.getMenuIdsByRoleId(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('system:role:add')")
    public Long add(@Valid @RequestBody RoleSaveDTO dto) {
        return sysRoleService.createRole(dto);
    }

    @PutMapping
    @PreAuthorize("hasAuthority('system:role:edit')")
    public boolean update(@Valid @RequestBody RoleSaveDTO dto) {
        sysRoleService.updateRole(dto);
        return true;
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("hasAuthority('system:role:remove')")
    public boolean remove(@PathVariable String ids) {
        sysRoleService.deleteRoles(ids);
        return true;
    }

    @PutMapping("/{id}/menus")
    @PreAuthorize("hasAuthority('system:role:auth')")
    public boolean assignMenus(@PathVariable Long id, @Valid @RequestBody AssignIdsDTO dto) {
        sysRoleService.assignMenus(id, dto.getIds());
        return true;
    }
}
