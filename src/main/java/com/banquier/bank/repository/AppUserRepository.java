package com.banquier.bank.repository;

import com.banquier.bank.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    // "find By Username" : Spring écrit tout seul la recherche d'un utilisateur par son identifiant
    Optional<AppUser> findByUsername(String username);
}