package com.banquier.bank.security;

import com.banquier.bank.model.AppUser;
import com.banquier.bank.repository.AppUserRepository;
import org.slf4j.Logger;                                          // pour écrire des messages dans la console
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;        // pour lire une valeur de application.properties
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

// CommandLineRunner = "exécute la méthode run() UNE FOIS, juste après le démarrage"
@Component
public class DataInitializer implements CommandLineRunner {

    // Le "journal de bord" de cette classe
    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final String demoPassword;           // le mot de passe commun de démonstration

    // @Value("${demo.password:}") = "lis la valeur demo.password dans application.properties"
    public DataInitializer(AppUserRepository appUserRepository,
                           PasswordEncoder passwordEncoder,
                           @Value("${demo.password:}") String demoPassword) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.demoPassword = demoPassword;
    }

    @Override
    public void run(String... args) {
        // isBlank() = "le texte est vide". Sans mot de passe fourni, on ne crée personne.
        if (demoPassword.isBlank()) {
            log.warn("DEMO_PASSWORD absent : aucun utilisateur de démonstration créé.");
            return;                              // return = on s'arrête ici
        }
        // Les 5 utilisateurs de démonstration partagent ce mot de passe (usage local uniquement)
        createIfMissing("admin", "ADMIN");
        createIfMissing("caissier1", "CAISSIER");
        createIfMissing("auditeur1", "AUDITEUR");
        createIfMissing("client1", "CLIENT");
        createIfMissing("client2", "CLIENT");
    }

    // On crée l'utilisateur seulement s'il n'existe pas déjà
    private void createIfMissing(String username, String role) {
        if (appUserRepository.findByUsername(username).isEmpty()) {
            appUserRepository.save(new AppUser(username, passwordEncoder.encode(demoPassword), role, true));
        }
    }
}