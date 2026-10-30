package com.health.management.service.system;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.health.management.dto.RegisterDTO;
import com.health.management.dto.UserCreateDTO;
import com.health.management.dto.UserQueryDTO;
import com.health.management.dto.UserUpdateDTO;
import com.health.management.entity.SysUser;
import com.health.management.vo.UserVO;

import java.util.List;

/**
 * 系统用户服务：账号新增、修改、删除、启停、重置密码、角色分配。
 */
public interface SysUserService extends IService<SysUser> {

    IPage<UserVO> pageUsers(UserQueryDTO query);

    UserVO getUserDetail(Long userId);

    Long createUser(UserCreateDTO dto);

    /**
     * 学生自主注册：只创建启用状态的学生账号，强制绑定内置学生角色
     */
    Long registerStudent(RegisterDTO dto);

    void updateUser(UserUpdateDTO dto);

    void deleteUsers(String ids);

    void resetPassword(Long userId, String newPassword);

    void updateStatus(Long userId, Integer status);

    void assignRoles(Long userId, List<Long> roleIds);

    List<Long> getRoleIdsByUserId(Long userId);

    void changePassword(Long userId, String oldPassword, String newPassword);

    void updateLoginInfo(Long userId, String loginIp);
}
