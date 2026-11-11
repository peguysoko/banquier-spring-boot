package com.banquier.bank.service;

import com.banquier.bank.model.Account;
import com.banquier.bank.model.LedgerEntry;
import com.banquier.bank.model.Transfer;
import com.banquier.bank.repository.AccountRepository;
import com.banquier.bank.repository.LedgerEntryRepository;
import com.banquier.bank.repository.TransferRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.Optional;

// @Service = "cette classe contient les règles métier de la banque"
@Service
public class TransferService {

    // Les trois magasiniers dont on a besoin
    private final AccountRepository accountRepository;
    private final TransferRepository transferRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    // Spring nous donne les trois magasiniers automatiquement
    public TransferService(AccountRepository accountRepository,
                           TransferRepository transferRepository,
                           LedgerEntryRepository ledgerEntryRepository) {
        this.accountRepository = accountRepository;
        this.transferRepository = transferRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    // @Transactional = TOUT OU RIEN : si une erreur survient au milieu,
    // MySQL annule tout ce qui a été fait depuis le début de la méthode.
    @Transactional
    public Transfer executeTransfer(String key, Long fromId, Long toId, BigDecimal amount) {

        // ÉTAPE 1 : cette clé a-t-elle déjà été utilisée ?
        Optional<Transfer> existing = transferRepository.findByIdempotencyKey(key);
        if (existing.isPresent()) {                 // isPresent = "il y a bien quelque chose"
            Transfer old = existing.get();          // get = on sort le virement de la boîte Optional
            // Même clé mais demande DIFFÉRENTE = suspect, on refuse
            boolean sameRequest = old.getFromAccountId().equals(fromId)
                    && old.getToAccountId().equals(toId)
                    && old.getAmount().compareTo(amount) == 0;   // compareTo == 0 : montants égaux
            if (!sameRequest) {
                throw new IllegalStateException("Cette clé d'idempotence a déjà servi pour une autre demande");
            }
            return old;                              // même demande : on renvoie le reçu d'origine, SANS refaire le virement
        }

        // ÉTAPE 2 : règle de base
        if (fromId.equals(toId)) {
            throw new IllegalArgumentException("Le compte émetteur et le compte récepteur doivent être différents");
        }

        // ÉTAPE 3 : on verrouille les deux comptes.
        // Toujours le plus petit id en premier : si deux virements croisés (A→B et B→A)
        // arrivent en même temps, ils verrouillent dans le même ordre et ne se bloquent pas mutuellement.
        Long firstId = Math.min(fromId, toId);
        Long secondId = Math.max(fromId, toId);
        Account first = accountRepository.findByIdForUpdate(firstId)
                .orElseThrow(() -> new IllegalArgumentException("Compte introuvable : " + firstId));
        Account second = accountRepository.findByIdForUpdate(secondId)
                .orElseThrow(() -> new IllegalArgumentException("Compte introuvable : " + secondId));

        // On retrouve lequel est l'émetteur et lequel est le récepteur
        Account from = fromId.equals(firstId) ? first : second;   // condition ? si_vrai : si_faux
        Account to = fromId.equals(firstId) ? second : first;

        // ÉTAPE 4 : les règles de la banque
        if (!"ACTIVE".equals(from.getStatus()) || !"ACTIVE".equals(to.getStatus())) {
            throw new IllegalStateException("Les deux comptes doivent être actifs");
        }
        if (from.getBalance().compareTo(amount) < 0) {            // compareTo < 0 : solde plus petit que le montant
            throw new IllegalStateException("Solde insuffisant");
        }

        // ÉTAPE 5 : on bouge l'argent
        from.debit(amount);    // l'émetteur perd le montant
        to.credit(amount);     // le récepteur le reçoit

        // ÉTAPE 6 : on écrit le reçu du virement
        Transfer transfer = transferRepository.save(
                new Transfer(key, fromId, toId, amount, "COMPLETED"));

        // ÉTAPE 7 : on écrit DEUX lignes dans le journal (débit + crédit), avec le solde après le mouvement
        ledgerEntryRepository.save(new LedgerEntry(transfer.getId(), fromId, "DEBIT", amount, from.getBalance()));
        ledgerEntryRepository.save(new LedgerEntry(transfer.getId(), toId, "CREDIT", amount, to.getBalance()));

        return transfer;
        // À la fin de la méthode, Spring valide la transaction : les nouveaux soldes sont enregistrés dans MySQL.
    }
        // ANNULER un virement par une écriture inverse. Tout se passe dans UNE transaction : tout ou rien.
    @Transactional
    public Transfer reverseTransfer(Long originalId) {

        // On retrouve le virement à annuler
        Transfer original = transferRepository.findById(originalId)
                .orElseThrow(() -> new IllegalArgumentException("Virement introuvable : " + originalId));

        // Seul un virement terminé peut être annulé
        if (!"COMPLETED".equals(original.getStatus())) {
            throw new IllegalStateException("Seul un virement terminé peut être annulé");
        }
        // Un virement qui est déjà une annulation ne peut pas être annulé à son tour
        if (original.getReversesTransferId() != null) {
            throw new IllegalStateException("On n'annule pas une annulation");
        }

        // On réutilise executeTransfer, en INVERSANT les deux comptes.
        // La clé "REV-<numéro>" est toujours la même pour un virement donné :
        // demander deux fois l'annulation renvoie donc la même annulation (idempotence).
        Transfer reversal = executeTransfer(
                "REV-" + originalId,
                original.getToAccountId(),      // celui qui avait REÇU devient l'émetteur
                original.getFromAccountId(),    // celui qui avait ENVOYÉ devient le récepteur
                original.getAmount());

        // On relie l'annulation au virement annulé
        reversal.setReversesTransferId(originalId);
        return reversal;
    }
}