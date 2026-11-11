package com.banquier.bank.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transfers")   // reflet de la table transfers
public class Transfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idempotency_key")
    private String idempotencyKey;      // la clé unique de la demande

    @Column(name = "from_account_id")
    private Long fromAccountId;         // numéro interne du compte qui envoie

    @Column(name = "to_account_id")
    private Long toAccountId;           // numéro interne du compte qui reçoit

    private BigDecimal amount;          // montant

    private String status;              // PENDING, COMPLETED ou FAILED
    // Si ce virement est une ANNULATION : le numéro du virement annulé. Sinon : vide.
    @Column(name = "reverses_transfer_id")
    private Long reversesTransferId;
    // insertable = false, updatable = false : on ne l'écrit pas depuis Java,
    // c'est MySQL qui remplit la date tout seul (CURRENT_TIMESTAMP)
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    // Constructeur VIDE : obligatoire, c'est lui que Spring utilise pour fabriquer un Transfer
    // quand il lit une ligne de la base.
    protected Transfer() {}

    // Constructeur PRATIQUE : c'est celui qu'on utilise pour créer un nouveau virement
    public Transfer(String idempotencyKey, Long fromAccountId, Long toAccountId,
                    BigDecimal amount, String status) {
        this.idempotencyKey = idempotencyKey;   // "this.x = x" : on range la valeur reçue dans le champ
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.amount = amount;
        this.status = status;
    }

    // Getters : pour lire chaque champ (et pour que Spring puisse les envoyer en JSON)
    public Long getId() { return id; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public Long getFromAccountId() { return fromAccountId; }
    public Long getToAccountId() { return toAccountId; }
    public BigDecimal getAmount() { return amount; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
        // Pour LIRE le numéro du virement annulé
    public Long getReversesTransferId() { return reversesTransferId; }

    // Pour ÉCRIRE ce numéro (utilisé par le service quand il crée une annulation)
    public void setReversesTransferId(Long reversesTransferId) { this.reversesTransferId = reversesTransferId; }
}