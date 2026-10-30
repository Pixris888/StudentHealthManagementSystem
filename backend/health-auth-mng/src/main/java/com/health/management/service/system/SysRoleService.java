package com.health.management.service.system;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.health.management.dto.RoleQueryDTO;
import com.health.management.dto.RoleSaveDTO;
import com.health.management.entity.SysRole;
import com.health.management.vo.RoleDetailVO;
import com.health.management.vo.RoleVO;

import java.util.List;

/**
 * 角色与授权服务
 */
public interface SysRoleService extends IService<SysRole> {

    IPage<RoleVO> pageRoles(RoleQueryDTO query);

    RoleDetailVO getRoleDetail(Long roleId);

    Long createRole(RoleSaveDTO dto);

    void updateRole(RoleSaveDTO dto);

    void deleteRoles(String ids);

    void assignMenus(Long roleId, List<Long> menuIds);

    List<RoleVO> listEnabledRoles();

    List<Long> getMenuIdsByRoleId(Long roleId);
}
