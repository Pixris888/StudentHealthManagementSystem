package com.health.management.common.util;

import com.health.management.common.exception.ServiceException;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 业务校验工具：统一抛出 ServiceException，由全局异常处理器返回 400/409 等业务码。
 */
public final class ValidateUtils {

    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z][A-Za-z0-9_]{3,19}$");
    private static final Pattern ROLE_CODE = Pattern.compile("^[A-Z][A-Z0-9_]{2,49}$");
    private static final Pattern PERMS = Pattern.compile("^[a-z][a-z0-9]*(:[a-z][a-z0-9]*)+$");
    private static final Pattern PHONE = Pattern.compile("^1[3-9]\\d{9}$");
    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern STUDENT_NO = Pattern.compile("^[A-Za-z0-9]{4,30}$");

    private ValidateUtils() {
    }

    public static boolean hasText(String value) {
        return value != null && value.trim().length() > 0;
    }

    public static void requireNotNull(Object value, String fieldName) {
        if (value == null) {
            throw ServiceException.badRequest(fieldName + "不能为空");
        }
    }

    public static void requireText(String value, String fieldName) {
        if (!hasText(value)) {
            throw ServiceException.badRequest(fieldName + "不能为空");
        }
    }

    public static void requireMaxLength(String value, int max, String fieldName) {
        if (value != null && value.length() > max) {
            throw ServiceException.badRequest(fieldName + "长度不能超过" + max + "个字符");
        }
    }

    public static void requireRange(Integer value, int min, int max, String fieldName) {
        if (value == null || value < min || value > max) {
            throw ServiceException.badRequest(fieldName + "必须在" + min + "-" + max + "之间");
        }
    }

    public static void requireUsername(String username) {
        requireText(username, "用户名");
        requireMaxLength(username, 50, "用户名");
        if (!USERNAME.matcher(username).matches()) {
            throw ServiceException.badRequest("用户名必须为4-20位，以字母开头，只能包含字母、数字、下划线");
        }
    }

    public static void requirePassword(String password) {
        requireText(password, "密码");
        if (password.length() < 8 || password.length() > 32) {
            throw ServiceException.badRequest("密码长度必须为8-32位");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw ServiceException.badRequest("密码过长，BCrypt最多支持72字节");
        }
        boolean lower = false;
        boolean upper = false;
        boolean digit = false;
        boolean special = false;
        for (int i = 0; i < password.length(); i++) {
            char c = password.charAt(i);
            if (Character.isWhitespace(c)) {
                throw ServiceException.badRequest("密码不能包含空白字符");
            }
            if (c >= 'a' && c <= 'z') {
                lower = true;
            } else if (c >= 'A' && c <= 'Z') {
                upper = true;
            } else if (c >= '0' && c <= '9') {
                digit = true;
            } else {
                special = true;
            }
        }
        int categories = (lower ? 1 : 0) + (upper ? 1 : 0) + (digit ? 1 : 0) + (special ? 1 : 0);
        if (categories < 3) {
            throw ServiceException.badRequest("密码必须包含大写字母、小写字母、数字、特殊字符中的至少三类");
        }
    }

    public static void requirePasswordNotContains(String password, String... values) {
        if (password == null) {
            return;
        }
        String lowerPwd = password.toLowerCase();
        for (String value : values) {
            if (hasText(value) && lowerPwd.contains(value.toLowerCase())) {
                throw ServiceException.badRequest("密码不能包含用户名或学号");
            }
        }
    }

    public static void requirePhone(String phone) {
        if (!hasText(phone)) {
            return;
        }
        if (phone.length() > 20 || !PHONE.matcher(phone).matches()) {
            throw ServiceException.badRequest("手机号格式不正确");
        }
    }

    public static void requireEmail(String email) {
        if (!hasText(email)) {
            return;
        }
        if (email.length() > 100 || !EMAIL.matcher(email).matches()) {
            throw ServiceException.badRequest("邮箱格式不正确");
        }
    }

    public static void requireStudentNo(String studentNo) {
        requireText(studentNo, "学号");
        if (!STUDENT_NO.matcher(studentNo).matches()) {
            throw ServiceException.badRequest("学号必须为4-30位字母或数字");
        }
    }

    public static void requireRoleCode(String roleCode) {
        requireText(roleCode, "角色编码");
        if (!ROLE_CODE.matcher(roleCode).matches()) {
            throw ServiceException.badRequest("角色编码必须为大写字母开头，只能包含大写字母、数字、下划线");
        }
    }

    public static void requirePerms(String perms) {
        requireText(perms, "权限标识");
        requireMaxLength(perms, 100, "权限标识");
        if (!PERMS.matcher(perms).matches()) {
            throw ServiceException.badRequest("权限标识格式应为 模块:实体:操作，例如 system:user:list");
        }
    }

    public static void requireStatus(Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw ServiceException.badRequest("状态只能是0（禁用/停用）或1（启用/正常）");
        }
    }

    public static void requireUserType(Integer userType) {
        requireRange(userType, 1, 3, "用户类型");
    }

    public static void requireMenuType(Integer menuType) {
        requireRange(menuType, 1, 3, "菜单类型");
    }

    public static void requireDataScope(Integer dataScope) {
        requireRange(dataScope, 1, 4, "数据权限范围");
    }

    public static void requireGender(Integer gender) {
        if (gender != null) {
            requireRange(gender, 0, 2, "性别");
        }
    }

    public static void requireIds(List<Long> ids, String fieldName) {
        if (ids == null || ids.isEmpty()) {
            throw ServiceException.badRequest(fieldName + "不能为空");
        }
        for (Long id : ids) {
            if (id == null || id <= 0) {
                throw ServiceException.badRequest(fieldName + "包含非法ID");
            }
        }
    }

    /**
     * 解析逗号分隔的ID，去重并保持顺序
     */
    public static List<Long> parseIds(String ids) {
        if (!hasText(ids)) {
            throw ServiceException.badRequest("ID不能为空");
        }
        String[] parts = ids.split(",");
        Set<Long> idSet = new LinkedHashSet<>();
        for (String part : parts) {
            String value = part.trim();
            if (value.length() == 0) {
                continue;
            }
            try {
                long id = Long.parseLong(value);
                if (id <= 0) {
                    throw ServiceException.badRequest("ID必须为正整数");
                }
                idSet.add(id);
            } catch (NumberFormatException e) {
                throw ServiceException.badRequest("ID格式错误：" + value);
            }
        }
        if (idSet.isEmpty()) {
            throw ServiceException.badRequest("ID不能为空");
        }
        if (idSet.size() > 1000) {
            throw ServiceException.badRequest("一次最多操作1000条记录");
        }
        return new ArrayList<>(idSet);
    }

    public static void requirePage(Integer page, Integer size) {
        if (page != null && page < 1) {
            throw ServiceException.badRequest("页码必须大于0");
        }
        if (size != null && (size < 1 || size > 100)) {
            throw ServiceException.badRequest("每页数量必须在1-100之间");
        }
    }
}
