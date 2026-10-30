package com.health.management.service.system.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.health.management.audit.AuditLogRecorder;
import com.health.management.common.constant.RoleConstants;
import com.health.management.common.exception.ServiceException;
import com.health.management.common.util.ValidateUtils;
import com.health.management.common.vo.MenuTree;
import com.health.management.convert.SystemConverter;
import com.health.management.dto.MenuSaveDTO;
import com.health.management.entity.SysMenu;
import com.health.management.entity.SysRoleMenu;
import com.health.management.mapper.SysMenuMapper;
import com.health.management.mapper.SysRoleMenuMapper;
import com.health.management.mapper.SysUserRoleMapper;
import com.health.management.security.LoginUser;
import com.health.management.security.SecurityUtils;
import com.health.management.security.TokenSessionService;
import com.health.management.service.system.SysMenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class SysMenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu> implements SysMenuService {

    @Autowired
    private SysRoleMenuMapper roleMenuMapper;

    @Autowired
    private SysUserRoleMapper userRoleMapper;

    @Autowired
    private AuditLogRecorder auditLogRecorder;

    @Autowired
    private TokenSessionService tokenSessionService;

    @Override
    public List<MenuTree> listMenuTree() {
        List<SysMenu> menus = list(new LambdaQueryWrapper<SysMenu>()
                .orderByAsc(SysMenu::getParentId)
                .orderByAsc(SysMenu::getOrderNum)
                .orderByAsc(SysMenu::getId));
        return buildTree(menus, true);
    }

    @Override
    public List<MenuTree> listCurrentUserMenuTree() {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        List<SysMenu> menus;
        if (loginUser.getRoles() != null && loginUser.getRoles().contains(RoleConstants.SUPER_ADMIN)) {
            menus = list(new LambdaQueryWrapper<SysMenu>()
                    .eq(SysMenu::getStatus, 1)
                    .orderByAsc(SysMenu::getParentId)
                    .orderByAsc(SysMenu::getOrderNum)
                    .orderByAsc(SysMenu::getId));
        } else {
            menus = baseMapper.selectMenusByUserId(loginUser.getUserId());
        }
        return buildTree(menus, false);
    }

    @Override
    public SysMenu getMenuDetail(Long menuId) {
        SysMenu menu = getById(menuId);
        if (menu == null) {
            throw ServiceException.notFound("菜单不存在");
        }
        return menu;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createMenu(MenuSaveDTO dto) {
        validateMenu(dto, null);
        SysMenu menu = new SysMenu();
        copyMenu(dto, menu);
        save(menu);
        auditLogRecorder.record("菜单管理", AuditLogRecorder.TYPE_ADD,
                "新增菜单：" + menu.getMenuName(), true, null);
        return menu.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMenu(MenuSaveDTO dto) {
        ValidateUtils.requireNotNull(dto.getId(), "菜单ID");
        SysMenu existing = getById(dto.getId());
        if (existing == null) {
            throw ServiceException.notFound("菜单不存在");
        }
        if (!Integer.valueOf(3).equals(existing.getMenuType()) && Integer.valueOf(3).equals(dto.getMenuType())) {
            long childCount = count(new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getParentId, existing.getId()));
            if (childCount > 0) {
                throw ServiceException.badRequest("该菜单下存在子菜单，不能修改为按钮类型");
            }
        }
        validateMenu(dto, existing);
        copyMenu(dto, existing);
        updateById(existing);
        refreshUsersOfMenu(existing.getId());
        auditLogRecorder.record("菜单管理", AuditLogRecorder.TYPE_UPDATE,
                "修改菜单：" + existing.getMenuName(), true, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMenu(Long menuId) {
        SysMenu existing = getById(menuId);
        if (existing == null) {
            throw ServiceException.notFound("菜单不存在");
        }
        long childCount = count(new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getParentId, menuId));
        if (childCount > 0) {
            throw ServiceException.badRequest("存在子菜单，不能删除");
        }
        if (roleMenuMapper.countByMenuId(menuId) > 0) {
            throw ServiceException.badRequest("菜单已被角色引用，请先解除授权");
        }
        removeById(menuId);
        auditLogRecorder.record("菜单管理", AuditLogRecorder.TYPE_DELETE,
                "删除菜单：" + existing.getMenuName(), true, null);
    }

    @Override
    public List<String> getPermissionsByUserId(Long userId) {
        return baseMapper.selectPermissionsByUserId(userId);
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

    private void validateMenu(MenuSaveDTO dto, SysMenu existing) {
        ValidateUtils.requireNotNull(dto, "请求参数");
        ValidateUtils.requireText(dto.getMenuName(), "菜单名称");
        ValidateUtils.requireMaxLength(dto.getMenuName(), 50, "菜单名称");
        ValidateUtils.requireMenuType(dto.getMenuType());
        if (dto.getStatus() != null) {
            ValidateUtils.requireStatus(dto.getStatus());
        }

        Long parentId = dto.getParentId() == null ? 0L : dto.getParentId();
        if (parentId != 0) {
            SysMenu parent = getById(parentId);
            if (parent == null) {
                throw ServiceException.badRequest("父菜单不存在");
            }
            if (Integer.valueOf(3).equals(dto.getMenuType())) {
                if (!Integer.valueOf(2).equals(parent.getMenuType())) {
                    throw ServiceException.badRequest("按钮必须挂在菜单下");
                }
            } else if (!Integer.valueOf(1).equals(parent.getMenuType())) {
                throw ServiceException.badRequest("目录/菜单必须挂在目录下");
            }
            if (existing != null) {
                checkNoCycle(parentId, existing.getId());
            }
        }

        if (Integer.valueOf(3).equals(dto.getMenuType())) {
            ValidateUtils.requirePerms(dto.getPerms());
        } else if (Integer.valueOf(1).equals(dto.getMenuType())) {
            ValidateUtils.requireText(dto.getPath(), "路由地址");
            ValidateUtils.requireMaxLength(dto.getPath(), 100, "路由地址");
            if (ValidateUtils.hasText(dto.getPerms())) {
                throw ServiceException.badRequest("目录不能设置权限标识");
            }
        } else {
            ValidateUtils.requireText(dto.getPath(), "路由地址");
            ValidateUtils.requireMaxLength(dto.getPath(), 100, "路由地址");
            if (ValidateUtils.hasText(dto.getPerms())) {
                ValidateUtils.requirePerms(dto.getPerms());
            }
        }

        if (ValidateUtils.hasText(dto.getPerms())) {
            int permCount = existing == null
                    ? baseMapper.countByPermsIncludingDeleted(dto.getPerms())
                    : baseMapper.countByPermsExcludingId(dto.getPerms(), existing.getId());
            if (permCount > 0) {
                throw ServiceException.conflict("权限标识已存在：" + dto.getPerms());
            }
        }

        long nameCount = baseMapper.countByMenuNameAndParent(
                dto.getMenuName().trim(), parentId, existing == null ? 0L : existing.getId());
        if (nameCount > 0) {
            throw ServiceException.conflict("同一父级下菜单名称已存在");
        }
    }

    private void checkNoCycle(Long parentId, Long menuId) {
        Set<Long> visited = new HashSet<>();
        Long cursor = parentId;
        while (cursor != null && cursor != 0) {
            if (cursor.equals(menuId)) {
                throw ServiceException.badRequest("不能选择自己或子菜单作为父级");
            }
            if (!visited.add(cursor)) {
                throw ServiceException.badRequest("菜单层级存在循环，请检查父级设置");
            }
            SysMenu parent = getById(cursor);
            cursor = parent == null || parent.getParentId() == null ? 0L : parent.getParentId();
        }
    }

    private void copyMenu(MenuSaveDTO dto, SysMenu menu) {
        menu.setMenuName(dto.getMenuName().trim());
        menu.setParentId(dto.getParentId() == null ? 0L : dto.getParentId());
        menu.setMenuType(dto.getMenuType());
        menu.setOrderNum(dto.getOrderNum() == null ? 0 : dto.getOrderNum());
        menu.setPath(Integer.valueOf(3).equals(dto.getMenuType()) ? null : trimToNull(dto.getPath()));
        menu.setComponent(Integer.valueOf(3).equals(dto.getMenuType()) ? null : trimToNull(dto.getComponent()));
        menu.setPerms(Integer.valueOf(1).equals(dto.getMenuType()) ? null : trimToNull(dto.getPerms()));
        menu.setIcon(trimToNull(dto.getIcon()));
        menu.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        menu.setRemark(trimToNull(dto.getRemark()));
    }

    private List<MenuTree> buildTree(List<SysMenu> menus, boolean includeButtons) {
        List<SysMenu> filtered = new ArrayList<>();
        for (SysMenu menu : menus) {
            if (!includeButtons && Integer.valueOf(3).equals(menu.getMenuType())) {
                continue;
            }
            filtered.add(menu);
        }
        filtered.sort(Comparator
                .comparing(SysMenu::getOrderNum, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(SysMenu::getId, Comparator.nullsLast(Long::compareTo)));

        Map<Long, MenuTree> nodeMap = new LinkedHashMap<>();
        for (SysMenu menu : filtered) {
            nodeMap.put(menu.getId(), SystemConverter.toMenuTree(menu));
        }

        // 只保留能从根节点可达的菜单，自动过滤孤儿节点和环
        Set<Long> valid = new HashSet<>();
        for (SysMenu menu : filtered) {
            if (menu.getParentId() == null || menu.getParentId() == 0) {
                valid.add(menu.getId());
            }
        }
        boolean changed = true;
        while (changed) {
            changed = false;
            for (SysMenu menu : filtered) {
                Long parentId = menu.getParentId();
                if (parentId != null && parentId != 0
                        && valid.contains(parentId) && !valid.contains(menu.getId())) {
                    valid.add(menu.getId());
                    changed = true;
                }
            }
        }

        List<MenuTree> roots = new ArrayList<>();
        for (SysMenu menu : filtered) {
            if (!valid.contains(menu.getId())) {
                continue;
            }
            MenuTree node = nodeMap.get(menu.getId());
            Long parentId = menu.getParentId();
            if (parentId == null || parentId == 0) {
                roots.add(node);
            } else {
                MenuTree parent = nodeMap.get(parentId);
                if (parent != null) {
                    parent.getChildren().add(node);
                }
            }
        }
        return roots;
    }

    private void refreshUsersOfMenu(Long menuId) {
        List<SysRoleMenu> relations = roleMenuMapper.selectByMenuId(menuId);
        for (SysRoleMenu relation : relations) {
            userRoleMapper.incrementTokenVersionByRoleId(relation.getRoleId());
            tokenSessionService.removeAllSessions(
                    userRoleMapper.selectUserIdsByRoleId(relation.getRoleId()));
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
