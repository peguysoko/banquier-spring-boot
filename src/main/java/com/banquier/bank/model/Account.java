// "package" = l'adresse du tiroir où se trouve ce fichier
package com.banquier.bank.model;

// "import" = on emprunte des outils déjà écrits par d'autres
import jakarta.persistence.*;          // outils pour relier une classe Java à une table SQL
import java.math.BigDecimal;           // type de nombre EXACT pour l'argent (le DECIMAL de Java)
import java.time.LocalDateTime;        // type pour stocker une date et une heure

// @Entity = "cette classe représente une table de la base"
@Entity
// @Table = "et c'est la table qui s'appelle accounts"
@Table(name = "accounts")
// Une "classe" est un moule : elle décrit à quoi ressemble UN compte
public class Account {

    @Id                                                    // ce champ est la clé primaire (la carte d'identité)
    @GeneratedValue(strategy = GenerationType.IDENTITY)    // MySQL génère le numéro tout seul (AUTO_INCREMENT)
    private Long id;                                       // "private" = personne ne le touche de l'extérieur

    @Column(name = "account_number")   // le champ Java accountNumber = la colonne SQL account_number
    private String accountNumber;      // String = du texte

    @Column(name = "owner_name")
    private String ownerName;

    private String currency;           // même nom en Java et en SQL, pas besoin de @Column

    private BigDecimal balance;        // le solde, en nombre exact

    private String status;             // ACTIVE, BLOCKED ou CLOSED

        // Le propriétaire du compte : le numéro (id) de la personne dans la table users
    @Column(name = "owner_user_id")
    private Long ownerUserId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;   // la date de création

    // Les "getters" : des petites méthodes qui permettent de LIRE chaque champ.
    // Spring en a besoin pour transformer le compte en JSON (le format texte envoyé sur internet).
        // Pour LIRE le propriétaire du compte
    public Long getOwnerUserId() { return ownerUserId; }
    public Long getId() { return id; }                          // "return" = renvoie la valeur
    public String getAccountNumber() { return accountNumber; }
    public String getOwnerName() { return ownerName; }
    public String getCurrency() { return currency; }
    public BigDecimal getBalance() { return balance; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
        // Méthode pour RETIRER de l'argent du compte.
    // "void" = cette méthode ne renvoie rien, elle fait juste une action.
    public void debit(BigDecimal amount) {
        // subtract = soustraire. On ne peut pas écrire "-" avec BigDecimal, on utilise des méthodes.
        this.balance = this.balance.subtract(amount);
    }

    // Méthode pour AJOUTER de l'argent au compte.
    public void credit(BigDecimal amount) {
        // add = additionner
        this.balance = this.balance.add(amount);
    }
}