package com.desapp.futbolplayerstokens.config;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.UUID;

import static org.slf4j.MDC.put;
import static org.slf4j.MDC.remove;

/**
 * Aspect para auditar todos los servicios REST publicados.
 * 
 * Loguea:
 * - Timestamp ISO-8601
 * - Usuario (desde SecurityContextHolder o "anonymous")
 * - Operación (HTTP method + path)
 * - Método Java ejecutado
 * - Parámetros sanitizados
 * - Tiempo de ejecución en ms
 */
@Slf4j
@Aspect
@Component
public class LoggingAspect {

    private static final String REQUEST_ID_HEADER = "X-Request-ID";
    private static final String MDC_REQUEST_ID = "requestId";
    private static final String MDC_USER = "user";
    private static final String MDC_OPERATION = "operation";
    private static final String[] SENSITIVE_KEYS = {"password", "secret", "token", "authorization", "apikey", "api_key"};
    private static final int MAX_PARAMETER_LENGTH = 500;
    private static final int MAX_BODY_LENGTH = 1000;

    /**
     * Intercepta todos los métodos públicos de controllers.
     * Patrón: com.desapp.futbolplayerstokens.controller.*
     */
    @Around("execution(public * com.desapp.futbolplayerstokens.controller..*(..))")
    public Object logAroundControllerMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        String requestId = UUID.randomUUID().toString();
        put(MDC_REQUEST_ID, requestId);
        
        try {
            // Obtener información de la request
            HttpServletRequest request = getHttpServletRequest();
            String httpMethod = request != null ? request.getMethod() : "UNKNOWN";
            String requestUri = request != null ? request.getRequestURI() : "UNKNOWN";
            
            // Obtener usuario
            String username = getUsername();
            put(MDC_USER, username);
            
            // Operación
            String operation = httpMethod + " " + requestUri;
            put(MDC_OPERATION, operation);
            
            // Método Java
            String methodName = joinPoint.getSignature().getName();
            String className = joinPoint.getTarget().getClass().getSimpleName();
            
            // Parámetros sanitizados
            String sanitizedArgs = sanitizeArguments(joinPoint.getArgs());
            
            // Log de inicio
            log.info("=== AUDIT START === | Operation: {} | Method: {}.{} | Parameters: {}",
                    operation, className, methodName, sanitizedArgs);
            
            // Ejecutar el método
            long startTime = System.currentTimeMillis();
            Object result = null;
            try {
                result = joinPoint.proceed();
                long executionTime = System.currentTimeMillis() - startTime;
                log.info("=== AUDIT SUCCESS === | Operation: {} | Execution Time: {}ms",
                        operation, executionTime);
                return result;
            } catch (Exception e) {
                long executionTime = System.currentTimeMillis() - startTime;
                log.error("=== AUDIT ERROR === | Operation: {} | Exception: {} | Execution Time: {}ms",
                        operation, e.getClass().getSimpleName(), executionTime, e);
                throw e;
            }
            
        } finally {
            // Limpiar MDC
            remove(MDC_REQUEST_ID);
            remove(MDC_USER);
            remove(MDC_OPERATION);
        }
    }

    /**
     * Obtiene el HttpServletRequest de la request actual.
     */
    private HttpServletRequest getHttpServletRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                return attributes.getRequest();
            }
        } catch (Exception e) {
            log.debug("No HttpServletRequest available in current context");
        }
        return null;
    }

    /**
     * Obtiene el nombre del usuario desde SecurityContextHolder o retorna "anonymous".
     */
    private String getUsername() {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            return username != null ? username : "anonymous";
        } catch (Exception e) {
            return "anonymous";
        }
    }

    /**
     * Sanitiza los argumentos de la request:
     * - Oculta valores de propiedades sensibles
     * - Trunca parámetros que excedan MAX_PARAMETER_LENGTH
     * - No loguea body de POST/PUT completos si exceden MAX_BODY_LENGTH
     */
    private String sanitizeArguments(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }

        StringBuilder result = new StringBuilder("[");
        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                result.append(", ");
            }
            Object arg = args[i];
            if (arg != null) {
                String argStr = sanitizeValue(arg);
                if (argStr.length() > MAX_PARAMETER_LENGTH) {
                    argStr = argStr.substring(0, MAX_PARAMETER_LENGTH) + "...[truncated]";
                }
                result.append(argStr);
            } else {
                result.append("null");
            }
        }
        result.append("]");
        return result.toString();
    }

    /**
     * Sanitiza un valor individual, reemplazando contenido sensible.
     */
    private String sanitizeValue(Object value) {
        if (value == null) {
            return "null";
        }

        String strValue = value.toString();
        
        // Si es una cadena larga (posible body), truncar
        if (strValue.length() > MAX_BODY_LENGTH) {
            strValue = strValue.substring(0, MAX_BODY_LENGTH) + "...[truncated]";
        }

        // Reemplazar valores sensibles
        for (String sensitiveKey : SENSITIVE_KEYS) {
            String pattern = "(?i)\"" + sensitiveKey + "\"\\s*:\\s*\"[^\"]*\"";
            strValue = strValue.replaceAll(pattern, "\"" + sensitiveKey + "\": \"***REDACTED***\"");
            
            pattern = "(?i)" + sensitiveKey + "\\s*=\\s*[^,}\\s]*";
            strValue = strValue.replaceAll(pattern, sensitiveKey + "=***REDACTED***");
        }

        return strValue;
    }
}
