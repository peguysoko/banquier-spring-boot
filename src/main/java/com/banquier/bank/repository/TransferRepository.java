package com.banquier.bank.repository;

import com.banquier.bank.model.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TransferRepository extends JpaRepository<Transfer, Long> {

    // Spring lit le NOM de la méthode et écrit le SQL tout seul :
    // "find By IdempotencyKey" = cherche le virement qui porte cette clé.
    Optional<Transfer> findByIdempotencyKey(String idempotencyKey);
}