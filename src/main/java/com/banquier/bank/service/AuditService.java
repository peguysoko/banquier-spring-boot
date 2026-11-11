package com.banquier.bank.service;

import com.banquier.bank.model.AuditLog;
import com.banquier.bank.repository.AuditLogRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    // REQUIRES_NEW = "ouvre une transaction toute neuve, indépendante des autres".
    // Pourquoi ? Si un virement est refusé, sa transaction est annulée (rollback).
    // Sans cela, la ligne d'audit du refus serait annulée avec lui, et on perdrait la trace de la tentative.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String action, String entityType, String entityId, String outcome, String details) {
        // SecurityContextHolder = "le registre de qui est connecté en ce moment"
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // Si personne n'est identifié, on écrit "anonymous"
        String username = (auth != null) ? auth.getName() : "anonymous";

        auditLogRepository.save(new AuditLog(username, action, entityType, entityId, outcome, details));
    }
}