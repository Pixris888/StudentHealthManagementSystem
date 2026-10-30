package com.health.management.audit;

import com.health.management.entity.SysOperLog;
import com.health.management.security.LoginUser;
import com.health.management.security.SecurityUtils;
import com.health.management.service.SysOperLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

/**
 * 操作审计记录器。
 * 使用独立事务（REQUIRES_NEW），审计失败不影响主业务，也不污染主事务。
 */
@Slf4j
@Component
public class AuditLogRecorder {

    public static final int TYPE_OTHER = 0;
    public static final int TYPE_ADD = 1;
    public static final int TYPE_UPDATE = 2;
    public static final int TYPE_DELETE = 3;
    public static final int TYPE_GRANT = 4;
    public static final int TYPE_EXPORT = 5;
    public static final int TYPE_IMPORT = 6;
    public static final int TYPE_FORCE_LOGOUT = 7;
    public static final int TYPE_RESET_PWD = 8;

    @Autowired
    private SysOperLogService operLogService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    public void record(String title, int businessType, String content, boolean success, String errorMsg) {
        LoginUser loginUser = currentLoginUser();
        record(title, businessType,
                loginUser == null ? 0L : loginUser.getUserId(),
                loginUser == null ? "" : loginUser.getUsername(),
                content, success, errorMsg);
    }

    public void record(String title, int businessType, Long userId, String username,
                       String content, boolean success, String errorMsg) {
        try {
            SysOperLog operLog = new SysOperLog();
            operLog.setTitle(truncate(title, 50));
            operLog.setBusinessType(businessType);
            operLog.setMethod(truncate(title, 100));
            operLog.setRequestMethod(currentRequestMethod());
            operLog.setOperatorType(1);
            operLog.setOperUserId(userId == null ? 0L : userId);
            operLog.setOperUserName(truncate(username, 50));
            operLog.setOperIp(truncate(getClientIp(), 50));
            operLog.setOperParam(truncate(content, 2000));
            operLog.setStatus(success ? 0 : 1);
            operLog.setErrorMsg(truncate(errorMsg, 2000));
            operLog.setOperTime(LocalDateTime.now());

            TransactionTemplate template = new TransactionTemplate(transactionManager);
            template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            template.execute(status -> {
                operLogService.save(operLog);
                return null;
            });
        } catch (Exception e) {
            log.warn("写审计日志失败: {}", e.getMessage(), e);
        }
    }

    public String getClientIp() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return "";
        }
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && ip.length() > 0 && !"unknown".equalsIgnoreCase(ip)) {
            int comma = ip.indexOf(',');
            return comma > 0 ? ip.substring(0, comma).trim() : ip.trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && ip.length() > 0 && !"unknown".equalsIgnoreCase(ip)) {
            return ip.trim();
        }
        return request.getRemoteAddr();
    }

    private String currentRequestMethod() {
        HttpServletRequest request = currentRequest();
        return request == null ? "" : request.getMethod();
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes) {
            return ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        }
        return null;
    }

    private LoginUser currentLoginUser() {
        try {
            return SecurityUtils.getLoginUser();
        } catch (Exception e) {
            return null;
        }
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
