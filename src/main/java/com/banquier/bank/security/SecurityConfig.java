package com.banquier.bank.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    // L'outil qui HACHE les mots de passe avec BCrypt (empreinte à sens unique)
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Le règlement : qui a le droit d'entrer où
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Notre API n'utilise pas de cookies de session : on désactive la protection CSRF
            .csrf(csrf -> csrf.disable())
            // STATELESS = le vigile ne retient personne : on se présente à CHAQUE demande
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // ANNULER un virement : seulement l'ADMIN.
                // Le "*" remplace UN morceau de l'adresse (le numéro du virement).
                .requestMatchers(HttpMethod.POST, "/api/transfers/*/reverse").hasRole("ADMIN")
                // FAIRE UN VIREMENT : seulement CAISSIER ou ADMIN
                .requestMatchers(HttpMethod.POST, "/api/transfers").hasAnyRole("CAISSIER", "ADMIN")
                // LIRE L'AUDIT : seulement AUDITEUR ou ADMIN
                .requestMatchers(HttpMethod.GET, "/api/audit/**").hasAnyRole("AUDITEUR", "ADMIN")
                // LANCER LA RÉCONCILIATION : seulement AUDITEUR ou ADMIN
                .requestMatchers(HttpMethod.GET, "/api/reconciliation/**").hasAnyRole("AUDITEUR", "ADMIN")
                // CONSULTER les comptes : tous les rôles (le contrôleur filtre pour les CLIENT)
                .requestMatchers(HttpMethod.GET, "/api/accounts/**").hasAnyRole("CLIENT", "CAISSIER", "AUDITEUR", "ADMIN")
                // La page d'erreur interne de Spring doit rester accessible pour afficher les refus (401, 403...)
                .requestMatchers("/error").permitAll()
                // NOUVEAU : tout le reste est REFUSÉ. Une règle oubliée ne laisse plus passer personne.
                .anyRequest().denyAll())
            // httpBasic = identifiant + mot de passe envoyés avec chaque demande
            .httpBasic(Customizer.withDefaults());
        return http.build();
    }
}