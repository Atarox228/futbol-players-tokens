package com.desapp.futbolplayerstokens.repository;

import com.desapp.futbolplayerstokens.modelo.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findAllByOrderByTimestampDesc(Pageable pageable);
}
