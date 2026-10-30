package com.health.management.common.constant;

/**
 * 系统内置角色与用户类型常量
 */
public final class RoleConstants {

    public static final String STUDENT = "STUDENT";
    public static final String DOCTOR_ADMIN = "DOCTOR_ADMIN";
    public static final String SUPER_ADMIN = "SUPER_ADMIN";

    public static final int USER_TYPE_STUDENT = 1;
    public static final int USER_TYPE_DOCTOR_ADMIN = 2;
    public static final int USER_TYPE_SUPER_ADMIN = 3;

    private RoleConstants() {
    }

    public static boolean isBuiltIn(String roleCode) {
        return STUDENT.equals(roleCode) || DOCTOR_ADMIN.equals(roleCode) || SUPER_ADMIN.equals(roleCode);
    }

    /**
     * 内置角色编码对应的用户类型；自定义角色返回 null
     */
    public static Integer userTypeOf(String roleCode) {
        if (STUDENT.equals(roleCode)) {
            return USER_TYPE_STUDENT;
        }
        if (DOCTOR_ADMIN.equals(roleCode)) {
            return USER_TYPE_DOCTOR_ADMIN;
        }
        if (SUPER_ADMIN.equals(roleCode)) {
            return USER_TYPE_SUPER_ADMIN;
        }
        return null;
    }

    public static String userTypeName(Integer userType) {
        if (userType == null) {
            return "未知";
        }
        switch (userType) {
            case USER_TYPE_STUDENT:
                return "学生";
            case USER_TYPE_DOCTOR_ADMIN:
                return "校医科普管理员";
            case USER_TYPE_SUPER_ADMIN:
                return "超级系统管理员";
            default:
                return "未知";
        }
    }
}
