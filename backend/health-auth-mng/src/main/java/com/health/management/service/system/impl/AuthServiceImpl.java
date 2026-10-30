package com.health.management.service.system.impl;

import com.health.management.audit.AuditLogRecorder;
import com.health.management.common.constant.RoleConstants;
import com.health.management.common.dto.LoginBody;
import com.health.management.common.enums.ResultCode;
import com.health.management.common.exception.ServiceException;
import com.health.management.common.util.ValidateUtils;
import com.health.management.common.vo.MenuTree;
import com.health.management.config.JwtProperties;
import com.health.management.convert.SystemConverter;
import com.health.management.dto.ChangePasswordDTO;
import com.health.management.dto.RegisterDTO;
import com.health.management.entity.SysRole;
import com.health.management.entity.SysUser;
import com.health.management.mapper.SysMenuMapper;
import com.health.management.mapper.SysRoleMapper;
import com.health.management.mapper.SysUserMapper;
import com.health.management.security.JwtTokenUtil;
import com.health.management.security.LoginUser;
import com.health.management.security.SecurityUtils;
import com.health.management.security.TokenSessionService;
import com.health.management.service.system.AuthService;
import com.health.management.service.system.SysMenuService;
import com.health.management.service.system.SysUserService;
import com.health.management.vo.LoginVO;
import com.health.management.vo.UserInfoVO;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private SysRoleMapper roleMapper;

    @Autowired
    private SysMenuMapper menuMapper;

    @Autowired
    private SysUserService userService;

    @Autowired
    private SysMenuService menuService;

    @Autowired
    private TokenSessionService tokenSessionService;

    @Autowired
    private AuditLogRecorder auditLogRecorder;

    @Override
    public LoginVO login(LoginBody loginBody) {
        ValidateUtils.requireNotNull(loginBody, "请求参数");
        ValidateUtils.requireText(loginBody.getUsername(), "用户名");
        ValidateUtils.requireText(loginBody.getPassword(), "密码");
        String username = loginBody.getUsername().trim();

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, loginBody.getPassword()));
        } catch (BadCredentialsException e) {
            auditLogRecorder.record("用户登录", AuditLogRecorder.TYPE_OTHER, 0L, username,
                    "username=" + username, false, "账号或密码错误");
            throw new ServiceException(ResultCode.LOGIN_FAILED, "账号或密码错误");
        } catch (DisabledException e) {
            auditLogRecorder.record("用户登录", AuditLogRecorder.TYPE_OTHER, 0L, username,
                    "username=" + username, false, "账号已被禁用");
            throw new ServiceException(ResultCode.FAIL, "账号已被禁用，请联系管理员");
        } catch (LockedException e) {
            auditLogRecorder.record("用户登录", AuditLogRecorder.TYPE_OTHER, 0L, username,
                    "username=" + username, false, "账号已被锁定");
            throw new ServiceException(ResultCode.FAIL, "账号已被锁定，请联系管理员");
        } catch (AuthenticationException e) {
            auditLogRecorder.record("用户登录", AuditLogRecorder.TYPE_OTHER, 0L, username,
                    "username=" + username, false, "登录失败");
            throw new ServiceException(ResultCode.LOGIN_FAILED, "登录失败，请稍后重试");
        }

        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        SysUser user = userMapper.selectById(loginUser.getUserId());
        if (user == null) {
            auditLogRecorder.record("用户登录", AuditLogRecorder.TYPE_OTHER, 0L, username,
                    "username=" + username, false, "账号不存在或已被删除");
            throw new ServiceException(ResultCode.LOGIN_FAILED, "账号不存在或已被删除");
        }
        if (!Integer.valueOf(1).equals(user.getStatus())) {
            auditLogRecorder.record("用户登录", AuditLogRecorder.TYPE_OTHER, user.getId(), user.getUsername(),
                    "username=" + username, false, "账号已被禁用");
            throw new ServiceException(ResultCode.FAIL, "账号已被禁用，请联系管理员");
        }
        if (loginUser.getRoles() == null || loginUser.getRoles().isEmpty()) {
            auditLogRecorder.record("用户登录", AuditLogRecorder.TYPE_OTHER, user.getId(), user.getUsername(),
                    "username=" + username, false, "账号未分配有效角色");
            throw new ServiceException(ResultCode.FAIL, "账号未分配有效角色，请联系管理员");
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);
        try {
            userService.updateLoginInfo(user.getId(), auditLogRecorder.getClientIp());
        } catch (Exception e) {
            log.warn("更新登录信息失败, userId={}", user.getId(), e);
        }

        String sid = tokenSessionService.createSession(user.getId(), user.getTokenVersion());
        try {
            String accessJti = UUID.randomUUID().toString().replace("-", "");
            String accessToken = jwtTokenUtil.createAccessToken(user.getUsername(), user.getId(),
                    loginUser.getRoles(), loginUser.getRoleIds(), loginUser.getPermissions(),
                    user.getTokenVersion(), sid, accessJti);
            String refreshToken = jwtTokenUtil.createRefreshToken(user.getUsername(), user.getId(),
                    user.getTokenVersion(), sid);

            auditLogRecorder.record("用户登录", AuditLogRecorder.TYPE_OTHER, user.getId(), user.getUsername(),
                    "username=" + user.getUsername(), true, null);
            return buildLoginVO(user, loginUser.getRoles(), loginUser.getPermissions(), accessToken, refreshToken);
        } catch (RuntimeException e) {
            tokenSessionService.removeSession(user.getId(), sid);
            throw e;
        }
    }

    @Override
    public LoginVO register(RegisterDTO dto) {
        userService.registerStudent(dto);
        LoginBody loginBody = new LoginBody();
        loginBody.setUsername(dto.getUsername());
        loginBody.setPassword(dto.getPassword());
        return login(loginBody);
    }

    @Override
    public LoginVO refresh(String refreshToken) {
        ValidateUtils.requireText(refreshToken, "刷新令牌");
        Claims claims;
        try {
            claims = jwtTokenUtil.parse(refreshToken);
        } catch (Exception e) {
            throw new ServiceException(ResultCode.UNAUTHORIZED, "刷新令牌无效或已过期");
        }
        if (!JwtTokenUtil.TYPE_REFRESH.equals(jwtTokenUtil.getTokenType(claims))) {
            throw new ServiceException(ResultCode.UNAUTHORIZED, "令牌类型错误");
        }

        Long userId = jwtTokenUtil.getUserId(claims);
        Integer tokenVersion = jwtTokenUtil.getTokenVersion(claims);
        String sid = jwtTokenUtil.getSid(claims);
        if (userId == null || tokenVersion == null || sid == null
                || !tokenSessionService.validateSession(userId, sid, tokenVersion)) {
            throw new ServiceException(ResultCode.UNAUTHORIZED, "刷新令牌已失效，请重新登录");
        }

        SysUser user = userMapper.selectById(userId);
        if (user == null || !Integer.valueOf(1).equals(user.getStatus())
                || user.getTokenVersion() == null || !user.getTokenVersion().equals(tokenVersion)) {
            tokenSessionService.removeSession(userId, sid);
            throw new ServiceException(ResultCode.UNAUTHORIZED, "账号状态已变化，请重新登录");
        }

        List<SysRole> roles = roleMapper.selectEnabledRolesByUserId(userId);
        if (roles == null || roles.isEmpty()) {
            tokenSessionService.removeSession(userId, sid);
            throw new ServiceException(ResultCode.FAIL, "账号未分配有效角色，请重新登录");
        }
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
                : menuMapper.selectPermissionsByUserId(userId);
        if (permissions == null) {
            permissions = new ArrayList<>();
        }

        // Refresh Token 轮换：新会话创建成功后再删除旧会话
        String newSid = tokenSessionService.createSession(userId, tokenVersion);
        try {
            String accessJti = UUID.randomUUID().toString().replace("-", "");
            String newAccessToken = jwtTokenUtil.createAccessToken(user.getUsername(), userId,
                    roleCodes, roleIds, permissions, tokenVersion, newSid, accessJti);
            String newRefreshToken = jwtTokenUtil.createRefreshToken(user.getUsername(), userId,
                    tokenVersion, newSid);
            tokenSessionService.removeSession(userId, sid);
            auditLogRecorder.record("用户登录", AuditLogRecorder.TYPE_OTHER, userId, user.getUsername(),
                    "刷新令牌", true, null);
            return buildLoginVO(user, roleCodes, permissions, newAccessToken, newRefreshToken);
        } catch (RuntimeException e) {
            tokenSessionService.removeSession(userId, newSid);
            throw e;
        }
    }

    @Override
    public UserInfoVO getCurrentUserInfo() {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        SysUser user = userMapper.selectById(loginUser.getUserId());
        if (user == null) {
            throw ServiceException.notFound("用户不存在或已被删除");
        }
        List<SysRole> roles = roleMapper.selectEnabledRolesByUserId(user.getId());
        boolean superAdmin = false;
        for (SysRole role : roles) {
            if (RoleConstants.SUPER_ADMIN.equals(role.getRoleCode())) {
                superAdmin = true;
                break;
            }
        }
        List<String> permissions = superAdmin
                ? menuMapper.selectAllEnabledPermissions()
                : menuMapper.selectPermissionsByUserId(user.getId());
        if (permissions == null) {
            permissions = new ArrayList<>();
        }
        List<MenuTree> menus = menuService.listCurrentUserMenuTree();
        return SystemConverter.toUserInfoVO(user, roles, permissions, menus);
    }

    @Override
    public void logout() {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        Long userId = loginUser.getUserId();
        String username = loginUser.getUsername();
        String sid = loginUser.getSid();
        String jti = loginUser.getJti();
        long ttl = loginUser.getTokenExpireAt() == null
                ? 0L : loginUser.getTokenExpireAt() - System.currentTimeMillis();
        try {
            tokenSessionService.removeSession(userId, sid);
            tokenSessionService.blacklistAccessToken(jti, ttl);
        } catch (Exception e) {
            log.warn("注销会话失败, userId={}", userId, e);
        }
        SecurityContextHolder.clearContext();
        auditLogRecorder.record("用户登录", AuditLogRecorder.TYPE_FORCE_LOGOUT, userId, username,
                "退出登录", true, null);
    }

    @Override
    public void logoutAll() {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        Long userId = loginUser.getUserId();
        String username = loginUser.getUsername();
        String jti = loginUser.getJti();
        long ttl = loginUser.getTokenExpireAt() == null
                ? 0L : loginUser.getTokenExpireAt() - System.currentTimeMillis();
        try {
            tokenSessionService.removeAllSessions(userId);
            userMapper.incrementTokenVersionById(userId);
            tokenSessionService.blacklistAccessToken(jti, ttl);
        } catch (Exception e) {
            log.warn("全端注销失败, userId={}", userId, e);
        }
        SecurityContextHolder.clearContext();
        auditLogRecorder.record("用户登录", AuditLogRecorder.TYPE_FORCE_LOGOUT, userId, username,
                "退出全部设备", true, null);
    }

    @Override
    public void changePassword(ChangePasswordDTO dto) {
        ValidateUtils.requireNotNull(dto, "请求参数");
        Long userId = SecurityUtils.getUserId();
        userService.changePassword(userId, dto.getOldPassword(), dto.getNewPassword());
    }

    private LoginVO buildLoginVO(SysUser user, List<String> roles, List<String> permissions,
                                 String accessToken, String refreshToken) {
        LoginVO vo = new LoginVO();
        vo.setAccessToken(accessToken);
        vo.setRefreshToken(refreshToken);
        vo.setTokenType("Bearer");
        vo.setExpiresIn(jwtProperties.getAccessExpiration() / 1000);
        vo.setRefreshExpiresIn(jwtProperties.getRefreshExpiration() / 1000);
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setUserType(user.getUserType());
        vo.setRoles(roles);
        vo.setPermissions(permissions);
        return vo;
    }
}
