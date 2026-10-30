package com.health.management.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.health.management.common.constant.RoleConstants;
import com.health.management.entity.SysRole;
import com.health.management.entity.SysUser;
import com.health.management.mapper.SysMenuMapper;
import com.health.management.mapper.SysRoleMapper;
import com.health.management.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Spring Security 用户详情服务：加载用户、启用角色和菜单权限。
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private SysRoleMapper roleMapper;

    @Autowired
    private SysMenuMapper menuMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        if (user == null) {
            throw new UsernameNotFoundException("账号或密码错误");
        }

        // 账号状态和“是否有有效角色”在密码校验通过后由 AuthServiceImpl 检查，
        // 避免在密码校验前泄露账号状态，也避免 Spring 把 DisabledException 包装掉。
        List<SysRole> roles = roleMapper.selectEnabledRolesByUserId(user.getId());
        List<String> roleCodes = new ArrayList<>();
        List<Long> roleIds = new ArrayList<>();
        boolean superAdmin = false;
        for (SysRole role : roles) {
            roleCodes.add(role.getRoleCode());
            roleIds.add(role.getId());
            if (RoleConstants.SUPER_ADMIN.equals(role.getRoleCode())) {
                superAdmin = true;
            }
        }

        List<String> permissions = superAdmin
                ? menuMapper.selectAllEnabledPermissions()
                : menuMapper.selectPermissionsByUserId(user.getId());

        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(user.getId());
        loginUser.setUsername(user.getUsername());
        loginUser.setPassword(user.getPassword());
        loginUser.setRoles(roleCodes);
        loginUser.setRoleIds(roleIds);
        loginUser.setPermissions(permissions == null ? new ArrayList<String>() : permissions);
        loginUser.buildAuthorities();
        return loginUser;
    }
}
