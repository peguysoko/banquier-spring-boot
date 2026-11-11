package com.banquier.bank.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_log")      // reflet de la table audit_log
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // La date est remplie par MySQL, on ne l'écrit pas depuis Java
    @Column(name = "occurred_at", insertable = false, updatable = false)
    private LocalDateTime occurredAt;

    private String username;                  // qui

    private String action;                    // quoi

    @Column(name = "entity_type")
    private String entityType;                // sur quel type d'objet

    @Column(name = "entity_id")
    private String entityId;                  // quel objet (peut être vide)

    private String outcome;                   // SUCCESS ou REJECTED

    private String details;                   // les détails

    protected AuditLog() {}                   // constructeur vide pour Spring

    public AuditLog(String username, String action, String entityType,
                    String entityId, String outcome, String details) {
        this.username = username;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.outcome = outcome;
        this.details = details;
    }

    public Long getId() { return id; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
    public String getUsername() { return username; }
    public String getAction() { return action; }
    public String getEntityType() { return entityType; }
    public String getEntityId() { return entityId; }
    public String getOutcome() { return outcome; }
    public String getDetails() { return details; }
}