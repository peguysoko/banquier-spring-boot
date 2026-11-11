package com.banquier.bank.repository;

import com.banquier.bank.model.Account;
import jakarta.persistence.LockModeType;                         // les types de verrous
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;             // pour poser un verrou
import org.springframework.data.jpa.repository.Query;            // pour écrire notre propre requête
import org.springframework.data.repository.query.Param;          // pour nommer un paramètre
import java.util.List;                                           // NOUVEAU : "List" = une liste d'éléments
import java.util.Optional;                                       // "peut-être une valeur, peut-être rien"

public interface AccountRepository extends JpaRepository<Account, Long> {

    // Verrou : tant que ce virement n'est pas fini, personne d'autre ne peut modifier ce compte
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.id = :id")
    Optional<Account> findByIdForUpdate(@Param("id") Long id);

    // Spring lit le NOM de la méthode : "trouve tous les comptes dont ownerUserId vaut cette valeur"
    List<Account> findByOwnerUserId(Long ownerUserId);
}