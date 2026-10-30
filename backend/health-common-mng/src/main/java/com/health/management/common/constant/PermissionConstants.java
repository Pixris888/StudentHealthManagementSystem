package com.health.management.common.constant;

/**
 * 权限标识常量（与 sys_menu.perms 保持一致）
 */
public final class PermissionConstants {

    // 用户管理
    public static final String SYSTEM_USER_LIST = "system:user:list";
    public static final String SYSTEM_USER_ADD = "system:user:add";
    public static final String SYSTEM_USER_EDIT = "system:user:edit";
    public static final String SYSTEM_USER_REMOVE = "system:user:remove";
    public static final String SYSTEM_USER_RESET_PWD = "system:user:resetpwd";
    public static final String SYSTEM_USER_STATUS = "system:user:status";
    public static final String SYSTEM_USER_ASSIGN_ROLE = "system:user:assignRole";

    // 角色管理
    public static final String SYSTEM_ROLE_LIST = "system:role:list";
    public static final String SYSTEM_ROLE_ADD = "system:role:add";
    public static final String SYSTEM_ROLE_EDIT = "system:role:edit";
    public static final String SYSTEM_ROLE_REMOVE = "system:role:remove";
    public static final String SYSTEM_ROLE_AUTH = "system:role:auth";

    // 菜单管理
    public static final String SYSTEM_MENU_LIST = "system:menu:list";
    public static final String SYSTEM_MENU_ADD = "system:menu:add";
    public static final String SYSTEM_MENU_EDIT = "system:menu:edit";
    public static final String SYSTEM_MENU_REMOVE = "system:menu:remove";

    // 业务模块（后续开发时使用）
    public static final String HEALTH_PROFILE_LIST = "health:profile:list";
    public static final String HEALTH_PHYSICAL_LIST = "health:physical:list";
    public static final String HEALTH_CHECKIN_LIST = "health:checkin:list";
    public static final String HEALTH_CHECKIN_ADD = "health:checkin:add";
    public static final String HEALTH_ARTICLE_LIST = "health:article:list";
    public static final String HEALTH_ARTICLE_ADD = "health:article:add";
    public static final String HEALTH_ARTICLE_EDIT = "health:article:edit";
    public static final String HEALTH_ARTICLE_REMOVE = "health:article:remove";
    public static final String HEALTH_ARTICLE_AUDIT = "health:article:audit";
    public static final String MESSAGE_LIST = "message:list";
    public static final String MESSAGE_SEND = "message:send";
    public static final String REPORT_LIST = "report:list";
    public static final String REPORT_EXPORT = "report:export";
    public static final String AI_CONSULT_LIST = "ai:consult:list";

    private PermissionConstants() {
    }
}
