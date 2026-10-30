package com.health.management.service.system.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.health.management.audit.AuditLogRecorder;
import com.health.management.common.constant.RoleConstants;
import com.health.management.common.exception.ServiceException;
import com.health.management.common.util.ValidateUtils;
import com.health.management.convert.SystemConverter;
import com.health.management.dto.RoleQueryDTO;
import com.health.management.dto.RoleSaveDTO;
import com.health.management.entity.SysMenu;
import com.health.management.entity.SysRole;
import com.health.management.entity.SysRoleMenu;
import com.health.management.entity.SysUserRole;
import com.health.management.mapper.SysMenuMapper;
import com.health.management.mapper.SysRoleMapper;
import com.health.management.mapper.SysRoleMenuMapper;
import com.health.management.mapper.SysUserRoleMapper;
import com.health.management.security.SecurityUtils;
import com.health.management.security.TokenSessionService;
import com.health.management.service.SysRoleMenuService;
import com.health.management.service.system.SysRoleService;
import com.health.management.vo.RoleDetailVO;
import com.health.management.vo.RoleVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements SysRoleService {

    @Autowired
    private SysRoleMenuMapper roleMenuMapper;

    @Autowired
    private SysUserRoleMapper userRoleMapper;

    @Autowired
    private SysMenuMapper menuMapper;

    @Autowired
    private SysRoleMenuService roleMenuService;

    @Autowired
    private AuditLogRecorder auditLogRecorder;

    @Autowired
    private TokenSessionService tokenSessionService;

    @Override
    public IPage<RoleVO> pageRoles(RoleQueryDTO query) {
        if (query == null) {
            query = new RoleQueryDTO();
        }
        ValidateUtils.requirePage(query.getPage(), query.getSize());
        int current = query.getPage() == null ? 1 : query.getPage();
        int size = query.getSize() == null ? 10 : query.getSize();

        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        if (ValidateUtils.hasText(query.getRoleCode())) {
            wrapper.like(SysRole::getRoleCode, query.getRoleCode().trim());
        }
        if (ValidateUtils.hasText(query.getRoleName())) {
            wrapper.like(SysRole::getRoleName, query.getRoleName().trim());
        }
        if (query.getStatus() != null) {
            wrapper.eq(SysRole::getStatus, query.getStatus());
        }
        wrapper.orderByAsc(SysRole::getRoleSort).orderByAsc(SysRole::getId);

        IPage<SysRole> page = page(new Page<>(current, size), wrapper);
        Page<RoleVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<RoleVO> records = new ArrayList<>();
        for (SysRole role : page.getRecords()) {
            records.add(SystemConverter.toRoleVO(role));
        }
        voPage.setRecords(records);
        return voPage;
    }

    @Override
    public RoleDetailVO getRoleDetail(Long roleId) {
        SysRole role = getById(roleId);
        if (role == null) {
            throw ServiceException.notFound("角色不存在");
        }
        return SystemConverter.toRoleDetailVO(role, getMenuIdsByRoleId(roleId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createRole(RoleSaveDTO dto) {
        validateRole(dto, null);
        if (baseMapper.countByRoleCodeIncludingDeleted(dto.getRoleCode()) > 0) {
            throw ServiceException.conflict("角色编码已存在");
        }
        SysRole role = new SysRole();
        copyRole(dto, role);
        try {
            save(role);
        } catch (DuplicateKeyException e) {
            throw ServiceException.conflict("角色编码已存在");
        }
        if (dto.getMenuIds() != null) {
            replaceRoleMenus(role.getId(), dto.getMenuIds());
        }
        auditLogRecorder.record("角色管理", AuditLogRecorder.TYPE_ADD,
                "新增角色：" + role.getRoleCode(), true, null);
        return role.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRole(RoleSaveDTO dto) {
        ValidateUtils.requireNotNull(dto.getId(), "角色ID");
        SysRole existing = getById(dto.getId());
        if (existing == null) {
            throw ServiceException.notFound("角色不存在");
        }
        validateRole(dto, existing);
        if (baseMapper.countByRoleCodeExcludingId(dto.getRoleCode(), existing.getId()) > 0) {
            throw ServiceException.conflict("角色编码已存在");
        }
        copyRole(dto, existing);
        try {
            updateById(existing);
        } catch (DuplicateKeyException e) {
            throw ServiceException.conflict("角色编码已存在");
        }
        if (dto.getMenuIds() != null) {
            replaceRoleMenus(existing.getId(), dto.getMenuIds());
        }
        userRoleMapper.incrementTokenVersionByRoleId(existing.getId());
        tokenSessionService.removeAllSessions(userRoleMapper.selectUserIdsByRoleId(existing.getId()));
        auditLogRecorder.record("角色管理", AuditLogRecorder.TYPE_UPDATE,
                "修改角色：" + existing.getRoleCode(), true, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRoles(String ids) {
        List<Long> idList = ValidateUtils.parseIds(ids);
        for (Long roleId : idList) {
            SysRole role = getById(roleId);
            if (role == null) {
                throw ServiceException.notFound("角色不存在，ID=" + roleId);
            }
            if (RoleConstants.isBuiltIn(role.getRoleCode())) {
                throw ServiceException.badRequest("内置角色不能删除：" + role.getRoleName());
            }
            if (userRoleMapper.countByRoleId(roleId) > 0) {
                throw ServiceException.badRequest("角色已被用户占用，不能删除：" + role.getRoleName());
            }
            roleMenuMapper.deleteByRoleIdPhysically(roleId);
            removeById(roleId);
        }
        auditLogRecorder.record("角色管理", AuditLogRecorder.TYPE_DELETE,
                "删除角色ID：" + ids, true, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignMenus(Long roleId, List<Long> menuIds) {
        SysRole role = getById(roleId);
        if (role == null) {
            throw ServiceException.notFound("角色不存在");
        }
        List<Long> targetIds;
        if (RoleConstants.SUPER_ADMIN.equals(role.getRoleCode())) {
            targetIds = new ArrayList<>();
            List<SysMenu> allEnabled = menuMapper.selectList(
                    new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getStatus, 1));
            for (SysMenu menu : allEnabled) {
                targetIds.add(menu.getId());
            }
        } else {
            targetIds = menuIds;
        }
        replaceRoleMenus(roleId, targetIds);
        userRoleMapper.incrementTokenVersionByRoleId(roleId);
        tokenSessionService.removeAllSessions(userRoleMapper.selectUserIdsByRoleId(roleId));
        auditLogRecorder.record("角色管理", AuditLogRecorder.TYPE_GRANT,
                "分配菜单：角色=" + role.getRoleCode() + ", 菜单数=" + (targetIds == null ? 0 : targetIds.size()),
                true, null);
    }

    @Override
    public List<RoleVO> listEnabledRoles() {
        List<SysRole> roles = list(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getStatus, 1)
                .orderByAsc(SysRole::getRoleSort)
                .orderByAsc(SysRole::getId));
        return SystemConverter.toRoleVOList(roles);
    }

    @Override
    public List<Long> getMenuIdsByRoleId(Long roleId) {
        List<SysRoleMenu> relations = roleMenuMapper.selectByRoleId(roleId);
        List<Long> ids = new ArrayList<>();
        for (SysRoleMenu relation : relations) {
            ids.add(relation.getMenuId());
        }
        return ids;
    }

    private void validateRole(RoleSaveDTO dto, SysRole existing) {
        ValidateUtils.requireNotNull(dto, "请求参数");
        ValidateUtils.requireRoleCode(dto.getRoleCode());
        ValidateUtils.requireText(dto.getRoleName(), "角色名称");
        ValidateUtils.requireMaxLength(dto.getRoleName(), 50, "角色名称");
        ValidateUtils.requireUserType(dto.getUserType());
        if (dto.getRoleSort() != null && dto.getRoleSort() < 0) {
            throw ServiceException.badRequest("显示顺序不能小于0");
        }
        if (dto.getDataScope() != null) {
            ValidateUtils.requireDataScope(dto.getDataScope());
        }
        if (dto.getStatus() != null) {
            ValidateUtils.requireStatus(dto.getStatus());
        }

        if (existing != null && RoleConstants.isBuiltIn(existing.getRoleCode())
                && !existing.getRoleCode().equals(dto.getRoleCode())) {
            throw ServiceException.badRequest("内置角色编码不能修改");
        }
        Integer builtInUserType = RoleConstants.userTypeOf(dto.getRoleCode());
        if (builtInUserType != null && !builtInUserType.equals(dto.getUserType())) {
            throw ServiceException.badRequest("内置角色的用户类型不能修改");
        }
        if (dto.getUserType() > currentUserType()) {
            throw ServiceException.forbidden("不能创建或修改高于自己权限级别的角色");
        }
        if (Integer.valueOf(RoleConstants.USER_TYPE_SUPER_ADMIN).equals(dto.getUserType()) && !isSuperAdmin()) {
            throw ServiceException.forbidden("只有超级管理员才能创建或修改超级管理员类型的角色");
        }
        if (RoleConstants.SUPER_ADMIN.equals(dto.getRoleCode())
                && Integer.valueOf(0).equals(dto.getStatus())) {
            throw ServiceException.badRequest("超级管理员角色不能停用");
        }
    }

    private void copyRole(RoleSaveDTO dto, SysRole role) {
        role.setRoleCode(dto.getRoleCode().trim());
        role.setRoleName(dto.getRoleName().trim());
        role.setUserType(dto.getUserType());
        role.setRoleSort(dto.getRoleSort() == null ? 0 : dto.getRoleSort());
        role.setDataScope(dto.getDataScope() == null ? 3 : dto.getDataScope());
        role.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        role.setRemark(trimToNull(dto.getRemark()));
    }

    private void replaceRoleMenus(Long roleId, List<Long> menuIds) {
        List<Long> expanded = expandMenuIds(menuIds);
        roleMenuMapper.deleteByRoleIdPhysically(roleId);
        if (!expanded.isEmpty()) {
            List<SysRoleMenu> relations = new ArrayList<>();
            for (Long menuId : expanded) {
                SysRoleMenu relation = new SysRoleMenu();
                relation.setRoleId(roleId);
                relation.setMenuId(menuId);
                relations.add(relation);
            }
            roleMenuService.saveBatch(relations);
        }
    }

    /**
     * 补齐所有祖先菜单，避免前端菜单树断链
     */
    private List<Long> expandMenuIds(List<Long> menuIds) {
        if (menuIds == null || menuIds.isEmpty()) {
            return new ArrayList<>();
        }
        Set<Long> input = new LinkedHashSet<>();
        for (Long menuId : menuIds) {
            if (menuId == null || menuId <= 0) {
                throw ServiceException.badRequest("菜单ID非法");
            }
            input.add(menuId);
        }
        List<SysMenu> allMenus = menuMapper.selectList(null);
        Map<Long, SysMenu> menuMap = new HashMap<>();
        for (SysMenu menu : allMenus) {
            menuMap.put(menu.getId(), menu);
        }
        Set<Long> result = new LinkedHashSet<>();
        for (Long menuId : input) {
            SysMenu menu = menuMap.get(menuId);
            if (menu == null) {
                throw ServiceException.badRequest("菜单不存在，ID=" + menuId);
            }
            if (!Integer.valueOf(1).equals(menu.getStatus())) {
                throw ServiceException.badRequest("菜单已停用，不能授权：" + menu.getMenuName());
            }
            result.add(menuId);
            Set<Long> visited = new HashSet<>();
            Long cursor = menu.getParentId();
            while (cursor != null && cursor != 0) {
                if (!visited.add(cursor)) {
                    throw ServiceException.badRequest("菜单层级存在循环，请先修复菜单数据");
                }
                SysMenu parent = menuMap.get(cursor);
                if (parent == null) {
                    throw ServiceException.badRequest("菜单层级不完整，缺少父菜单ID=" + cursor);
                }
                result.add(parent.getId());
                cursor = parent.getParentId();
            }
        }
        return new ArrayList<>(result);
    }

    private boolean isSuperAdmin() {
        try {
            return SecurityUtils.hasRole(RoleConstants.SUPER_ADMIN);
        } catch (Exception e) {
            return false;
        }
    }

    private int currentUserType() {
        try {
            List<String> roles = SecurityUtils.getRoles();
            if (roles.contains(RoleConstants.SUPER_ADMIN)) {
                return RoleConstants.USER_TYPE_SUPER_ADMIN;
            }
            if (roles.contains(RoleConstants.DOCTOR_ADMIN)) {
                return RoleConstants.USER_TYPE_DOCTOR_ADMIN;
            }
            return RoleConstants.USER_TYPE_STUDENT;
        } catch (Exception e) {
            return RoleConstants.USER_TYPE_STUDENT;
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() == 0 ? null : trimmed;
    }
}
