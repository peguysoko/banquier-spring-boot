package com.banquier.bank.repository;

import com.banquier.bank.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    // Spring lit le nom : "trouve les 50 premières lignes, triées par id du plus récent au plus ancien"
    List<AuditLog> findTop50ByOrderByIdDesc();
}