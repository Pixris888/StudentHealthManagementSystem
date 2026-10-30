package com.health.management.convert;

import com.health.management.common.constant.RoleConstants;
import com.health.management.common.vo.MenuTree;
import com.health.management.entity.SysMenu;
import com.health.management.entity.SysRole;
import com.health.management.entity.SysUser;
import com.health.management.vo.RoleDetailVO;
import com.health.management.vo.RoleVO;
import com.health.management.vo.UserInfoVO;
import com.health.management.vo.UserVO;

import java.util.ArrayList;
import java.util.List;

/**
 * 实体 → VO 转换，避免把 SysUser 的密码等敏感字段返回前端。
 */
public final class SystemConverter {

    private SystemConverter() {
    }

    public static RoleVO toRoleVO(SysRole role) {
        if (role == null) {
            return null;
        }
        RoleVO vo = new RoleVO();
        vo.setId(role.getId());
        vo.setRoleCode(role.getRoleCode());
        vo.setRoleName(role.getRoleName());
        vo.setUserType(role.getUserType());
        vo.setRoleSort(role.getRoleSort());
        vo.setDataScope(role.getDataScope());
        vo.setStatus(role.getStatus());
        vo.setRemark(role.getRemark());
        return vo;
    }

    public static List<RoleVO> toRoleVOList(List<SysRole> roles) {
        List<RoleVO> list = new ArrayList<>();
        if (roles != null) {
            for (SysRole role : roles) {
                list.add(toRoleVO(role));
            }
        }
        return list;
    }

    public static UserVO toUserVO(SysUser user, List<SysRole> roles) {
        if (user == null) {
            return null;
        }
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setNickName(user.getNickName());
        vo.setUserType(user.getUserType());
        vo.setUserTypeName(RoleConstants.userTypeName(user.getUserType()));
        vo.setStudentNo(user.getStudentNo());
        vo.setGender(user.getGender());
        vo.setAvatar(user.getAvatar());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setDeptName(user.getDeptName());
        vo.setStatus(user.getStatus());
        vo.setLoginIp(user.getLoginIp());
        vo.setLoginTime(user.getLoginTime());
        vo.setCreateTime(user.getCreateTime());
        vo.setRemark(user.getRemark());

        List<Long> roleIds = new ArrayList<>();
        if (roles != null) {
            for (SysRole role : roles) {
                roleIds.add(role.getId());
            }
        }
        vo.setRoleIds(roleIds);
        vo.setRoles(toRoleVOList(roles));
        return vo;
    }

    public static UserInfoVO toUserInfoVO(SysUser user, List<SysRole> roles,
                                          List<String> permissions, List<MenuTree> menus) {
        if (user == null) {
            return null;
        }
        UserInfoVO vo = new UserInfoVO();
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setNickName(user.getNickName());
        vo.setUserType(user.getUserType());
        vo.setUserTypeName(RoleConstants.userTypeName(user.getUserType()));
        vo.setAvatar(user.getAvatar());
        vo.setDeptName(user.getDeptName());

        List<String> roleCodes = new ArrayList<>();
        if (roles != null) {
            for (SysRole role : roles) {
                roleCodes.add(role.getRoleCode());
            }
        }
        vo.setRoles(roleCodes);
        vo.setPermissions(permissions == null ? new ArrayList<String>() : permissions);
        vo.setMenus(menus == null ? new ArrayList<MenuTree>() : menus);
        return vo;
    }

    public static RoleDetailVO toRoleDetailVO(SysRole role, List<Long> menuIds) {
        if (role == null) {
            return null;
        }
        RoleDetailVO vo = new RoleDetailVO();
        vo.setId(role.getId());
        vo.setRoleCode(role.getRoleCode());
        vo.setRoleName(role.getRoleName());
        vo.setUserType(role.getUserType());
        vo.setRoleSort(role.getRoleSort());
        vo.setDataScope(role.getDataScope());
        vo.setStatus(role.getStatus());
        vo.setRemark(role.getRemark());
        vo.setMenuIds(menuIds == null ? new ArrayList<Long>() : menuIds);
        return vo;
    }

    public static MenuTree toMenuTree(SysMenu menu) {
        if (menu == null) {
            return null;
        }
        MenuTree tree = new MenuTree();
        tree.setId(menu.getId());
        tree.setParentId(menu.getParentId());
        tree.setMenuName(menu.getMenuName());
        tree.setPath(menu.getPath());
        tree.setComponent(menu.getComponent());
        tree.setPerms(menu.getPerms());
        tree.setIcon(menu.getIcon());
        tree.setMenuType(menu.getMenuType());
        tree.setOrderNum(menu.getOrderNum());
        tree.setStatus(menu.getStatus());
        return tree;
    }
}
