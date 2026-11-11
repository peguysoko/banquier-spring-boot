package com.banquier.bank.model;

import jakarta.persistence.*;

@Entity
@Table(name = "users")      // reflet de la table users
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;            // identifiant de connexion

    @Column(name = "password_hash")
    private String passwordHash;        // mot de passe haché (voir l'explication plus bas)

    private String role;                // CLIENT, CAISSIER, AUDITEUR ou ADMIN

    private boolean enabled;            // boolean = vrai ou faux : le compte est-il actif ?

    protected AppUser() {}              // constructeur vide pour Spring

    public AppUser(String username, String passwordHash, String role, boolean enabled) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.enabled = enabled;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getRole() { return role; }
    public boolean isEnabled() { return enabled; }   // pour un boolean, le getter s'écrit "is..."
}