package com.desapp.futbolplayerstokens.controller;

import com.desapp.futbolplayerstokens.modelo.AuditLog;
import com.desapp.futbolplayerstokens.repository.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogControllerTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLogController auditLogController;

    @Test
    void getAuditLogs_returnsPage() {
        PageRequest pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "timestamp"));
        AuditLog log = AuditLog.builder().username("user1").operation("PlayerController.search").build();
        Page<AuditLog> page = new PageImpl<>(List.of(log));

        when(auditLogRepository.findAllByOrderByTimestampDesc(pageable)).thenReturn(page);

        Page<AuditLog> result = auditLogController.getAuditLogs(pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("user1", result.getContent().getFirst().getUsername());
    }

    @Test
    void getAuditLogs_emptyPage() {
        PageRequest pageable = PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "timestamp"));

        when(auditLogRepository.findAllByOrderByTimestampDesc(pageable)).thenReturn(Page.empty());

        Page<AuditLog> result = auditLogController.getAuditLogs(pageable);

        assertTrue(result.isEmpty());
    }

    @Test
    void getAuditLogs_defaultSorting() {
        PageRequest pageable = PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "timestamp"));

        auditLogController.getAuditLogs(pageable);

        verify(auditLogRepository).findAllByOrderByTimestampDesc(pageable);
    }
}
