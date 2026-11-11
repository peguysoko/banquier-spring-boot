package com.banquier.bank.controller;

import com.banquier.bank.dto.ReversalRequest;
import com.banquier.bank.dto.TransferRequest;
import com.banquier.bank.model.Transfer;
import com.banquier.bank.service.AuditService;
import com.banquier.bank.service.TransferService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transfers")
public class TransferController {

    private final TransferService transferService;
    private final AuditService auditService;        // le greffier

    public TransferController(TransferService transferService, AuditService auditService) {
        this.transferService = transferService;
        this.auditService = auditService;
    }

    // Faire un virement : /api/transfers
    @PostMapping
    public Transfer create(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody TransferRequest request) {

        String details = "de " + request.fromAccountId() + " vers " + request.toAccountId()
                + ", montant " + request.amount() + ", clé " + idempotencyKey;

        try {
            Transfer transfer = transferService.executeTransfer(
                    idempotencyKey, request.fromAccountId(), request.toAccountId(), request.amount());
            auditService.log("TRANSFER", "transfer", String.valueOf(transfer.getId()), "SUCCESS", details);
            return transfer;
        } catch (RuntimeException e) {
            auditService.log("TRANSFER", "transfer", null, "REJECTED", details + " | raison : " + e.getMessage());
            throw e;   // on relance l'erreur : le client reçoit quand même sa réponse (409, 400...)
        }
    }

    // Annuler un virement : /api/transfers/5/reverse (le {id} est lu grâce à @PathVariable)
    @PostMapping("/{id}/reverse")
    public Transfer reverse(@PathVariable Long id, @Valid @RequestBody ReversalRequest request) {

        String details = "annulation du virement " + id + " | motif : " + request.reason();

        try {
            Transfer reversal = transferService.reverseTransfer(id);
            auditService.log("REVERSAL", "transfer", String.valueOf(reversal.getId()), "SUCCESS", details);
            return reversal;
        } catch (RuntimeException e) {
            auditService.log("REVERSAL", "transfer", null, "REJECTED", details + " | raison : " + e.getMessage());
            throw e;
        }
    }
}