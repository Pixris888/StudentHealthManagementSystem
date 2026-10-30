package com.health.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.health.management.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 用户名唯一性检查：包含逻辑删除记录，避免数据库唯一索引报错但提示不友好
     */
    int countByUsernameIncludingDeleted(@Param("username") String username);

    int countByStudentNoIncludingDeleted(@Param("studentNo") String studentNo);

    int countByStudentNoExcludingId(@Param("studentNo") String studentNo, @Param("id") Long id);

    /**
     * JWT 过滤器每请求只查这几个字段，避免加载密码
     */
    SysUser selectAuthStateById(@Param("id") Long id);

    /**
     * 事务内锁定用户行，用于最后管理员/自我保护校验
     */
    SysUser selectByIdForUpdate(@Param("id") Long id);

    int incrementTokenVersionById(@Param("id") Long id);
}
