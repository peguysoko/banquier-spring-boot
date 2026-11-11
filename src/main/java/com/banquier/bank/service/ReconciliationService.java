package com.banquier.bank.service;

import org.springframework.jdbc.core.JdbcTemplate;   // outil qui envoie du SQL directement à MySQL
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.LinkedHashMap;                      // une "map" qui garde l'ordre d'ajout
import java.util.List;
import java.util.Map;                                // une map = un ensemble de paires "nom -> valeur"

@Service
public class ReconciliationService {

    private final JdbcTemplate jdbc;

    public ReconciliationService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Map<String, Object> run() {

        // CONTRÔLE 1 : le solde de chaque compte = le solde après la DERNIÈRE écriture du journal.
        List<Map<String, Object>> balanceGaps = jdbc.queryForList("""
                SELECT a.id AS account_id, a.account_number,
                       a.balance AS account_balance, l.balance_after AS ledger_balance
                FROM accounts a
                JOIN ledger_entries l ON l.account_id = a.id
                WHERE l.id = (SELECT MAX(l2.id) FROM ledger_entries l2 WHERE l2.account_id = a.id)
                  AND l.balance_after <> a.balance
                """);

        // CONTRÔLE 2 : chaque virement COMPLETED a exactement 1 débit et 1 crédit, du bon montant.
        List<Map<String, Object>> incompleteTransfers = jdbc.queryForList("""
                SELECT t.id AS transfer_id, t.idempotency_key, t.amount,
                       SUM(CASE WHEN l.entry_type = 'DEBIT'  THEN 1 ELSE 0 END) AS debit_lines,
                       SUM(CASE WHEN l.entry_type = 'CREDIT' THEN 1 ELSE 0 END) AS credit_lines
                FROM transfers t
                LEFT JOIN ledger_entries l ON l.transfer_id = t.id
                WHERE t.status = 'COMPLETED'
                GROUP BY t.id, t.idempotency_key, t.amount
                HAVING debit_lines <> 1
                    OR credit_lines <> 1
                    OR SUM(CASE WHEN l.entry_type = 'DEBIT'  THEN l.amount ELSE 0 END) <> t.amount
                    OR SUM(CASE WHEN l.entry_type = 'CREDIT' THEN l.amount ELSE 0 END) <> t.amount
                """);

        // CONTRÔLE 3 : total des débits = total des crédits.
        Map<String, Object> totals = jdbc.queryForMap("""
                SELECT COALESCE(SUM(CASE WHEN entry_type = 'DEBIT'  THEN amount END), 0) AS total_debits,
                       COALESCE(SUM(CASE WHEN entry_type = 'CREDIT' THEN amount END), 0) AS total_credits
                FROM ledger_entries
                """);
        BigDecimal debits = new BigDecimal(totals.get("total_debits").toString());
        BigDecimal credits = new BigDecimal(totals.get("total_credits").toString());
        boolean totalsMatch = debits.compareTo(credits) == 0;

        // CONTRÔLE 4 : chaque virement COMPLETED a une ligne d'audit SUCCESS.
        List<Map<String, Object>> transfersWithoutAudit = jdbc.queryForList("""
                SELECT t.id AS transfer_id, t.idempotency_key, t.created_at
                FROM transfers t
                WHERE t.status = 'COMPLETED'
                  AND NOT EXISTS (
                      SELECT 1 FROM audit_log a
                      WHERE a.action IN ('TRANSFER', 'REVERSAL') AND a.outcome = 'SUCCESS' AND a.entity_id = t.id)
                """);

        // CONTRÔLE 5 (NOUVEAU, une alerte) : doublons suspects.
        // On compare chaque virement t1 avec chaque virement t2 (une "auto-jointure") :
        // mêmes comptes, même montant, t2 plus récent, CLÉ DIFFÉRENTE, et moins de 10 minutes d'écart.
        List<Map<String, Object>> suspectedDuplicates = jdbc.queryForList("""
                SELECT t1.id AS transfer_id_1, t2.id AS transfer_id_2,
                       t1.from_account_id, t1.to_account_id, t1.amount,
                       t1.idempotency_key AS key_1, t2.idempotency_key AS key_2,
                       TIMESTAMPDIFF(SECOND, t1.created_at, t2.created_at) AS seconds_apart
                FROM transfers t1
                JOIN transfers t2
                  ON  t2.from_account_id = t1.from_account_id
                  AND t2.to_account_id   = t1.to_account_id
                  AND t2.amount          = t1.amount
                  AND t2.id              > t1.id
                  AND t2.idempotency_key <> t1.idempotency_key
                  AND t2.created_at <= DATE_ADD(t1.created_at, INTERVAL 10 MINUTE)
                WHERE t1.status = 'COMPLETED' AND t2.status = 'COMPLETED'
                """);

        // Contrôles 1 à 3 = l'argent. Contrôle 4 = la traçabilité. Contrôle 5 = une alerte, pas une erreur.
        boolean moneyOk = balanceGaps.isEmpty() && incompleteTransfers.isEmpty() && totalsMatch;

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("statut_argent", moneyOk ? "COHERENT" : "ANOMALIES");
        report.put("controle1_soldes_contre_journal", Map.of("ok", balanceGaps.isEmpty(), "ecarts", balanceGaps));
        report.put("controle2_virements_complets", Map.of("ok", incompleteTransfers.isEmpty(), "anomalies", incompleteTransfers));
        report.put("controle3_debits_egal_credits", Map.of("ok", totalsMatch, "total_debits", debits, "total_credits", credits));
        report.put("controle4_tracabilite", Map.of("ok", transfersWithoutAudit.isEmpty(), "virements_sans_audit", transfersWithoutAudit));
        report.put("controle5_doublons_suspects", Map.of("a_verifier", !suspectedDuplicates.isEmpty(), "paires", suspectedDuplicates));
        return report;
    }
}