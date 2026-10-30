package com.health.management.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.health.management.dto.AssignIdsDTO;
import com.health.management.dto.ResetPasswordDTO;
import com.health.management.dto.UserCreateDTO;
import com.health.management.dto.UserQueryDTO;
import com.health.management.dto.UserUpdateDTO;
import com.health.management.service.system.SysUserService;
import com.health.management.vo.UserVO;
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
 * 用户管理：所有业务校验/事务在 SysUserServiceImpl。
 */
@RestController
@RequestMapping("/api/system/user")
public class SysUserController {

    @Autowired
    private SysUserService sysUserService;

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('system:user:list')")
    public IPage<UserVO> page(@Valid UserQueryDTO query) {
        return sysUserService.pageUsers(query);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:list')")
    public UserVO getById(@PathVariable Long id) {
        return sysUserService.getUserDetail(id);
    }

    @GetMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('system:user:list')")
    public List<Long> getRoleIds(@PathVariable Long id) {
        return sysUserService.getRoleIdsByUserId(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('system:user:add')")
    public Long add(@Valid @RequestBody UserCreateDTO dto) {
        return sysUserService.createUser(dto);
    }

    @PutMapping
    @PreAuthorize("hasAuthority('system:user:edit')")
    public boolean update(@Valid @RequestBody UserUpdateDTO dto) {
        sysUserService.updateUser(dto);
        return true;
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("hasAuthority('system:user:remove')")
    public boolean remove(@PathVariable String ids) {
        sysUserService.deleteUsers(ids);
        return true;
    }

    @PutMapping("/{id}/password")
    @PreAuthorize("hasAuthority('system:user:resetpwd')")
    public boolean resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordDTO dto) {
        sysUserService.resetPassword(id, dto.getNewPassword());
        return true;
    }

    @PutMapping("/{id}/status/{status}")
    @PreAuthorize("hasAuthority('system:user:status')")
    public boolean updateStatus(@PathVariable Long id, @PathVariable Integer status) {
        sysUserService.updateStatus(id, status);
        return true;
    }

    @PutMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('system:user:assignRole')")
    public boolean assignRoles(@PathVariable Long id, @Valid @RequestBody AssignIdsDTO dto) {
        sysUserService.assignRoles(id, dto.getIds());
        return true;
    }
}
