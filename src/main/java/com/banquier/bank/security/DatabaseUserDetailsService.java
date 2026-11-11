package com.banquier.bank.security;

import com.banquier.bank.model.AppUser;
import com.banquier.bank.repository.AppUserRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

// UserDetailsService = "le carnet d'adresses du vigile".
// Quand quelqu'un se présente avec un identifiant, le vigile appelle loadUserByUsername pour le chercher.
@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private final AppUserRepository appUserRepository;

    public DatabaseUserDetailsService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Override   // @Override = "je remplace la méthode prévue par Spring Security"
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // On cherche dans MySQL ; si la personne n'existe pas, on lance une erreur
        AppUser user = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur inconnu : " + username));

        // On fabrique la "fiche" que comprend Spring Security :
        // identifiant, mot de passe haché, rôle, et compte actif ou non.
        return User.withUsername(user.getUsername())
                .password(user.getPasswordHash())
                .roles(user.getRole())              // ex: "CAISSIER" devient le badge ROLE_CAISSIER
                .disabled(!user.isEnabled())        // "!" = le contraire : compte bloqué si enabled vaut false
                .build();
    }
}