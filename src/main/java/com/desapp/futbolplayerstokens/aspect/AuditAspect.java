package com.desapp.futbolplayerstokens.aspect;

import com.desapp.futbolplayerstokens.modelo.AuditLog;
import com.desapp.futbolplayerstokens.repository.AuditLogRepository;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.stream.Collectors;

@Aspect
@Component
public class AuditAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);
    private static final int MAX_PARAM_LENGTH = 500;

    private final AuditLogRepository auditLogRepository;

    public AuditAspect(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Around("execution(public * com.desapp.futbolplayerstokens.controller..*(..)) && !execution(* com.desapp.futbolplayerstokens.controller.AuditLogController.*(..))")
    public Object audit(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();

        Object result = joinPoint.proceed();

        long executionTime = System.currentTimeMillis() - start;
        LocalDateTime now = LocalDateTime.now(ZoneId.of("America/Argentina/Buenos_Aires"));

        String username = resolveUsername();
        String operation = joinPoint.getSignature().getDeclaringType().getSimpleName() + "." + joinPoint.getSignature().getName();
        String parameters = truncate(formatParams(joinPoint.getArgs()), MAX_PARAM_LENGTH);

        log.info("[AUDIT] user={}, operation={}, params={}, executionTime={}ms", username, operation, parameters, executionTime);

        try {
            AuditLog auditLog = AuditLog.builder()
                    .timestamp(now)
                    .username(username)
                    .operation(operation)
                    .parameters(parameters)
                    .executionTimeMs(executionTime)
                    .build();
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.warn("Failed to persist audit log: {}", e.getMessage());
        }

        return result;
    }

    private String resolveUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return "anonymous";
        }
        return auth.getName();
    }

    private String formatParams(Object[] args) {
        if (args == null || args.length == 0) return "";
        return Arrays.stream(args)
                .map(a -> {
                    if (a == null) return "null";
                    String s = a.toString();
                    return s.length() > 200 ? s.substring(0, 200) + "..." : s;
                })
                .collect(Collectors.joining(", "));
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
