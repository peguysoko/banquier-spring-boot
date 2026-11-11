package com.banquier.bank.controller;

import com.banquier.bank.service.ReconciliationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping("/api/reconciliation")      // adresse du guichet de la réconciliation
public class ReconciliationController {

    private final ReconciliationService reconciliationService;

    public ReconciliationController(ReconciliationService reconciliationService) {
        this.reconciliationService = reconciliationService;
    }

    // On lance les 4 contrôles et on renvoie le rapport
    @GetMapping
    public Map<String, Object> reconcile() {
        return reconciliationService.run();
    }
}