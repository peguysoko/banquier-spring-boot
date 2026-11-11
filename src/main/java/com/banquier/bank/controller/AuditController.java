package com.banquier.bank.controller;

import com.banquier.bank.model.AuditLog;
import com.banquier.bank.repository.AuditLogRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/audit")      // adresse du guichet de l'audit
public class AuditController {

    private final AuditLogRepository auditLogRepository;

    public AuditController(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    // Les 50 dernières actions, de la plus récente à la plus ancienne
    @GetMapping
    public List<AuditLog> latest() {
        return auditLogRepository.findTop50ByOrderByIdDesc();
    }
}