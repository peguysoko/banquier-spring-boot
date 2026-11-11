package com.banquier.bank.repository;

import com.banquier.bank.model.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;

// Vide : on a seulement besoin d'ÉCRIRE dans le journal (save), pas de recherche spéciale
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {
}