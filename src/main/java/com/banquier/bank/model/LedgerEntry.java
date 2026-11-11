package com.banquier.bank.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ledger_entries")   // reflet de la table ledger_entries
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transfer_id")
    private Long transferId;        // à quel virement appartient cette ligne

    @Column(name = "account_id")
    private Long accountId;         // quel compte est touché

    @Column(name = "entry_type")
    private String entryType;       // "DEBIT" (argent qui sort) ou "CREDIT" (argent qui entre)

    private BigDecimal amount;      // montant du mouvement

    @Column(name = "balance_after")
    private BigDecimal balanceAfter;// solde du compte juste après le mouvement

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected LedgerEntry() {}      // constructeur vide pour Spring

    public LedgerEntry(Long transferId, Long accountId, String entryType,
                       BigDecimal amount, BigDecimal balanceAfter) {
        this.transferId = transferId;
        this.accountId = accountId;
        this.entryType = entryType;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
    }

    public Long getId() { return id; }
    public Long getTransferId() { return transferId; }
    public Long getAccountId() { return accountId; }
    public String getEntryType() { return entryType; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}