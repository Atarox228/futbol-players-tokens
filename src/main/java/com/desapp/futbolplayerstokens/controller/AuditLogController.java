package com.desapp.futbolplayerstokens.controller;

import com.desapp.futbolplayerstokens.modelo.AuditLog;
import com.desapp.futbolplayerstokens.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;

    public AuditLogController(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping
    public Page<AuditLog> getAuditLogs(
            @PageableDefault(size = 50, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {
        return auditLogRepository.findAllByOrderByTimestampDesc(pageable);
    }
}
