package com.health.management.service.system.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.health.management.audit.AuditLogRecorder;
import com.health.management.common.constant.RoleConstants;
import com.health.management.common.exception.ServiceException;
import com.health.management.common.util.ValidateUtils;
import com.health.management.convert.SystemConverter;
import com.health.management.dto.RegisterDTO;
import com.health.management.dto.UserCreateDTO;
import com.health.management.dto.UserQueryDTO;
import com.health.management.dto.UserUpdateDTO;
import com.health.management.entity.SysRole;
import com.health.management.entity.SysUser;
import com.health.management.entity.SysUserRole;
import com.health.management.mapper.SysRoleMapper;
import com.health.management.mapper.SysUserMapper;
import com.health.management.mapper.SysUserRoleMapper;
import com.health.management.security.SecurityUtils;
import com.health.management.security.TokenSessionService;
import com.health.management.service.SysUserRoleService;
import com.health.management.service.system.SysUserService;
import com.health.management.vo.UserVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    @Autowired
    private SysUserRoleMapper userRoleMapper;

    @Autowired
    private SysRoleMapper roleMapper;

    @Autowired
    private SysUserRoleService userRoleService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuditLogRecorder auditLogRecorder;

    @Autowired
    private TokenSessionService tokenSessionService;

    @Override
    public IPage<UserVO> pageUsers(UserQueryDTO query) {
        if (query == null) {
            query = new UserQueryDTO();
        }
        ValidateUtils.requirePage(query.getPage(), query.getSize());
        if (query.getUserType() != null) {
            ValidateUtils.requireUserType(query.getUserType());
        }
        if (query.getStatus() != null) {
            ValidateUtils.requireStatus(query.getStatus());
        }
        int current = query.getPage() == null ? 1 : query.getPage();
        int size = query.getSize() == null ? 10 : query.getSize();

        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (ValidateUtils.hasText(query.getUsername())) {
            wrapper.like(SysUser::getUsername, query.getUsername().trim());
        }
        if (ValidateUtils.hasText(query.getRealName())) {
            wrapper.like(SysUser::getRealName, query.getRealName().trim());
        }
        if (query.getUserType() != null) {
            wrapper.eq(SysUser::getUserType, query.getUserType());
        }
        if (query.getStatus() != null) {
            wrapper.eq(SysUser::getStatus, query.getStatus());
        }
        if (ValidateUtils.hasText(query.getDeptName())) {
            wrapper.like(SysUser::getDeptName, query.getDeptName().trim());
        }
        wrapper.orderByDesc(SysUser::getId);

        IPage<SysUser> page = page(new Page<>(current, size), wrapper);
        List<Long> userIds = new ArrayList<>();
        for (SysUser user : page.getRecords()) {
            userIds.add(user.getId());
        }
        Map<Long, List<SysRole>> roleMap = loadRoleMap(userIds);

        Page<UserVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<UserVO> records = new ArrayList<>();
        for (SysUser user : page.getRecords()) {
            records.add(SystemConverter.toUserVO(user, roleMap.get(user.getId())));
        }
        voPage.setRecords(records);
        return voPage;
    }

    @Override
    public UserVO getUserDetail(Long userId) {
        SysUser user = getById(userId);
        if (user == null) {
            throw ServiceException.notFound("用户不存在");
        }
        return SystemConverter.toUserVO(user, loadRoles(userId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createUser(UserCreateDTO dto) {
        ValidateUtils.requireNotNull(dto, "请求参数");
        ValidateUtils.requireUsername(dto.getUsername());
        ValidateUtils.requirePassword(dto.getPassword());
        ValidateUtils.requireText(dto.getRealName(), "真实姓名");
        ValidateUtils.requireMaxLength(dto.getRealName(), 50, "真实姓名");
        ValidateUtils.requireGender(dto.getGender());
        validateContactFields(dto.getPhone(), dto.getEmail(), dto.getNickName(),
                dto.getAvatar(), dto.getDeptName(), dto.getRemark());

        List<Long> roleIds = dedupeIds(dto.getRoleIds(), "角色");
        List<SysRole> roles = resolveRoles(roleIds);
        validateOperatorCanAssign(roles);
        int userType = resolveUserType(roles);
        if (dto.getUserType() != null && !Integer.valueOf(userType).equals(dto.getUserType())) {
            throw ServiceException.badRequest("用户类型与所选角色不一致");
        }

        if (baseMapper.countByUsernameIncludingDeleted(dto.getUsername().trim()) > 0) {
            throw ServiceException.conflict("用户名已存在（可能被已删除账号占用）");
        }

        String studentNo = null;
        if (Integer.valueOf(RoleConstants.USER_TYPE_STUDENT).equals(userType)) {
            ValidateUtils.requireStudentNo(dto.getStudentNo());
            studentNo = dto.getStudentNo().trim();
            if (baseMapper.countByStudentNoIncludingDeleted(studentNo) > 0) {
                throw ServiceException.conflict("学号已存在（可能被已删除账号占用）");
            }
        } else if (ValidateUtils.hasText(dto.getStudentNo())) {
            throw ServiceException.badRequest("非学生账号不能填写学号");
        }
        ValidateUtils.requirePasswordNotContains(dto.getPassword(), dto.getUsername(), studentNo);

        SysUser user = new SysUser();
        user.setUsername(dto.getUsername().trim());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRealName(dto.getRealName().trim());
        user.setNickName(ValidateUtils.hasText(dto.getNickName())
                ? dto.getNickName().trim() : dto.getRealName().trim());
        user.setUserType(userType);
        user.setStudentNo(studentNo);
        user.setGender(dto.getGender() == null ? 0 : dto.getGender());
        user.setAvatar(trimToNull(dto.getAvatar()));
        user.setPhone(trimToNull(dto.getPhone()));
        user.setEmail(trimToNull(dto.getEmail()));
        user.setDeptName(trimToNull(dto.getDeptName()));
        user.setStatus(1);
        user.setTokenVersion(0);
        user.setRemark(trimToNull(dto.getRemark()));

        try {
            save(user);
        } catch (DuplicateKeyException e) {
            throw ServiceException.conflict("用户名或学号已存在");
        }
        saveUserRoles(user.getId(), roleIds);

        auditLogRecorder.record("用户管理", AuditLogRecorder.TYPE_ADD,
                "新增用户：" + user.getUsername(), true, null);
        return user.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long registerStudent(RegisterDTO dto) {
        ValidateUtils.requireNotNull(dto, "请求参数");
        ValidateUtils.requireUsername(dto.getUsername());
        ValidateUtils.requirePassword(dto.getPassword());
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw ServiceException.badRequest("两次输入的密码不一致");
        }
        ValidateUtils.requireText(dto.getRealName(), "真实姓名");
        ValidateUtils.requireMaxLength(dto.getRealName(), 50, "真实姓名");
        ValidateUtils.requireStudentNo(dto.getStudentNo());
        ValidateUtils.requireGender(dto.getGender());
        ValidateUtils.requirePhone(dto.getPhone());
        ValidateUtils.requireEmail(dto.getEmail());
        ValidateUtils.requireMaxLength(dto.getDeptName(), 100, "院系/部门");

        String username = dto.getUsername().trim();
        String studentNo = dto.getStudentNo().trim();
        ValidateUtils.requirePasswordNotContains(dto.getPassword(), username, studentNo);

        if (baseMapper.countByUsernameIncludingDeleted(username) > 0) {
            throw ServiceException.conflict("用户名已存在（可能被已删除账号占用）");
        }
        if (baseMapper.countByStudentNoIncludingDeleted(studentNo) > 0) {
            throw ServiceException.conflict("学号已存在（可能被已删除账号占用）");
        }

        SysRole studentRole = roleMapper.selectEnabledByRoleCode(RoleConstants.STUDENT);
        if (studentRole == null) {
            throw ServiceException.fail("系统未配置启用的学生角色，请联系管理员");
        }

        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRealName(dto.getRealName().trim());
        user.setNickName(dto.getRealName().trim());
        user.setUserType(RoleConstants.USER_TYPE_STUDENT);
        user.setStudentNo(studentNo);
        user.setGender(dto.getGender() == null ? 0 : dto.getGender());
        user.setPhone(trimToNull(dto.getPhone()));
        user.setEmail(trimToNull(dto.getEmail()));
        user.setDeptName(trimToNull(dto.getDeptName()));
        user.setStatus(1);
        user.setTokenVersion(0);

        try {
            save(user);
        } catch (DuplicateKeyException e) {
            throw ServiceException.conflict("用户名或学号已存在");
        }
        saveUserRoles(user.getId(), Collections.singletonList(studentRole.getId()));

        auditLogRecorder.record("用户注册", AuditLogRecorder.TYPE_ADD, user.getId(), user.getUsername(),
                "学生自主注册：" + user.getUsername(), true, null);
        return user.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(UserUpdateDTO dto) {
        ValidateUtils.requireNotNull(dto.getId(), "用户ID");
        SysUser existing = getById(dto.getId());
        if (existing == null) {
            throw ServiceException.notFound("用户不存在");
        }
        ValidateUtils.requireText(dto.getRealName(), "真实姓名");
        ValidateUtils.requireMaxLength(dto.getRealName(), 50, "真实姓名");
        ValidateUtils.requireGender(dto.getGender());
        validateContactFields(dto.getPhone(), dto.getEmail(), dto.getNickName(),
                dto.getAvatar(), dto.getDeptName(), dto.getRemark());

        String studentNo = dto.getStudentNo();
        if (Integer.valueOf(RoleConstants.USER_TYPE_STUDENT).equals(existing.getUserType())) {
            if (!ValidateUtils.hasText(studentNo)) {
                studentNo = existing.getStudentNo();
            }
            ValidateUtils.requireStudentNo(studentNo);
            studentNo = studentNo.trim();
            if (!studentNo.equals(existing.getStudentNo())
                    && baseMapper.countByStudentNoExcludingId(studentNo, existing.getId()) > 0) {
                throw ServiceException.conflict("学号已存在");
            }
        } else {
            if (ValidateUtils.hasText(studentNo)) {
                throw ServiceException.badRequest("非学生账号不能填写学号");
            }
            studentNo = null;
        }

        LambdaUpdateWrapper<SysUser> update = new LambdaUpdateWrapper<>();
        update.eq(SysUser::getId, existing.getId())
                .set(SysUser::getRealName, dto.getRealName().trim())
                .set(SysUser::getNickName, trimToNull(dto.getNickName()))
                .set(SysUser::getGender, dto.getGender() == null ? 0 : dto.getGender())
                .set(SysUser::getAvatar, trimToNull(dto.getAvatar()))
                .set(SysUser::getPhone, trimToNull(dto.getPhone()))
                .set(SysUser::getEmail, trimToNull(dto.getEmail()))
                .set(SysUser::getDeptName, trimToNull(dto.getDeptName()))
                .set(SysUser::getStudentNo, studentNo)
                .set(SysUser::getRemark, trimToNull(dto.getRemark()))
                .set(SysUser::getUpdateTime, LocalDateTime.now())
                .set(SysUser::getUpdateBy, currentUserIdOrZero());
        baseMapper.update(null, update);

        auditLogRecorder.record("用户管理", AuditLogRecorder.TYPE_UPDATE,
                "修改用户：" + existing.getUsername(), true, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUsers(String ids) {
        List<Long> idList = ValidateUtils.parseIds(ids);
        Long currentUserId = currentUserIdOrNull();
        roleMapper.selectByRoleCodeForUpdate(RoleConstants.SUPER_ADMIN);

        for (Long userId : idList) {
            SysUser user = baseMapper.selectByIdForUpdate(userId);
            if (user == null) {
                throw ServiceException.notFound("用户不存在，ID=" + userId);
            }
            if (currentUserId != null && currentUserId.equals(userId)) {
                throw ServiceException.forbidden("不能删除当前登录账号");
            }
            if (Integer.valueOf(1).equals(user.getStatus()) && hasSuperAdminRole(userId)
                    && userRoleMapper.countEnabledUsersByRoleCode(RoleConstants.SUPER_ADMIN) <= 1) {
                throw ServiceException.forbidden("不能删除最后一个启用的超级管理员");
            }
            userRoleMapper.deleteByUserIdPhysically(userId);
            removeById(userId);
            tokenSessionService.removeAllSessions(userId);
        }
        auditLogRecorder.record("用户管理", AuditLogRecorder.TYPE_DELETE,
                "删除用户ID：" + ids, true, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long userId, String newPassword) {
        SysUser user = getById(userId);
        if (user == null) {
            throw ServiceException.notFound("用户不存在");
        }
        ValidateUtils.requirePassword(newPassword);
        ValidateUtils.requirePasswordNotContains(newPassword, user.getUsername(), user.getStudentNo());

        LambdaUpdateWrapper<SysUser> update = new LambdaUpdateWrapper<>();
        update.eq(SysUser::getId, userId)
                .set(SysUser::getPassword, passwordEncoder.encode(newPassword))
                .set(SysUser::getUpdateTime, LocalDateTime.now())
                .set(SysUser::getUpdateBy, currentUserIdOrZero());
        baseMapper.update(null, update);
        baseMapper.incrementTokenVersionById(userId);
        tokenSessionService.removeAllSessions(userId);

        auditLogRecorder.record("用户管理", AuditLogRecorder.TYPE_RESET_PWD,
                "重置密码：" + user.getUsername(), true, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long userId, Integer status) {
        ValidateUtils.requireStatus(status);
        SysUser user = baseMapper.selectByIdForUpdate(userId);
        if (user == null) {
            throw ServiceException.notFound("用户不存在");
        }
        Long currentUserId = currentUserIdOrNull();
        if (currentUserId != null && currentUserId.equals(userId)) {
            throw ServiceException.forbidden("不能修改自己的账号状态");
        }
        if (Integer.valueOf(0).equals(status)) {
            roleMapper.selectByRoleCodeForUpdate(RoleConstants.SUPER_ADMIN);
            if (hasSuperAdminRole(userId)
                    && userRoleMapper.countEnabledUsersByRoleCode(RoleConstants.SUPER_ADMIN) <= 1) {
                throw ServiceException.forbidden("不能禁用最后一个启用的超级管理员");
            }
        }

        LambdaUpdateWrapper<SysUser> update = new LambdaUpdateWrapper<>();
        update.eq(SysUser::getId, userId)
                .set(SysUser::getStatus, status)
                .set(SysUser::getUpdateTime, LocalDateTime.now())
                .set(SysUser::getUpdateBy, currentUserIdOrZero());
        baseMapper.update(null, update);
        if (Integer.valueOf(0).equals(status)) {
            baseMapper.incrementTokenVersionById(userId);
            tokenSessionService.removeAllSessions(userId);
        }

        auditLogRecorder.record("用户管理", AuditLogRecorder.TYPE_UPDATE,
                (status == 1 ? "启用" : "禁用") + "账号：" + user.getUsername(), true, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRoles(Long userId, List<Long> roleIds) {
        SysUser user = baseMapper.selectByIdForUpdate(userId);
        if (user == null) {
            throw ServiceException.notFound("用户不存在");
        }
        Long currentUserId = currentUserIdOrNull();
        if (currentUserId != null && currentUserId.equals(userId)) {
            throw ServiceException.forbidden("不能修改自己的角色");
        }

        List<Long> idList = dedupeIds(roleIds, "角色");
        List<SysRole> roles = resolveRoles(idList);
        validateOperatorCanAssign(roles);
        roleMapper.selectByRoleCodeForUpdate(RoleConstants.SUPER_ADMIN);

        boolean newHasSuperAdmin = false;
        for (SysRole role : roles) {
            if (RoleConstants.SUPER_ADMIN.equals(role.getRoleCode())) {
                newHasSuperAdmin = true;
            }
        }
        if (hasSuperAdminRole(userId) && !newHasSuperAdmin
                && Integer.valueOf(1).equals(user.getStatus())
                && userRoleMapper.countEnabledUsersByRoleCode(RoleConstants.SUPER_ADMIN) <= 1) {
            throw ServiceException.forbidden("不能移除最后一个启用超级管理员的超级管理员角色");
        }

        int newUserType = resolveUserType(roles);
        if (Integer.valueOf(RoleConstants.USER_TYPE_STUDENT).equals(newUserType)
                && !ValidateUtils.hasText(user.getStudentNo())) {
            throw ServiceException.badRequest("学生账号必须填写学号，请先修改用户信息补充学号");
        }

        userRoleMapper.deleteByUserIdPhysically(userId);
        saveUserRoles(userId, idList);

        LambdaUpdateWrapper<SysUser> update = new LambdaUpdateWrapper<>();
        update.eq(SysUser::getId, userId)
                .set(SysUser::getUserType, newUserType)
                .set(SysUser::getUpdateTime, LocalDateTime.now())
                .set(SysUser::getUpdateBy, currentUserIdOrZero())
                .setSql("token_version = token_version + 1");
        if (!Integer.valueOf(RoleConstants.USER_TYPE_STUDENT).equals(newUserType)) {
            update.set(SysUser::getStudentNo, null);
        }
        baseMapper.update(null, update);
        tokenSessionService.removeAllSessions(userId);

        auditLogRecorder.record("用户管理", AuditLogRecorder.TYPE_GRANT,
                "分配角色：" + user.getUsername() + ", 角色数=" + idList.size(), true, null);
    }

    @Override
    public List<Long> getRoleIdsByUserId(Long userId) {
        List<SysUserRole> relations = userRoleMapper.selectByUserId(userId);
        List<Long> ids = new ArrayList<>();
        for (SysUserRole relation : relations) {
            ids.add(relation.getRoleId());
        }
        return ids;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        SysUser user = getById(userId);
        if (user == null) {
            throw ServiceException.notFound("用户不存在");
        }
        ValidateUtils.requireText(oldPassword, "原密码");
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw ServiceException.badRequest("原密码错误");
        }
        ValidateUtils.requirePassword(newPassword);
        ValidateUtils.requirePasswordNotContains(newPassword, user.getUsername(), user.getStudentNo());
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw ServiceException.badRequest("新密码不能与原密码相同");
        }

        LambdaUpdateWrapper<SysUser> update = new LambdaUpdateWrapper<>();
        update.eq(SysUser::getId, userId)
                .set(SysUser::getPassword, passwordEncoder.encode(newPassword))
                .set(SysUser::getUpdateTime, LocalDateTime.now())
                .set(SysUser::getUpdateBy, userId)
                .setSql("token_version = token_version + 1");
        baseMapper.update(null, update);
        tokenSessionService.removeAllSessions(userId);

        auditLogRecorder.record("用户管理", AuditLogRecorder.TYPE_RESET_PWD,
                "修改本人密码：" + user.getUsername(), true, null);
    }

    @Override
    public void updateLoginInfo(Long userId, String loginIp) {
        LambdaUpdateWrapper<SysUser> update = new LambdaUpdateWrapper<>();
        update.eq(SysUser::getId, userId)
                .set(SysUser::getLoginIp, loginIp == null ? "" : loginIp)
                .set(SysUser::getLoginTime, LocalDateTime.now())
                .set(SysUser::getUpdateTime, LocalDateTime.now())
                .set(SysUser::getUpdateBy, userId);
        baseMapper.update(null, update);
    }

    private Map<Long, List<SysRole>> loadRoleMap(List<Long> userIds) {
        Map<Long, List<SysRole>> result = new HashMap<>();
        if (userIds == null || userIds.isEmpty()) {
            return result;
        }
        List<SysUserRole> relations = userRoleMapper.selectByUserIds(userIds);
        Map<Long, List<Long>> userRoleIds = new HashMap<>();
        Set<Long> roleIds = new LinkedHashSet<>();
        for (SysUserRole relation : relations) {
            List<Long> ids = userRoleIds.get(relation.getUserId());
            if (ids == null) {
                ids = new ArrayList<>();
                userRoleIds.put(relation.getUserId(), ids);
            }
            ids.add(relation.getRoleId());
            roleIds.add(relation.getRoleId());
        }
        if (roleIds.isEmpty()) {
            return result;
        }
        List<SysRole> roles = roleMapper.selectBatchIds(roleIds);
        Map<Long, SysRole> roleById = new HashMap<>();
        for (SysRole role : roles) {
            roleById.put(role.getId(), role);
        }
        for (Map.Entry<Long, List<Long>> entry : userRoleIds.entrySet()) {
            List<SysRole> userRoles = new ArrayList<>();
            for (Long roleId : entry.getValue()) {
                SysRole role = roleById.get(roleId);
                if (role != null) {
                    userRoles.add(role);
                }
            }
            result.put(entry.getKey(), userRoles);
        }
        return result;
    }

    private List<SysRole> loadRoles(Long userId) {
        List<SysUserRole> relations = userRoleMapper.selectByUserId(userId);
        List<Long> roleIds = new ArrayList<>();
        for (SysUserRole relation : relations) {
            roleIds.add(relation.getRoleId());
        }
        return roleIds.isEmpty() ? new ArrayList<SysRole>() : roleMapper.selectBatchIds(roleIds);
    }

    private List<Long> dedupeIds(List<Long> ids, String fieldName) {
        Set<Long> set = new LinkedHashSet<>();
        if (ids != null) {
            for (Long id : ids) {
                if (id == null || id <= 0) {
                    throw ServiceException.badRequest(fieldName + "ID非法");
                }
                set.add(id);
            }
        }
        if (set.isEmpty()) {
            throw ServiceException.badRequest("至少选择一个" + fieldName);
        }
        return new ArrayList<>(set);
    }

    private List<SysRole> resolveRoles(List<Long> roleIds) {
        List<SysRole> roles = roleMapper.selectBatchIds(roleIds);
        if (roles == null || roles.size() != roleIds.size()) {
            throw ServiceException.badRequest("角色不存在或已删除");
        }
        Integer userType = null;
        boolean hasBuiltIn = false;
        for (SysRole role : roles) {
            if (!Integer.valueOf(1).equals(role.getStatus())) {
                throw ServiceException.badRequest("角色已停用：" + role.getRoleName());
            }
            if (role.getUserType() == null) {
                throw ServiceException.badRequest("角色未配置用户类型：" + role.getRoleName());
            }
            if (userType == null) {
                userType = role.getUserType();
            } else if (!userType.equals(role.getUserType())) {
                throw ServiceException.badRequest("不能同时分配不同用户类型的角色");
            }
            if (RoleConstants.isBuiltIn(role.getRoleCode())) {
                hasBuiltIn = true;
            }
        }
        if (!hasBuiltIn) {
            throw ServiceException.badRequest("角色中至少包含一个内置角色（学生/校医科普管理员/超级管理员）");
        }
        return roles;
    }

    private int resolveUserType(List<SysRole> roles) {
        return roles.get(0).getUserType();
    }

    private void validateOperatorCanAssign(List<SysRole> roles) {
        int operatorType = currentUserType();
        for (SysRole role : roles) {
            if (role.getUserType() != null && role.getUserType() > operatorType) {
                throw ServiceException.forbidden("不能分配高于自己权限级别的角色");
            }
            if (Integer.valueOf(RoleConstants.USER_TYPE_SUPER_ADMIN).equals(role.getUserType())
                    && !isSuperAdmin()) {
                throw ServiceException.forbidden("只有超级管理员才能分配超级管理员角色");
            }
        }
    }

    /**
     * 当前操作人的用户类型：超级管理员3 / 校医管理员2 / 其他1
     */
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

    private void saveUserRoles(Long userId, List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        List<SysUserRole> relations = new ArrayList<>();
        for (Long roleId : roleIds) {
            SysUserRole relation = new SysUserRole();
            relation.setUserId(userId);
            relation.setRoleId(roleId);
            relations.add(relation);
        }
        userRoleService.saveBatch(relations);
    }

    private boolean hasSuperAdminRole(Long userId) {
        return userRoleMapper.countByUserIdAndRoleCode(userId, RoleConstants.SUPER_ADMIN) > 0;
    }

    private boolean isSuperAdmin() {
        try {
            return SecurityUtils.hasRole(RoleConstants.SUPER_ADMIN);
        } catch (Exception e) {
            return false;
        }
    }

    private Long currentUserIdOrNull() {
        try {
            return SecurityUtils.getUserId();
        } catch (Exception e) {
            return null;
        }
    }

    private Long currentUserIdOrZero() {
        Long userId = currentUserIdOrNull();
        return userId == null ? 0L : userId;
    }

    private void validateContactFields(String phone, String email, String nickName,
                                       String avatar, String deptName, String remark) {
        ValidateUtils.requirePhone(phone);
        ValidateUtils.requireEmail(email);
        ValidateUtils.requireMaxLength(nickName, 50, "昵称");
        ValidateUtils.requireMaxLength(avatar, 255, "头像地址");
        ValidateUtils.requireMaxLength(deptName, 100, "院系/部门");
        ValidateUtils.requireMaxLength(remark, 500, "备注");
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() == 0 ? null : trimmed;
    }
}
