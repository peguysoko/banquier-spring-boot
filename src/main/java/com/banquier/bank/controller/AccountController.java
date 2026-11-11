package com.banquier.bank.controller;

import com.banquier.bank.model.Account;
import com.banquier.bank.model.AppUser;
import com.banquier.bank.repository.AccountRepository;
import com.banquier.bank.repository.AppUserRepository;
import org.springframework.security.access.AccessDeniedException;   // l'erreur "accès interdit" (403)
import org.springframework.security.core.Authentication;            // "la carte d'identité de la personne connectée"
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;        // pour lire le numéro dans l'adresse
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountRepository accountRepository;
    private final AppUserRepository appUserRepository;   // pour retrouver qui est la personne connectée

    public AccountController(AccountRepository accountRepository, AppUserRepository appUserRepository) {
        this.accountRepository = accountRepository;
        this.appUserRepository = appUserRepository;
    }

    // Liste des comptes : /api/accounts
    // "Authentication authentication" : Spring nous donne automatiquement l'identité de la personne connectée.
    @GetMapping
    public List<Account> getAllAccounts(Authentication authentication) {
        if (isClient(authentication)) {
            // Un CLIENT ne reçoit QUE ses propres comptes
            return accountRepository.findByOwnerUserId(currentUser(authentication).getId());
        }
        // CAISSIER, AUDITEUR et ADMIN voient tous les comptes
        return accountRepository.findAll();
    }

    // Un seul compte : /api/accounts/1 (le {id} dans l'adresse est lu grâce à @PathVariable)
    @GetMapping("/{id}")
    public Account getAccount(@PathVariable Long id, Authentication authentication) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new AccessDeniedException("Accès interdit"));

        if (isClient(authentication)) {
            Long myId = currentUser(authentication).getId();
            // Si le compte n'appartient pas à ce client, on refuse.
            // account.getOwnerUserId() peut être vide (null), donc on compare avec myId.equals(...)
            if (!myId.equals(account.getOwnerUserId())) {
                throw new AccessDeniedException("Accès interdit");   // le client reçoit un 403
            }
        }
        return account;
    }

    // Méthode utilitaire : la personne connectée a-t-elle le rôle CLIENT ?
    // Dans Spring Security, un rôle s'écrit avec le préfixe "ROLE_" (ROLE_CLIENT).
    private boolean isClient(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CLIENT"));
    }

    // Méthode utilitaire : retrouve la fiche de la personne connectée dans la table users
    private AppUser currentUser(Authentication authentication) {
        // authentication.getName() = l'identifiant de connexion (ex: "client1")
        return appUserRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException("Utilisateur introuvable"));
    }
}